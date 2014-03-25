#ifndef SYNCTCPSERVER_H
#define SYNCTCPSERVER_H

#include <QTcpServer>
#include <QTcpSocket>
#include <QThread>


class SocketThread : public QThread
{
    Q_OBJECT
public:
    SocketThread(int socketDescriptor, QTcpServer *parent);
    const QTcpSocket& getSocket() const;

signals:

private:
    int socketDescriptor;
    QTcpSocket clientSocket;
    QTcpSocket::SocketError state;
};

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
