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
    switch (state)
    {
        case NOT_IDENTIFIED:
        {
            SyncRequest request;
            if(!request.ParseFromArray(bytesReceived.constData(), bytesReceived.size()))
                qDebug() << "error parsing message from client!";

            SyncResponse response;
            response.set_syncack(SyncResponse::OK);

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

