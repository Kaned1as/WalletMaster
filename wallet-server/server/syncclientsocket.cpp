#include "syncclientsocket.h"

SyncClientSocket::SyncClientSocket(QObject *parent) : QTcpSocket(parent), state(NOT_IDENTIFIED), pendingMessageSize(0), conn(NULL)
{
    qDebug() << tr("Got new connection!");
    connect(this, &QTcpSocket::readyRead, this, &SyncClientSocket::readClientData); //we should handle this in socket's own thread
}

SyncClientSocket::~SyncClientSocket()
{
    // free the database
    if(conn)
    {
        QString name = conn->connectionName();
        delete conn;
        QSqlDatabase::removeDatabase(name);
    }
    qDebug() << tr("Closing connection.");
}

SyncClientSocket::SyncState SyncClientSocket::getState() const
{
    return state;
}

void SyncClientSocket::setState(const SyncState &value)
{
    state = value;
}

void SyncClientSocket::initDbConnection()
{
    conn = new QSqlDatabase(QSqlDatabase::addDatabase("QMYSQL", QString::number(socketDescriptor())));
    conn->setHostName("localhost");
    conn->setDatabaseName("wallet");
    conn->setUserName("root");
    conn->setPassword("root");
    if(!conn->open())
        qDebug() << tr("Cannot connect to database! Error: %1").arg(conn->lastError().text());
}

// each sync message has prepended size as implemented in protobuf delimited read/write
// wee need to scan size first, then to read full message as it is available
void SyncClientSocket::readClientData()
{
    if(!conn)
        initDbConnection();

    if(!pendingMessageSize) // new data
        if(!readMessageSize(&pendingMessageSize)) // cannot read incoming size - wrong packet?
        {
            qDebug() << tr("Wrong packet! Can't read size.");
            readAll();
            return;
        }

    if(bytesAvailable() < pendingMessageSize) // read only full data
        return;

    QByteArray message = read(pendingMessageSize);
    pendingMessageSize = 0;
    handleMessage(message);

    if(bytesAvailable() > 0) // pending data after message parse - have more messages pending?
        readClientData(); // read next
}

// workaround for QTcpSocket && CodedOutputStream for parse delimited
bool SyncClientSocket::readMessageSize(quint32 * const out)
{
    QByteArray sizeContainer;
    for(quint8 i = 0; i < sizeof(quint32) && bytesAvailable(); ++i)
    {
        sizeContainer.append(read(1)); // read by one byte. At the most cases only one iteration is needed...
        pbuf::io::ArrayInputStream outputData(sizeContainer.constData(), sizeContainer.size());
        pbuf::io::CodedInputStream byteStream(&outputData); // we know it's byte stream
        if(byteStream.ReadVarint32(out))
            return true;
    }

    return false;
}

// for sending response
bool SyncClientSocket::writeDelimited(const google::protobuf::Message& message)
{
    const int messageSize = message.ByteSize();
    const int packetSize = pbuf::io::CodedOutputStream::VarintSize32(messageSize) + messageSize; // assure we have size+message bytes
    char* const bytes = new char[packetSize];

    pbuf::io::ArrayOutputStream outputData(bytes, packetSize);
    pbuf::io::CodedOutputStream outStream(&outputData);
    outStream.WriteVarint32(messageSize);                       // Implementation of
    message.SerializeToCodedStream(&outStream);                 // writeDelimitedTo
    bool success = writeData(bytes, packetSize) == packetSize;  // assure we have all bytes written

    delete[] bytes;
    flush();

    return success;
}

void SyncClientSocket::handleMessage(const QByteArray& incomingData)
{
    switch (state)
    {
        case NOT_IDENTIFIED: // wait register/auth
        {
            handleGeneric<sync::SyncRequest, sync::SyncResponse>(incomingData);
            break;
        }
        case WAITING_ACCOUNTS:     // wait accounts
        {
            handleGeneric<sync::EntityRequest, sync::EntityResponse>(incomingData);
            setState(SENT_ACCOUNTS);
            break;
        }
        case SENT_ACCOUNTS: // wait response
        {
            handleGeneric<sync::EntityResponse, sync::EntityAck>(incomingData);
            setState(WAITING_CATEGORIES);
            break;
        }
        case WAITING_CATEGORIES: // wait categories
        {
            handleGeneric<sync::EntityRequest, sync::EntityResponse>(incomingData);
            setState(SENT_CATEGORIES);
            break;
        }
        case FINISHED:
        {
            disconnect();
            break;
        }
        default:
            break;
    }
}

