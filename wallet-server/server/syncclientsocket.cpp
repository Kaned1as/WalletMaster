#include "syncclientsocket.h"

SyncClientSocket::SyncClientSocket(QObject *parent) : QTcpSocket(parent), state(NOT_IDENTIFIED)
{
    qDebug() << tr("Got new connection!");
    connect(this, &QTcpSocket::readyRead, this, &SyncClientSocket::readClientData); //we should handle this in socket's own thread
    initDbConnection();
}

SyncClientSocket::~SyncClientSocket()
{
    conn.close();
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
    conn = QSqlDatabase::addDatabase("QMYSQL", QString::number(socketDescriptor()));
    conn.setHostName("localhost");
    conn.setDatabaseName("test");
    conn.setUserName("root");
    conn.setPassword("root");
    if(!conn.open())
        qDebug() << tr("Cannot connect to database! Error: %1").arg(conn.lastError().text());
}

// each sync message has prepended size as implemented in protobuf delimited read/write
// wee need to scan size first, then to read full message as it is available
void SyncClientSocket::readClientData()
{
    static quint32 messageSize = 0;
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
        pbuf::io::CodedInputStream byteStream(&pbuf::io::ArrayInputStream(sizeContainer.constData(), sizeContainer.size())); // we know it's byte stream
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

    pbuf::io::CodedOutputStream outStream(&pbuf::io::ArrayOutputStream(bytes, packetSize));
    outStream.WriteVarint32(messageSize);                       // Implementation of
    message.SerializeToCodedStream(&outStream);                // writeDelimitedTo
    bool success = writeData(bytes, packetSize) == packetSize;  // assure we have all bytes written
    delete[] bytes;

    return success;
}

void SyncClientSocket::handleMessage(const QByteArray &incomingData)
{
    switch (state)
    {
        case NOT_IDENTIFIED:
        {
            sync::SyncRequest request;
            if(!request.ParseFromArray(incomingData.constData(), incomingData.size()))
                qDebug() << "error parsing message from client!";

            sync::SyncResponse response;
            response.set_syncack(sync::SyncResponse::OK);

            if(!writeDelimited(response))
                qDebug() << "Error sending sync response to client! error string" << errorString();
            flush();
            setState(AUTHORIZED);
            break;
        }
        default:
            break;
    }
}

