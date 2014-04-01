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


void SyncClientSocket::readClientData()
{
    QByteArray bytesReceived = readAll();
    protocol::io::CodedInputStream byteStream(reinterpret_cast<const protocol::uint8*>(bytesReceived.constData()), bytesReceived.size());
    static quint32 messagesize = 0;
    byteStream.ReadVarint32(&messagesize);

    switch (state)
    {
        case NOT_IDENTIFIED:
        {
            sync::SyncRequest request;
            if(!request.ParseFromArray(bytesReceived.constData(), bytesReceived.size()))
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