sync::SyncResponse SyncClientSocket::handle(const sync::SyncRequest& request)
{
    sync::SyncResponse response;
    if(!conn->isOpen())
    {
        qDebug() << tr("cannot process message from client, db connection is broken!");
        response.set_syncack(sync::SyncResponse::UNKNOWN_ERROR);
        return response;
    }

    switch (request.synctype())
    {
        case sync::SyncRequest::REGISTER:
        {
            QSqlQuery checkExists(*conn);
            checkExists.prepare("SELECT login FROM sync_accounts WHERE login = :check");
            checkExists.bindValue(":check", request.account().c_str());
            if(!checkExists.exec())
            {
                qDebug() << tr("cannot check login, db error %1").arg(checkExists.lastError().text());
                response.set_syncack(sync::SyncResponse::UNKNOWN_ERROR);
                break;
            }

            if(checkExists.next()) // login exists, deny
            {
                response.set_syncack(sync::SyncResponse::ACCOUNT_EXISTS);
                break;
            }
            else // we can register this account
            {
                QSqlQuery createSyncAcc(*conn);
                createSyncAcc.prepare("INSERT INTO sync_accounts(login, password) VALUES(:login, md5(:password))");
                createSyncAcc.bindValue(":login", request.account().c_str());
                createSyncAcc.bindValue(":password", request.password().c_str());
                if(!createSyncAcc.exec())
                {
                    qDebug() << tr("cannot insert new account, error %1").arg(createSyncAcc.lastError().text());
                    response.set_syncack(sync::SyncResponse::UNKNOWN_ERROR);
                    break;
                }

                // successfully inserted log:pass pair
                userId = createSyncAcc.lastInsertId().toULongLong();
                response.set_syncack(sync::SyncResponse::OK);
                setState(WAITING_ACCOUNTS);
            }
            break;
        }
        case sync::SyncRequest::MERGE:
        {
            QSqlQuery checkExists(*conn);
            checkExists.prepare("SELECT id FROM sync_accounts WHERE login = :login AND password = md5(:password)");
            checkExists.bindValue(":login", request.account().c_str());
            checkExists.bindValue(":password", request.password().c_str());

            if(!checkExists.exec())
            {
                qDebug() << tr("cannot check login, db error %1").arg(checkExists.lastError().text());
                response.set_syncack(sync::SyncResponse::UNKNOWN_ERROR);
                break;
            }

            if(checkExists.next()) // login exists, pass
            {
                userId = checkExists.value(0).toULongLong();

                response.set_syncack(sync::SyncResponse::OK);
                setState(WAITING_ACCOUNTS);
                break;
            }
            else // no such login!
                response.set_syncack(sync::SyncResponse::AUTH_WRONG);
            break;
        }
    }
    return response;
}

