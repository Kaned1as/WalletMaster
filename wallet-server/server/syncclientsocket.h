#ifndef SYNCCLIENTSOCKET_H
#define SYNCCLIENTSOCKET_H

#include <QTcpSocket>
#include "google/protobuf/stubs/common.h"
#include "sync/sync_protocol.pb.h"

using namespace com::adonai::wallet::sync;

class SyncClientSocket : public QTcpSocket
{
    Q_OBJECT
public:
    explicit SyncClientSocket(QObject *parent = 0);

    enum SyncState
    {
        NOT_IDENTIFIED = 0,
        AUTHORIZED,
        SENT_ACCOUNTS,
        SENT_OPERATIONS,
        SENT_CATEGORIES
    };

    SyncState getState() const;
    void setState(const SyncState &value);

signals:

public slots:

private:
    void readClientData();

    SyncState state;

};

#endif // SYNCCLIENTSOCKET_H
