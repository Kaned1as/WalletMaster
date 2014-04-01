#ifndef SYNCCLIENTSOCKET_H
#define SYNCCLIENTSOCKET_H

#include <QTcpSocket>
#include "google/protobuf/stubs/common.h"

#include "google/protobuf/io/coded_stream.h"
#include "google/protobuf/io/zero_copy_stream_impl_lite.h"

#include "sync/sync_protocol.pb.h"

namespace sync = com::adonai::wallet::sync;
namespace protocol = google::protobuf;

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
