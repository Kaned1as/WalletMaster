/**************************************************************************
** This program is free software; you can redistribute it and/or
** modify it under the terms of the GNU General Public License as
** published by the Free Software Foundation; either version 3 of the
** License, or (at your option) any later version.
**
** This program is distributed in the hope that it will be useful, but
** WITHOUT ANY WARRANTY; without even the implied warranty of
** MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
** General Public License for more details:
** http://www.gnu.org/licenses/gpl.txt
**************************************************************************/

#include "synclistener.h"
#include <QTcpSocket>

#define LISTEN_PORT 17001

SyncListener::SyncListener(QObject *parent) : QObject(parent)
{
    server = new QTcpServer(this);
}

SyncListener::~SyncListener()
{
    if(server->isListening())
        server->close();
    delete server;
}

void SyncListener::start()
{
    if(!server->listen(QHostAddress::Any, LISTEN_PORT))
        qDebug() << tr("Unable to start server, error string %1.").arg(server->errorString());
    else
    {
        qDebug() << tr("Server started!");
        connect(server, &QTcpServer::newConnection, this, &SyncListener::handleNewConnection);
    }
}

void SyncListener::stop()
{
    for(QTcpSocket* const client : clients)
        client->close();
    server->close();
}

void SyncListener::handleNewConnection()
{
    qDebug() << tr("Got new connection!");
    QTcpSocket* const clientSocket = server->nextPendingConnection();
    clients[clientSocket->socketDescriptor()] = clientSocket;
    connect(clientSocket, &QTcpSocket::readyRead, this, &SyncListener::readClientData);
    connect(clientSocket, &QTcpSocket::disconnected, [&] () {
        clients.remove(clientSocket->socketDescriptor());
        clientSocket->deleteLater();
    });
}

void SyncListener::readClientData()
{
    QTcpSocket* client = qobject_cast<QTcpSocket*>(sender());
}


