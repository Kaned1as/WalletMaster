#include "syncclientsocket.h"

SyncClientSocket::SyncClientSocket(QObject *parent) : QTcpSocket(parent), state(NOT_IDENTIFIED), transactionOpened(false), pendingMessageSize(0), conn(NULL)
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

void SyncClientSocket::disconnectFromHost()
{
    if(transactionOpened) // client disconnected and left sync in progress, need to close transaction
        conn->rollback();

    QAbstractSocket::disconnectFromHost();
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
    conn->setPassword("root"); // i know it's hacky but it's localhost only
    if(!conn->open())
    {
        qDebug() << tr("Cannot connect to database! Error: %1").arg(conn->lastError().text());
        disconnectFromHost();
        return;
    }
    if(!(transactionOpened = conn->transaction()))
    {
        qDebug() << tr("Cannot start database transaction! Error: %1").arg(conn->lastError().text());
        disconnectFromHost();
        return;
    }
}

void SyncClientSocket::finishProcessing()
{
    conn->commit();
    transactionOpened = false;
    disconnectFromHost();
}

void SyncClientSocket::interruptProcessing()
{
    if(!conn->rollback())
        qDebug() << tr("Cannot rollback transaction! Error: %1").arg(conn->lastError().text()); // should never happen!
    transactionOpened = false;
    setState(ERROR);
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
    if(state == ERROR) // no need to send, disconnect
    {
        disconnectFromHost();
        return true;
    }

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
            if(state == NOT_IDENTIFIED) // auth error
                interruptProcessing();
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
        case SENT_CATEGORIES: // sent response
        {
            handleGeneric<sync::EntityResponse, sync::EntityAck>(incomingData);
            setState(WAITING_OPERATIONS);
            break;
        }
        case WAITING_OPERATIONS: // wait operations
        {
            handleGeneric<sync::EntityRequest, sync::EntityResponse>(incomingData);
            setState(SENT_OPERATIONS);
            break;
        }
        case SENT_OPERATIONS: // sent response
        {
            handleGeneric<sync::EntityResponse, sync::EntityAck>(incomingData);
            finishProcessing();
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
            checkExists.bindValue(":check", request.account().data());
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
            checkExists.bindValue(":login", request.account().data());
            checkExists.bindValue(":password", request.password().data());

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
    // get all known entities on server
    switch(state)
    {
        case WAITING_ACCOUNTS:
            selectSyncedEntities.prepare("SELECT id, name, description, currency, amount, color, last_modified FROM accounts WHERE sync_account = :userId");
            break;
        case WAITING_CATEGORIES:
            selectSyncedEntities.prepare("SELECT id, name, type, preferred_account_id, last_modified FROM categories WHERE sync_account = :userId");
            break;
        case WAITING_OPERATIONS:
            selectSyncedEntities.prepare("SELECT id, description, amount, category_id, time, charger_id, beneficiar_id, converting_rate, last_modified FROM operations WHERE sync_account = :userId");
            break;
        default:
            qDebug() << tr("Unknown entity type processing!");
            interruptProcessing();
            return response;
    }
    selectSyncedEntities.bindValue(":userId", userId);

    if(!selectSyncedEntities.exec())
    {
        qDebug() << tr("cannot retrieve nonsynced entities, db error %1").arg(selectSyncedEntities.lastError().text());
        interruptProcessing();
        return response; // empty response
    }

    QList<QString> knownIds;
    knownIds.reserve(request.knownid_size());
    for(std::string known : request.knownid())
        knownIds.append(QString(known.data()));

    while(selectSyncedEntities.next())
    {
        const QString currentId = selectSyncedEntities.value("id").toString();
        EntityState entityState = EQUAL;
        // check whether we need any processing
        if(knownIds.contains(currentId)) // we have this entity on device already, should detect, if entity was modified
        {
            knownIds.removeOne(currentId); // we have it on server and client, remove it
            const quint64 serverLastModified = selectSyncedEntities.value("last_modified").toDateTime().toMSecsSinceEpoch();
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
                account->set_id(selectSyncedEntities.value("id").toString().toStdString());
                account->set_name(selectSyncedEntities.value("name").toString().toStdString());
                if(!selectSyncedEntities.value("description").isNull())
                    account->set_description(selectSyncedEntities.value("description").toString().toStdString());
                account->set_currency(selectSyncedEntities.value("currency").toString().toStdString());
                account->set_amount(selectSyncedEntities.value("amount").toString().toStdString());
                account->set_color(selectSyncedEntities.value("color").toInt());
                break;
            }
            case WAITING_CATEGORIES:
            {
                sync::Category* const category = entityState == ADDED
                        ? response.add_added()->mutable_category()
                        : response.add_modified()->mutable_category();
                category->set_id(selectSyncedEntities.value("id").toString().toStdString());
                category->set_name(selectSyncedEntities.value("name").toString().toStdString());
                category->set_type(selectSyncedEntities.value("type").toInt());
                if(!selectSyncedEntities.value("preferred_account_id").isNull())
                    category->set_preferredaccount(selectSyncedEntities.value("preferred_account_id").toString().toStdString());
                break;
            }
            case WAITING_OPERATIONS:
            {
                sync::Operation* const operation = entityState == ADDED
                    ? response.add_added()->mutable_operation()
                    : response.add_modified()->mutable_operation();
                operation->set_id(selectSyncedEntities.value("id").toString().toStdString());
                if(!selectSyncedEntities.value("description").isNull())
                    operation->set_description(selectSyncedEntities.value("description").toString().toStdString());
                operation->set_amount(selectSyncedEntities.value("amount").toString().toStdString());
                operation->set_time(selectSyncedEntities.value("time").toDateTime().toMSecsSinceEpoch());
                operation->set_categoryid(selectSyncedEntities.value("category_id").toString().toStdString());
                if(!selectSyncedEntities.value("charger_id").isNull())
                    operation->set_chargerid(selectSyncedEntities.value("charger_id").toString().toStdString());
                if(!selectSyncedEntities.value("beneficiar_id").isNull())
                    operation->set_beneficiarid(selectSyncedEntities.value("beneficiar_id").toString().toStdString());
                if(!selectSyncedEntities.value("converting_rate").isNull())
                    operation->set_convertingrate(selectSyncedEntities.value("converting_rate").toDouble());
                operation->set_amount(selectSyncedEntities.value("amount").toString().toStdString());
                break;
            }
            default:
                qDebug() << tr("Unknown entity type processing!");
                interruptProcessing();
                return response;
        }
    }

    // remaining entities are deleted on server, and so, should be returned to device being marked for deletion
    for(QString delId : knownIds)
        response.add_deletedid(delId.toStdString());

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
            adder.prepare("INSERT INTO categories(sync_account, id, name, type, preferred_account_id) VALUES(?, ?, ?, ?, ?)");
            modifier.prepare("UPDATE categories SET name = ?, type = ?, preferred_account_id = ? WHERE sync_account = ? AND id = ?");
            break;
        case SENT_OPERATIONS:
            deleter.prepare("DELETE FROM operations WHERE sync_account = :userId AND id = :id");
            adder.prepare("INSERT INTO operations(sync_account, id, description, amount, category_id, time, charger_id, beneficiar_id, converting_rate) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)");
            modifier.prepare("UPDATE operations SET description = ?, amount = ?, category_id = ?, time = ?, charger_id = ?, beneficiar_id = ?, converting_rate = ? WHERE sync_account = ? AND id = ?");
            break;
        default:
            qDebug() << tr("Unknown entity type processing!");
            interruptProcessing();
            return ack;
    }

    /// delete entities
    QVariantList userList, idList;
    userList.reserve(response.deletedid_size());
    idList.reserve(response.deletedid_size());
    for(std::string id : response.deletedid())
    {
        userList.append(userId);
        idList.append(id.data());
        //ack.add_deletedguid(id);
    }
    deleter.addBindValue(userList);
    deleter.addBindValue(idList);
    if(!deleter.execBatch())
    {
        qDebug() << tr("Cannot delete entities from server! Error %1").arg(deleter.lastError().text());
        interruptProcessing();
        return ack;
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
                adder.addBindValue(acc.id().data());
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
                adder.addBindValue(userId);
                adder.addBindValue(category.id().data());
                adder.addBindValue(category.name().data());
                adder.addBindValue(category.type());
                if(category.has_preferredaccount())
                    adder.addBindValue(category.preferredaccount().data());
                else
                    adder.addBindValue(QVariant(QVariant::String));
                break;
            }
            case SENT_OPERATIONS:
            {
                const sync::Operation& operation = entity.operation();
                adder.addBindValue(userId);
                adder.addBindValue(operation.id().data());
                if(operation.has_description())
                    adder.addBindValue(operation.description().data());
                else
                    adder.addBindValue(QVariant(QVariant::String)); // no desc
                adder.addBindValue(operation.amount().data());
                adder.addBindValue(operation.categoryid().data());
                adder.addBindValue(QDateTime::fromMSecsSinceEpoch(operation.time()));

                if(operation.has_chargerid())
                    adder.addBindValue(operation.chargerid().data());
                else
                    adder.addBindValue(QVariant(QVariant::String)); // no charger

                if(operation.has_beneficiarid())
                    adder.addBindValue(operation.beneficiarid().data());
                else
                    adder.addBindValue(QVariant(QVariant::String)); // no beneficiar

                if(operation.has_convertingrate())
                    adder.addBindValue(operation.convertingrate());
                else
                    adder.addBindValue(QVariant(QVariant::Double)); // no converting rate
                break;
            }
            default:
                qDebug() << tr("Unknown entity type processing!");
                interruptProcessing();
                return ack;
        }

        if(!adder.exec())
        {
            qDebug() << tr("Cannot add entities from device! Error: %1").arg(adder.lastError().text());
            interruptProcessing();
            return ack;
        }

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
                modifier.addBindValue(acc.id().data());
                break;
            }
            case SENT_CATEGORIES:
            {
                const sync::Category& category = entity.category();
                modifier.addBindValue(category.name().data());
                modifier.addBindValue(category.type());
                if(category.has_preferredaccount())
                    modifier.addBindValue(category.preferredaccount().data());
                else
                    modifier.addBindValue(QVariant(QVariant::String));
                modifier.addBindValue(userId);
                modifier.addBindValue(category.id().data());
                break;
            }
            case SENT_OPERATIONS:
            {
                const sync::Operation& operation = entity.operation();
                if(operation.has_description())
                    modifier.addBindValue(operation.description().data());
                else
                    modifier.addBindValue(QVariant(QVariant::String));

                modifier.addBindValue(operation.amount().data());
                modifier.addBindValue(operation.categoryid().data());
                modifier.addBindValue(QDateTime::fromMSecsSinceEpoch(operation.time()));
                if(operation.has_chargerid())
                    modifier.addBindValue(operation.chargerid().data());
                else
                    modifier.addBindValue(QVariant(QVariant::String));

                if(operation.has_beneficiarid())
                    modifier.addBindValue(operation.beneficiarid().data());
                else
                    modifier.addBindValue(QVariant(QVariant::String));

                if(operation.has_convertingrate())
                    modifier.addBindValue(operation.convertingrate());
                else
                    modifier.addBindValue(QVariant(QVariant::String));

                modifier.addBindValue(userId);
                modifier.addBindValue(operation.id().data());
                break;
            }
            default:
                qDebug() << tr("Unknown entity type processing!");
                interruptProcessing();
                return ack;
        }

        if(!modifier.exec())
        {
            qDebug() << tr("Cannot modify entities from device! Error %1").arg(modifier.lastError().text());
            interruptProcessing();
            return ack;
        }

        modifier.finish();
    }

    QSqlQuery newTimeRetriever(*conn);
    newTimeRetriever.exec("SELECT CURRENT_TIMESTAMP()");
    if(newTimeRetriever.exec() && newTimeRetriever.next())
        ack.set_newservertimestamp(newTimeRetriever.value(0).toDateTime().toMSecsSinceEpoch());
    else
    {
        qDebug() << tr("Cannot send new time to device! Error %1").arg(newTimeRetriever.lastError().text());
        interruptProcessing();
    }

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
    if(isOpen())

    // send response
    if(!writeDelimited(response))
        qDebug() << tr("Error sending %2 to client! error string %1").arg(this->errorString()).arg(response.GetMetadata().descriptor->name().data());
}
