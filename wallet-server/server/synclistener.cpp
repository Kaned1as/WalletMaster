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
    qDebug() << "Listener starting...";
    server = new SyncTcpServer(this);
}

SyncListener::~SyncListener()
{
    qDebug() << "Listener deleting...";
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
    for(QTcpSocket* const client : activeClients)
        client->close();
    server->close();
        qDebug() << "Server stopped!";
}

void SyncListener::handleNewConnection()
{
    qDebug() << tr("Got new connection!");
    SyncClientSocket* const clientSocket = server->nextPendingConnection();
    activeClients[clientSocket->socketDescriptor()] = clientSocket;
    connect(clientSocket, &QTcpSocket::disconnected, this, [=] () {
        activeClients.remove(clientSocket->socketDescriptor()); // remove client from active list (`this` to handle in our thread)
    });
}

