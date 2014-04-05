#ifndef SYNCCLIENTSOCKET_H
#define SYNCCLIENTSOCKET_H

#include <QTcpSocket>
#include <QtSql>

#include "google/protobuf/stubs/common.h"
#include "google/protobuf/io/coded_stream.h"
#include "google/protobuf/io/zero_copy_stream_impl_lite.h"

#include "sync/sync_protocol.pb.h"

namespace sync = com::adonai::wallet::sync;
namespace pbuf = google::protobuf;

class SyncClientSocket : public QTcpSocket
{
    Q_OBJECT
public:
    explicit SyncClientSocket(QObject *parent = 0);
    ~SyncClientSocket();

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
    void initDbConnection();

    void readClientData();
    bool readMessageSize(quint32 * const out);
    bool writeDelimited(const google::protobuf::Message &message);
    void handleMessage(const QByteArray& incomingData);

    sync::SyncResponse handleSyncRequest(const sync::SyncRequest& request);
    sync::AccountResponse handleAccountRequest(const sync::AccountRequest& request);

    SyncState state;
    quint32 pendingMessageSize;

    QSqlDatabase* conn;
    quint64 userId;
};

#endif // SYNCCLIENTSOCKET_H
