#include "syncclientsocket.h"

SyncClientSocket::SyncClientSocket(QObject *parent) : QTcpSocket(parent), state(NOT_IDENTIFIED)
{
    connect(this, &QTcpSocket::readyRead, this, &SyncClientSocket::readClientData); //we should handle this in socket's own thread
}

SyncClientSocket::SyncState SyncClientSocket::getState() const
{
    return state;
}

void SyncClientSocket::setState(const SyncState &value)
{
    state = value;
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
        pbuf::io::CodedInputStream byteStream(reinterpret_cast<const pbuf::uint8*>(sizeContainer.constData()), sizeContainer.size()); // we know it's byte stream
        if(byteStream.ReadVarint32(out))
            return true;
    }

    return false;
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

            // sending response
            const int messageSize = response.ByteSize();
            char* const bytes = new char[messageSize];
            response.SerializeToArray(bytes, messageSize);
            if(writeData(bytes, messageSize) == -1)
                qDebug() << "Error sending sync response to client! error string" << errorString();
            flush();
            delete[] bytes;
            setState(AUTHORIZED);
            break;
        }
        default:
            break;
    }
}