sync::EntityResponse SyncClientSocket::handle(const sync::EntityRequest &request)
{
    sync::EntityResponse response;

    // entity type that should be processed is FULLY DETERMINED ONLY BY CURRENT STATE (!)

    // we should select all entities present on server
    QSqlQuery selectSyncedEntities(*conn);
    switch(state)
    {
        case WAITING_ACCOUNTS:
            // get all known entities on server!
            selectSyncedEntities.prepare("SELECT id, name, description, currency, amount, color, last_modified FROM accounts WHERE sync_account = :userId");
            break;
        case WAITING_CATEGORIES:
            break;
        case WAITING_OPERATIONS:
            break;
        default:
            qDebug() << tr("Unknown entity type processing!");
            break;
    }
    selectSyncedEntities.bindValue(":userId", userId);

    if(!selectSyncedEntities.exec())
    {
        qDebug() << tr("cannot retrieve nonsynced entities, db error %1").arg(selectSyncedEntities.lastError().text());
        return response; // empty response
    }

    QList<qint64> knownIds;
    knownIds.reserve(request.knownid_size());
    for(qint64 known : request.knownid())
        knownIds.append(known);

    while(selectSyncedEntities.next())
    {
        const qint64 currentId = selectSyncedEntities.value("id").toLongLong();
        EntityState entityState = EQUAL;
        // check whether we need any processing
        if(knownIds.contains(currentId)) // we have this entity on device already, should detect, if entity was modified
        {
            knownIds.removeOne(currentId); // we have it on server and client, remove it
            const quint64 serverLastModified = selectSyncedEntities.value("last_modified").toLongLong();
            if(request.lastknownservertimestamp() > serverLastModified)
                continue;
            else
                entityState = MODIFIED; // we have old copy on our device, need to update (or merge!)

        }
        else // we don't have this entity on device
            entityState = ADDED;

        switch(state)
        {
            case WAITING_ACCOUNTS:
            {
                sync::Account* const account = entityState == ADDED
                    ? response.add_added()->mutable_account()
                    : response.add_modified()->mutable_account();
                account->set_id(selectSyncedEntities.value("id").toLongLong());
                account->set_name(selectSyncedEntities.value("name").toString().toStdString());
                account->set_description(selectSyncedEntities.value("description").toString().toStdString());
                account->set_currency(selectSyncedEntities.value("currency").toString().toStdString());
                account->set_amount(selectSyncedEntities.value("amount").toString().toStdString());
                account->set_color(selectSyncedEntities.value("color").toInt());
                break;
            }
            case WAITING_CATEGORIES:
            {
                //sync::Category* const category = response.add_entity()->mutable_category();
                break;
            }
            case WAITING_OPERATIONS:
            {
                //sync::Operation* const operation = response.add_entity()->mutable_operation();
                break;
            }
            default:
                qDebug() << tr("Unknown entity type processing!");
                break;
        }
    }

    // remaining entities are deleted on server, and so, should be returned to device being marked for deletion
    for(qint64 delId : knownIds)
        response.add_deletedid(delId);

    return response;
}

/**
 * @brief SyncClientSocket::handle
 *
 * First we need to add entities that were added on device
 * Then we need to modify entities merged on device and sent back
 * Then we need to delete entities deleted on device
 * That's all for now
 *
 * @param response - response from client with entities
 * @return Ack with new timestamp to set for client
 */
