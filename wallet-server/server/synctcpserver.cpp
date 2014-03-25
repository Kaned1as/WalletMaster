#include "synctcpserver.h"

SyncTcpServer::SyncTcpServer(QObject *parent) : QTcpServer(parent)
{
}

void SyncTcpServer::incomingConnection(qintptr handle)
{
    SocketThread* thread = new SocketThread(handle, this);
    connect(thread, &QThread::finished, thread, &QThread::deleteLater);
    thread->start();
}


SocketThread::SocketThread(int socketDescriptor, QTcpServer *parent) : QThread(parent), socketDescriptor(socketDescriptor), ready(false)
{
    ready = clientSocket.setSocketDescriptor(socketDescriptor);
}

const QTcpSocket& SocketThread::getSocket() const
{
    return clientSocket;
}

const bool SocketThread::isReady() const
{
    return ready;
}
