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
    QByteArray bytes = readAll();
    switch (state)
    {
        case NOT_IDENTIFIED:
        {
            SyncRequest request;
            if(!request.ParseFromArray(bytes.data(), bytes.size()))
                qDebug() << "error parsing message from client!";
            break;
        }
        default:
            break;
    }
}