sync::EntityAck SyncClientSocket::handle(const sync::EntityResponse &response)
{
    sync::EntityAck ack;

    // delete entities that deleted on device
    QSqlQuery deleter(*conn);
    // add entities that were created on device
    QSqlQuery adder(*conn);
    // modify entities that were merged on device
    QSqlQuery modifier(*conn);
    switch(state)
    {
        case SENT_ACCOUNTS:
            deleter.prepare("DELETE FROM accounts WHERE sync_account = :userId AND id = :id");
            adder.prepare("INSERT INTO accounts(sync_account, id, name, description, currency, amount, color) VALUES(?, ?, ?, ?, ?, ?, ?)");
            modifier.prepare("UPDATE accounts SET name = ?, description = ?, currency = ?, amount = ?, color = ? WHERE sync_account = ? AND id = ?");
            break;
        case SENT_CATEGORIES:
            deleter.prepare("DELETE FROM categories WHERE sync_account = :userId AND id = :id");
            //adder.prepare("INSERT INTO accounts(sync_account, name, description, currency, amount, color) VALUES(?, ?, ?, ?, ?, ?)");
            break;
        case SENT_OPERATIONS:
            deleter.prepare("DELETE FROM operations WHERE sync_account = :userId AND id = :id");
            //adder.prepare("INSERT INTO accounts(sync_account, name, description, currency, amount, color) VALUES(?, ?, ?, ?, ?, ?)");
            break;
        default:
            qDebug() << tr("Unknown entity type processing!");
            break;
    }

    /// delete entities
    QVariantList userList, idList;
    userList.reserve(response.deletedid_size());
    idList.reserve(response.deletedid_size());
    for(qint64 id : response.deletedid())
    {
        userList.append(userId);
        idList.append(id);
        //ack.add_deletedguid(id);
    }
    deleter.addBindValue(userList);
    deleter.addBindValue(idList);
    if(!deleter.execBatch())
    {
        qDebug() << tr("Cannot delete entities from server! Error %1").arg(deleter.lastError().text());
        //ack.clear_deletedguid();
    }

    /// add entities
    // should execute each query async-ly because lastInsertId works only for last :(
    for(sync::Entity entity : response.added())
    {
        switch(state)
        {
            case SENT_ACCOUNTS:
            {
                const sync::Account& acc = entity.account();
                adder.addBindValue(userId);
                adder.addBindValue((quint64)acc.id());
                adder.addBindValue(acc.name().data());
                if(acc.has_description())
                    adder.addBindValue(acc.description().data());
                else
                    adder.addBindValue(QVariant(QVariant::String)); // no desc
                adder.addBindValue(acc.currency().data());
                adder.addBindValue(acc.amount().data());
                if(acc.has_color())
                    adder.addBindValue(acc.color());
                else
                    adder.addBindValue(QVariant(QVariant::Int));
                break;
            }
            case SENT_CATEGORIES:
            {
                const sync::Category& category = entity.category();
                break;
            }
            case SENT_OPERATIONS:
            {
                const sync::Operation& operation = entity.operation();
                break;
            }
            default:
                qDebug() << tr("Unknown entity type processing!");
                break;
        }

        if(!adder.exec())
            //ack.add_writtenguid(adder.lastInsertId().toLongLong());
        //else
            qDebug() << tr("Cannot add entities from device! Error: %1").arg(adder.lastError().text());

        adder.finish();
    }

    /// modify entities
    for(sync::Entity entity : response.modified())
    {
        switch(state)
        {
            case SENT_ACCOUNTS:
            {
                const sync::Account& acc = entity.account();
                modifier.addBindValue(acc.name().data());
                if(acc.has_description())
                    modifier.addBindValue(acc.description().data());
                else
                    modifier.addBindValue(QVariant(QVariant::String)); // no desc
                modifier.addBindValue(acc.currency().data());
                modifier.addBindValue(acc.amount().data());
                if(acc.has_color())
                    modifier.addBindValue(acc.color());
                else
                    modifier.addBindValue(QVariant(QVariant::Int));
                modifier.addBindValue(userId);
                modifier.addBindValue((quint64) acc.id());
                break;
            }
            case SENT_CATEGORIES:
            {
                const sync::Category& category = entity.category();
                break;
            }
            case SENT_OPERATIONS:
            {
                const sync::Operation& operation = entity.operation();
                break;
            }
            default:
                qDebug() << tr("Unknown entity type processing!");
                break;
        }

        if(!modifier.exec())
            //ack.add_writtenguid(adder.lastInsertId().toLongLong());
        //else
            qDebug() << tr("Cannot modify entities from device! Error %1").arg(modifier.lastError().text());
        modifier.finish();
    }

    QSqlQuery newTimeRetriever(*conn);
    newTimeRetriever.exec("SELECT CURRENT_TIMESTAMP");
    if(newTimeRetriever.exec() && newTimeRetriever.next())
        ack.set_newservertimestamp(newTimeRetriever.value(0).toLongLong());
    else
        qDebug() << tr("Cannot send new time to device! Error %1").arg(newTimeRetriever.lastError().text());

    return ack;
}

template<typename REQ, typename RESP> void SyncClientSocket::handleGeneric(const QByteArray& incomingData)
{
    // accept request
    REQ request;
    if(!request.ParseFromArray(incomingData.constData(), incomingData.size()))
        qDebug() << tr("error parsing %1 request from client!").arg(request.GetMetadata().descriptor->name().data());

    // handle
    RESP response = handle(request);

    // send response
    if(!writeDelimited(response))
        qDebug() << tr("Error sending %2 to client! error string %1").arg(this->errorString()).arg(response.GetMetadata().descriptor->name().data());
}
