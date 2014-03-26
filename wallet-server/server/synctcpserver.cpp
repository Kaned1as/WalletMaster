#include "synctcpserver.h"

SyncTcpServer::SyncTcpServer(QObject *parent) : QTcpServer(parent)
{
}

void SyncTcpServer::incomingConnection(qintptr handle) // newConnection is emitted later
{

    QTcpSocket* socket = new QTcpSocket();
    if(!socket->setSocketDescriptor(handle))
    {
        qDebug() << tr("Cant start new client connection, error %1").arg(socket->error());
        delete socket;
        return;
    }

    QThread *thread = new QThread(this);
    socket->moveToThread(thread);
    connect(socket, &QTcpSocket::disconnected, thread, &QThread::quit);
    connect(thread, &QThread::finished, socket, &QTcpSocket::deleteLater);
    connect(thread, &QThread::finished, thread, &QThread::deleteLater);
    thread->start();
    addPendingConnection(socket);
}