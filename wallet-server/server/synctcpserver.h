#ifndef SYNCTCPSERVER_H
#define SYNCTCPSERVER_H

#include <QTcpServer>
#include <QTcpSocket>
#include <QThread>

#include "syncclientsocket.h"

class SyncTcpServer : public QTcpServer
{
    Q_OBJECT
public:
    explicit SyncTcpServer(QObject *parent = 0);

    // QTcpServer interface
protected:
    void incomingConnection(qintptr handle) override;

    // QTcpServer interface
public:
    SyncClientSocket *nextPendingConnection() override;
};

#endif // SYNCTCPSERVER_H
