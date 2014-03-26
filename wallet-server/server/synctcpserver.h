#ifndef SYNCTCPSERVER_H
#define SYNCTCPSERVER_H

#include <QTcpServer>
#include <QTcpSocket>
#include <QThread>

class SyncTcpServer : public QTcpServer
{
    Q_OBJECT
public:
    explicit SyncTcpServer(QObject *parent = 0);

    // QTcpServer interface
protected:
    void incomingConnection(qintptr handle);
};

#endif // SYNCTCPSERVER_H
