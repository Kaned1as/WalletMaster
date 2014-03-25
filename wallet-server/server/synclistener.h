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

#ifndef SYNCLISTENER_H
#define SYNCLISTENER_H

#include <QObject>
#include <QTcpServer>

class SyncListener : public QObject
{
    Q_OBJECT
public:
    explicit SyncListener(QObject *parent = 0);
    ~SyncListener();


signals:

public slots:
    void start();
    void stop();
private:
    void handleNewConnection();
    void readClientData();

    QTcpServer* server;
    QMap<int, QTcpSocket*> clients;
};

#endif // SYNCLISTENER_H
