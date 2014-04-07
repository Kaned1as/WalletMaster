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
        case NOT_IDENTIFIED:
        {
            handleGeneric<sync::SyncRequest, sync::SyncResponse>(incomingData);
            break;
        }
        case AUTHORIZED:
        {
            handleGeneric<sync::AccountRequest, sync::AccountResponse>(incomingData);
            setState(SENT_ACCOUNTS);
            break;
        }
        case SENT_ACCOUNTS:
        {
            handleGeneric<sync::AccountResponse, sync::AccountAck>(incomingData);
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
                return response;
            }

            if(checkExists.next()) // login exists, deny
            {
                response.set_syncack(sync::SyncResponse::ACCOUNT_EXISTS);
                return response;
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
                    return response;
                }

                // successfully inserted log:pass pair
                userId = createSyncAcc.lastInsertId().toULongLong();
                response.set_syncack(sync::SyncResponse::OK);
                setState(AUTHORIZED);
                return response;
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
                 return response;
             }

             if(checkExists.next()) // login exists, pass
             {
                 userId = checkExists.value(0).toULongLong();

                 response.set_syncack(sync::SyncResponse::OK);
                 setState(AUTHORIZED);
                 return response;
             }
             else // no such login!
             {
                 response.set_syncack(sync::SyncResponse::AUTH_WRONG);
                 return response;
             }
        }
    }
}

sync::AccountResponse SyncClientSocket::handle(const sync::AccountRequest &request)
{
    sync::AccountResponse response;

    // we should select all accounts associated with
    QSqlQuery selectUserAccs(*conn);
                                     /* 0   1     2            3         4       5 */
    selectUserAccs.prepare("SELECT id, name, description, currency, amount, color FROM accounts WHERE sync_account = :userId");
    selectUserAccs.bindValue(":userId", userId);

    if(!selectUserAccs.exec())
    {
        qDebug() << tr("cannot retrieve nonsynced accounts, db error %1").arg(selectUserAccs.lastError().text());
        return response; // empty response
    }

    QList<qint64> knownIds;
    knownIds.reserve(request.knownid_size());
    for(qint64 known : request.knownid())
        knownIds.append(known);

    while(selectUserAccs.next())
    {
        const qint64 currentId = selectUserAccs.value("id").toLongLong();
        if(knownIds.contains(currentId)) // we have this account on device already
            knownIds.removeOne(currentId);
        else // we don't have this account on device
        {
            sync::Account* const account = response.add_account();
            account->set_id(selectUserAccs.value("id").toLongLong());
            account->set_name(selectUserAccs.value("name").toString().toStdString());
            account->set_description(selectUserAccs.value("description").toString().toStdString());
            account->set_currency(selectUserAccs.value("currency").toString().toStdString());
            account->set_amount(selectUserAccs.value("amount").toString().toStdString());
            account->set_color(selectUserAccs.value("color").toInt());
        }
    }

    // remaining accounts are deleted on server
    for(qint64 delId : knownIds)
        response.add_deletedid(delId);

    return response;
}

sync::AccountAck SyncClientSocket::handle(const sync::AccountResponse &response)
{
    sync::AccountAck ack;

    // delete accounts that deleted on device
    QSqlQuery deleter(*conn);
    deleter.prepare("DELETE FROM accounts WHERE sync_account = :userId AND id = :guid");
    QVariantList userList, idList;
    userList.reserve(response.deletedid_size());
    idList.reserve(response.deletedid_size());
    for(qint64 id : response.deletedid())
    {
        userList.append(userId);
        idList.append(id);
        ack.add_deletedguid(id);
    }
    deleter.addBindValue(userList);
    deleter.addBindValue(idList);
    if(!deleter.execBatch())
    {
        qDebug() << tr("Cannot delete accounts from server!");
        ack.clear_deletedguid();
    }

    // add accounts that were created on device
    QSqlQuery adder(*conn);
    adder.prepare("INSERT INTO accounts(sync_account, name, description, currency, amount, color) VALUES(?, ?, ?, ?, ?, ?)");
    // should execute each query async-ly because lastInsertId works only for last :(
    for(sync::Account acc : response.account())
    {
        adder.addBindValue(userId);
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

        if(adder.exec())
            ack.add_writtenguid(adder.lastInsertId().toLongLong());
        else
            qDebug() << tr("Cannot add accounts from device!");

        adder.finish();
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

    // send response
    if(!writeDelimited(response))
        qDebug() << tr("Error sending %2 to client! error string %1").arg(this->errorString()).arg(response.GetMetadata().descriptor->name().data());
}
