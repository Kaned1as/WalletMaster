#include "syncclientsocket.h"

SyncClientSocket::SyncClientSocket(QObject *parent) : QTcpSocket(parent), state(NOT_IDENTIFIED), pendingMessageSize(0), conn(NULL)
{
    qDebug() << tr("Got new connection!");
    connect(this, &QTcpSocket::readyRead, this, &SyncClientSocket::readClientData); //we should handle this in socket's own thread
}

SyncClientSocket::~SyncClientSocket()
{
    // free the database
    conn->close();
    QString name = conn->connectionName();
    delete conn;
    QSqlDatabase::removeDatabase(name);
    qDebug() << tr("Closing connection.");
}

bool SyncClientSocket::setSocketDescriptor(qintptr socketDescriptor, QAbstractSocket::SocketState state, QIODevice::OpenMode openMode)
{
    bool result = QTcpSocket::setSocketDescriptor(socketDescriptor, state, openMode);
    if(result)
        initDbConnection();

    return result;
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
            // accept request
            sync::SyncRequest request;
            if(!request.ParseFromArray(incomingData.constData(), incomingData.size()))
                qDebug() << tr("error parsing sync request from client!");

            // handle
            const sync::SyncResponse& response = handleSyncRequest(request); // avoid copying data...
            if(response.syncack() == sync::SyncResponse::OK)
                setState(AUTHORIZED);

            // send response
            if(!writeDelimited(response))
                qDebug() << tr("Error sending sync response to client! error string %1").arg(errorString());
            break;
        }
        case AUTHORIZED:
        {
            sync::AccountRequest request;
            if(!request.ParseFromArray(incomingData.constData(), incomingData.size()))
                qDebug() << tr("error parsing account request from client!");

            // handle
            sync::AccountResponse response = handleAccountRequest(request);

            // send response
            if(!writeDelimited(response))
                qDebug() << tr("Error sending account response to client! error string %1").arg(errorString());
        }
        default:
            break;
    }
}

sync::SyncResponse SyncClientSocket::handleSyncRequest(const sync::SyncRequest& request)
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
                checkExists.finish();
                response.set_syncack(sync::SyncResponse::ACCOUNT_EXISTS);
                return response;
            }
            else // we can register this account
            {
                checkExists.finish();

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
                createSyncAcc.finish();
                response.set_syncack(sync::SyncResponse::OK);
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
                 checkExists.finish();

                 response.set_syncack(sync::SyncResponse::OK);
                 return response;
             }
             else // no such login!
             {
                 checkExists.finish();
                 response.set_syncack(sync::SyncResponse::AUTH_WRONG);
                 return response;
             }
        }
    }
}

sync::AccountResponse SyncClientSocket::handleAccountRequest(const sync::AccountRequest &request)
{
    sync::AccountResponse response;

    // we should select non-synced accounts from our database and send them
    QSqlQuery selectNonSyncedAccs(*conn);
                                     /* 0   1     2            3         4       5 */
    selectNonSyncedAccs.prepare("SELECT id, name, description, currency, amount, color FROM accounts WHERE sync_account = :userId AND id > :lastClientKnownId");
    selectNonSyncedAccs.bindValue(":userId", userId);
    selectNonSyncedAccs.bindValue(":lastClientKnownId", (qint64) request.lastknownid());

    if(!selectNonSyncedAccs.exec())
    {
        qDebug() << tr("cannot retrieve nonsynced accounts, db error %1").arg(selectNonSyncedAccs.lastError().text());
        return response; // empty response
    }

    while(selectNonSyncedAccs.next()) {
        sync::Account* account = response.add_accounts();
        account->set_id(selectNonSyncedAccs.value("id").toLongLong());
        account->set_name(selectNonSyncedAccs.value("name").toString().toStdString());
        account->set_description(selectNonSyncedAccs.value("description").toString().toStdString());
        account->set_currency(selectNonSyncedAccs.value("currency").toString().toStdString());
        account->set_amount(selectNonSyncedAccs.value("amount").toString().toStdString());
        account->set_color(selectNonSyncedAccs.value("color").toInt());
    }

    return response;
}

