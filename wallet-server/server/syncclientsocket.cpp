#include "syncclientsocket.h"

SyncClientSocket::SyncClientSocket(QObject *parent) : QTcpSocket(parent), state(NOT_IDENTIFIED)
{
    qDebug() << tr("Got new connection!");
    connect(this, &QTcpSocket::readyRead, this, &SyncClientSocket::readClientData); //we should handle this in socket's own thread
}

SyncClientSocket::~SyncClientSocket()
{
    conn.close();
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
    conn = QSqlDatabase::addDatabase("QMYSQL", QString::number(socketDescriptor()));
    conn.setHostName("localhost");
    conn.setDatabaseName("wallet");
    conn.setUserName("root");
    conn.setPassword("root");
    if(!conn.open())
        qDebug() << tr("Cannot connect to database! Error: %1").arg(conn.lastError().text());
}

// each sync message has prepended size as implemented in protobuf delimited read/write
// wee need to scan size first, then to read full message as it is available
void SyncClientSocket::readClientData()
{
    thread_local quint32 messageSize = 0;
    if(!messageSize) // new data
        if(!readMessageSize(&messageSize)) // cannot read incoming size - wrong packet?
        {
            qDebug() << tr("Wrong packet! Can't read size.");
            readAll();
            return;
        }

    if(bytesAvailable() < messageSize) // read only full data
        return;

    QByteArray message = read(messageSize);
    messageSize = 0;
    handleMessage(message);

    if(bytesAvailable() > 0) // pending data after message parse - have more messages pending?
        readClientData(); // read next
}

// workaround for QTcpSocket && CodedOutputStream for parse delimited
bool SyncClientSocket::readMessageSize(quint32 *out)
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
bool SyncClientSocket::writeDelimited(google::protobuf::Message &message)
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

void SyncClientSocket::handleMessage(const QByteArray &incomingData)
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
            sync::SyncResponse response = handleSyncRequest(request);
            if(response.syncack() == sync::SyncResponse::OK)
                setState(AUTHORIZED);

            // send response
            if(!writeDelimited(response))
                qDebug() << tr("Error sending sync response to client! error string %1").arg(errorString());
            break;
        }
        default:
            break;
    }
}

sync::SyncResponse SyncClientSocket::handleSyncRequest(sync::SyncRequest &request)
{
    sync::SyncResponse response;
    if(!conn.isOpen())
    {
        qDebug() << tr("cannot process message from client, db connection is broken!");
        response.set_syncack(sync::SyncResponse::UNKNOWN_ERROR);
        return response;
    }

    switch (request.synctype())
    {
        case sync::SyncRequest::REGISTER:
        {
            QSqlQuery checkExists(conn);
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
                response.set_syncack(sync::SyncResponse::OK);
                return response;
            }
            break;
        }
    }

}

