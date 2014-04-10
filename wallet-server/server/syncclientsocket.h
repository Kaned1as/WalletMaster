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

/**
 * @brief The SyncClientSocket class
 *
 * This class provides full sync process between server and client
 *
 * 1) Client sends sync auth/register packet. Server responds with convenient answer
 * 2) Client sends known server ids and last revision known to him
 *
 * Server stores last revision of data by specified account
 * Server stores last modification revision along with any entity received from client
 *
 * If outdated device requests sync, server compares its last revision number with current revision for this account
 * After that server can select objects with last revision > last client revision.
 * That are added and modified objects.
 * Server also creates a list of deleted objects' IDs as (knownToClientIDs subtract presentOnServerIDs)
 *
 * 3) Server sends added and modified objects to client. Server also sends deleted entities' IDs.
 *
 * Client is meant to be able to recoincile all the data from server - this is important part! In short this process looks like `git rebase`
 * a) Client adds received objects locally
 * b) Client overwrites its objects with modified on server
 * c) Client deletes local objects that do not exist on server (bound objects are deleted also...)
 *
 * d) Client reapplies all the local non-synced changes
 *
 * 4) Client sends pack of its added, modified, deleted data to server
 * 5) Server modifies its data and sends confirmation (with IDs) to client
 * 6) Sync is finished
 *
 */

class SyncClientSocket : public QTcpSocket
{
    Q_OBJECT
public:
    explicit SyncClientSocket(QObject *parent = 0);
    ~SyncClientSocket();

    enum SyncState
    {
        NOT_IDENTIFIED = 0,
        WAITING_ACCOUNTS,
        SENT_ACCOUNTS,
        WAITING_CATEGORIES,
        SENT_CATEGORIES,
        WAITING_OPERATIONS,
        SENT_OPERATIONS,
        FINISHED
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

    sync::SyncResponse handle(const sync::SyncRequest& request);
    sync::EntityResponse handle(const sync::EntityRequest& request);
    sync::EntityAck handle(const sync::EntityResponse& response);

    template<typename REQ, typename RESP> void handleGeneric(const QByteArray& incomingData);

    SyncState state;
    quint32 pendingMessageSize;

    QSqlDatabase* conn;
    quint64 userId;
};

#endif // SYNCCLIENTSOCKET_H
