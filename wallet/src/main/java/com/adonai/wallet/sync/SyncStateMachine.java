package com.adonai.wallet.sync;

import android.content.SharedPreferences;
import android.database.Observable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.preference.PreferenceManager;

import com.adonai.wallet.DatabaseDAO;
import com.adonai.wallet.R;
import com.adonai.wallet.WalletBaseActivity;
import com.adonai.wallet.WalletConstants;
import com.adonai.wallet.database.DbProvider;
import com.adonai.wallet.entities.Account;
import com.adonai.wallet.entities.Entity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.sql.SQLException;
import java.util.List;

import static com.adonai.wallet.sync.SyncProtocol.SyncRequest;
import static com.adonai.wallet.sync.SyncProtocol.SyncResponse;

/**
 * In short, synchronization scheme is as follows:
 *
 *| Client sends register/authentication packet ---------------------------------------------------------------->|
 *|                                                                                                              |
 *| <----------------------- Server replies with ok/error. If we have error here, communication ends immediately |
 *|                                                                                                              |
 *| Client sends account request containing all known to him synced account ID --------------------------------->|
 *|                                                                                                              |
 *| *** Server parses request and makes a list of added/removed/modified IDs on server based on its data         |
 *| *** Added accounts are transferred fully and contain GUIDs if present, removed accounts contain only GUIDs   |
 *|                                                                                                              |
 *| <--------------------------------- Server replies with account response containing added/removed accounts    |
 *|                                                                                                              |
 *| *** Client adds and removes mentioned accounts locally. Thus on client we are up-to-date with server         |
 *| *** Client makes his own account response of data that was added/removed/modified locally                    |
 *|                                                                                                              |
 *| Client sends account response containing locally deleted/added/modified accounts --------------------------->|
 *|                                                                                                              |
 *| *** Server adds or removes the client data to/from server database. Thus we are now almost synced            |
 *| *** Server expects no more data about current entities                                                       |
 *|                                                                                                              |
 *| <-------------------------------- Server replies with account acknowledge containing new timestamp-----------|
 *|                                                                                                              |
 *| *** Client updates its last sync date                                                                        |
 *| *** Client also purges contents of table that tracks deletions of synced entities                            |
 *|                                                                                                              |
 * *** Now we are synced
 *
 * *** This procedure is repeated for each entity type (accounts, operations, categories)
 *
 *
 */
public class SyncStateMachine extends Observable<SyncStateMachine.SyncListener> {
    public enum State {
        INIT,
        REGISTER(true),
        REGISTER_SENT,
        REGISTER_ACK,
        REGISTER_DENIED,

        AUTH(true),
        AUTH_SENT,
        AUTH_ACK,
        AUTH_DENIED,

        ACC_REQ(true),
        ACC_REQ_SENT,
        ACC_REQ_ACK,

        CAT_REQ(true),
        CAT_REQ_SENT,
        CAT_REQ_ACK,

        OP_REQ(true),
        OP_REQ_SENT,
        OP_REQ_ACK;


        private boolean needsAction;

        State(boolean needsAction) {
            this.needsAction = needsAction;
        }

        State() {
            needsAction = false;
        }

        public boolean isActionNeeded() {
            return needsAction;
        }
    }

    public interface SyncListener {
        void handleSyncMessage(State state, String errorMsg);
    }

    private State state;
    private Handler mHandler;
    private Socket mSocket;

    private final WalletBaseActivity mContext;
    private final SocketCallback mCallback = new SocketCallback();
    private final SharedPreferences mPreferences;


    public SyncStateMachine(WalletBaseActivity context) {
        final HandlerThread thr = new HandlerThread("ServiceThread");
        thr.start();
        mHandler = new Handler(thr.getLooper(), mCallback);

        mObservers.add(context);
        mContext = context;
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @SuppressWarnings("unchecked")
    public void notifyListeners(State state, String errorString) {
        final List<SyncListener> shadow = (List<SyncListener>) mObservers.clone();
        for(final SyncListener lsnr : shadow)
            lsnr.handleSyncMessage(state, errorString);
    }

    public void shutdown() {
        mHandler.getLooper().quit();
        if(mSocket != null)
            try {
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    public void setState(State state) {
        this.state = state;
        if(state.isActionNeeded()) // state is for internal handling, not for notifying
            mHandler.sendEmptyMessage(state.ordinal());
        else
            notifyListeners(state, null);
    }

    public void setState(State state, String errorMsg) {
        this.state = state;
        if(state.isActionNeeded())
            mHandler.sendEmptyMessage(state.ordinal());
        else
            notifyListeners(state, errorMsg);
    }

    public boolean isSyncing() {
        return state != State.INIT;
    }

    private class SocketCallback implements Handler.Callback {
        @Override
        public boolean handleMessage(Message msg) {
            final State state = State.values()[msg.what];
            try {
                switch (state) {
                    case AUTH: { // at this state, account should be already configured and accessible from preferences!
                        if (!mPreferences.contains(WalletConstants.ACCOUNT_NAME_KEY))
                            throw new RuntimeException("No account configured! Can't sync!"); // shouldn't happen
                        start();

                        final InputStream is = mSocket.getInputStream();
                        final OutputStream os = mSocket.getOutputStream();

                        sendAuthRequest(os);
                        handleAuthResponse(is);
                        break;
                    }
                    case ACC_REQ: { // at this state we must be authorized on server
                        final InputStream is = mSocket.getInputStream();
                        final OutputStream os = mSocket.getOutputStream();

                        sendLastTimestamp(os, Account.class);
                        setState(State.ACC_REQ_SENT);

                        final SyncProtocol.EntityResponse serverSide = SyncProtocol.EntityResponse.parseDelimitedFrom(is);
                        setState(State.ACC_REQ_ACK);

                        // handle modified entities - check if we updated them too...
                        for(final SyncProtocol.Entity entity : serverSide.getModifiedList()) {
                            final Account remote = Account.fromProtoEntity(entity);
                            final Account local = DbProvider.getHelper().getAccountDao().queryForId(remote.getId());
                            if(local == null) // not found on client, but exists remotely, should create on client
                                DbProvider.getHelper().getEntityDao(Account.class).createByServer(remote);
                            else if (!local.isDirty()) // updated on server but not on client, replace local with remote
                                DbProvider.getHelper().getEntityDao(Account.class).updateByServer(remote);
                            else { // update on server and on client, should resolve conflicts (just take server version for now)
                                Account merged = mergeAccounts(remote, local, (Account) local.getBackup());
                                DbProvider.getHelper().getEntityDao(Account.class).update(merged); // do not reset dirty flag
                            }
                        }

                        final SyncProtocol.EntityResponse.Builder serverUpdate = SyncProtocol.EntityResponse.newBuilder();

                        serverUpdate.build().writeDelimitedTo(os);

                        final SyncProtocol.EntityAck ack = SyncProtocol.EntityAck.parseDelimitedFrom(is);
                        setState(State.CAT_REQ);
                        break;
                    }
                    /*case CAT_REQ: {
                        final InputStream is = mSocket.getInputStream();
                        final OutputStream os = mSocket.getOutputStream();

                        sendLastTimestamp(os, Category.class);
                        setState(State.CAT_REQ_SENT);

                        final SyncProtocol.EntityResponse serverSide = SyncProtocol.EntityResponse.parseDelimitedFrom(is);
                        setState(State.CAT_REQ_ACK);

                        final SyncProtocol.EntityResponse.Builder serverUpdate = SyncProtocol.EntityResponse.newBuilder();
                        final List<String> deletedLocally = db.getDeleted(Category.class);

                        // handle modified entities - check if we updated them too...
                        for(final SyncProtocol.Entity entity : serverSide.getModifiedList()) {
                            final Category remote = Category.fromProtoCategory(entity.getCategory());
                            final Category changed = Category.getFromDB(remote.getId()); // should not be null
                            final Category base = db.getBackedVersion(remote);
                            if(base == null) // we have not modified this entity locally
                                remote.update();
                            else if (changed == null) // it's modified on server and deleted locally
                                continue; // leave deleted
                            else
                                changed.update();
                        }
                        // add to modified list local modifications
                        final List<Category> modifiedLocally = db.getModified(Category.class);
                        for(final Category cat : modifiedLocally)
                            serverUpdate.addModified(SyncProtocol.Entity.newBuilder().setCategory(Category.toProtoCategory(cat)).build());

                        // handle added entities
                        // need to delete all entities and replace it with ours...

                        // add remote
                        for(final SyncProtocol.Entity entity : serverSide.getAddedList()) {
                            final Category remote = Category.fromProtoCategory(entity.getCategory());
                            remote.persist();
                        }
                        // re-add locals + add to add-list
                        final List<Category> addedLocally = db.getAdded(Category.class);
                        for(final Category cat : addedLocally)
                            serverUpdate.addAdded(SyncProtocol.Entity.newBuilder().setCategory(Category.toProtoCategory(cat)).build());


                        // handle deleted
                        for(final String deletedID : serverSide.getDeletedIDList())
                            db.delete(deletedID, DatabaseDAO.EntityType.CATEGORIES.toString());
                        // + add to delete list entities that we deleted locally
                        for(final String deletedLocal : deletedLocally)
                            serverUpdate.addDeletedID(deletedLocal);

                        serverUpdate.build().writeDelimitedTo(os);

                        final SyncProtocol.EntityAck ack = SyncProtocol.EntityAck.parseDelimitedFrom(is);
                        setState(State.OP_REQ);
                        break;
                    }
                    case OP_REQ: {
                        final InputStream is = mSocket.getInputStream();
                        final OutputStream os = mSocket.getOutputStream();

                        sendLastTimestamp(os, Operation.class);
                        setState(State.OP_REQ_SENT);

                        final SyncProtocol.EntityResponse serverSide = SyncProtocol.EntityResponse.parseDelimitedFrom(is);
                        setState(State.OP_REQ_ACK);

                        final SyncProtocol.EntityResponse.Builder serverUpdate = SyncProtocol.EntityResponse.newBuilder();
                        final List<String> deletedLocally = db.getDeleted(Operation.class);

                        // handle modified entities - check if we updated them too...
                        for(final SyncProtocol.Entity entity : serverSide.getModifiedList()) {
                            final Operation remote = Operation.fromProtoOperation(entity.getOperation());
                            final Operation changed = Operation.getFromDB(remote.getId()); // should not be null
                            final Operation base = db.getBackedVersion(remote);
                            if(base == null) // we have not modified this entity locally
                                remote.update();
                            else if (changed == null) // it's modified on server and deleted locally
                                continue; // leave deleted
                            else { // select changed as resulting operation
                                changed.update();
                            }
                        }
                        // add to modified list local modifications
                        final List<Operation> modifiedLocally = db.getModified(Operation.class);
                        for(final Operation op : modifiedLocally)
                            serverUpdate.addModified(SyncProtocol.Entity.newBuilder().setOperation(Operation.toProtoOperation(op)).build());

                        // handle added entities
                        // need to delete all entities and replace it with ours...

                        // add remote
                        for(final SyncProtocol.Entity entity : serverSide.getAddedList()) {
                            final Operation remote = Operation.fromProtoOperation(entity.getOperation());
                            remote.persist();
                        }
                        // re-add locals + add to add-list
                        final List<Operation> addedLocally = db.getAdded(Operation.class);
                        for(final Operation op : addedLocally)
                            serverUpdate.addAdded(SyncProtocol.Entity.newBuilder().setOperation(Operation.toProtoOperation(op)).build());

                        // handle deleted
                        for(final String deletedID : serverSide.getDeletedIDList())
                            db.delete(deletedID, DatabaseDAO.EntityType.OPERATIONS.toString());
                        // + add to delete list entities that we deleted locally
                        for(final String deletedLocal : deletedLocally)
                            serverUpdate.addDeletedID(deletedLocal);

                        serverUpdate.build().writeDelimitedTo(os);

                        final SyncProtocol.EntityAck ack = SyncProtocol.EntityAck.parseDelimitedFrom(is);
                        mPreferences.edit().putLong(WalletConstants.ACCOUNT_LAST_SYNC, ack.getNewServerTimestamp()).commit();
                        finish();
                        break;
                    }*/
                }
            } catch (Exception io) {
                interrupt(io.getMessage());
            }
            return true;
        }

        private void start() throws IOException {
            /*DatabaseDAO.getInstance().beginTransaction();
            mSocket = new Socket(); // creating socket here!
            mSocket.setSoTimeout(10000);
            mSocket.connect(new InetSocketAddress(mPreferences.getString("sync.server", "anticitizen.dhis.org"), 17001));*/
        }

        private void finish() throws IOException {
            /*final DatabaseDAO db = DatabaseDAO.getInstance();
            db.clearActions();
            db.setTransactionSuccessful();
            db.endTransaction();
            mSocket.close();
            setState(State.INIT, mContext.getString(R.string.sync_completed));*/
        }

        private void interrupt(String error) {
            /*DatabaseDAO.getInstance().endTransaction();
            setState(State.INIT, error);
            if(!mSocket.isClosed())
                try {
                    mSocket.close();
                } catch (IOException e) { throw new RuntimeException(e); } // should not happen*/
        }

        private void sendLastTimestamp(OutputStream os, Class<? extends Entity> clazz) throws IOException, SQLException {
            final Long lastServerTime = DatabaseDAO.getLastServerTimestamp(clazz);

            SyncProtocol.EntityRequest.newBuilder()
                    .setLastKnownServerTimestamp(lastServerTime)
                    .build().writeDelimitedTo(os); // sent request
        }

        private void handleAuthResponse(InputStream is) throws IOException {
            final SyncResponse response = SyncResponse.parseDelimitedFrom(is);
            switch (response.getSyncAck()) {
                case OK:
                    setState(State.AUTH_ACK);
                    setState(State.ACC_REQ);
                    mPreferences.edit().putBoolean(WalletConstants.ACCOUNT_SYNC_KEY, true).commit(); // save auth
                    break;
                case AUTH_WRONG:
                    interrupt(mContext.getString(R.string.auth_invalid));
                    clearAccountInfo();
                    break;
                case ACCOUNT_EXISTS:
                    interrupt(mContext.getString(R.string.account_already_exist));
                    clearAccountInfo();
                    break;
            }
        }

        private void sendAuthRequest(OutputStream os) throws IOException {
            // fill request
            final SyncRequest.Builder request = SyncRequest.newBuilder()
                    .setAccount(mPreferences.getString(WalletConstants.ACCOUNT_NAME_KEY, ""))
                    .setPassword(mPreferences.getString(WalletConstants.ACCOUNT_PASSWORD_KEY, ""));
            if(mPreferences.getBoolean(WalletConstants.ACCOUNT_SYNC_KEY, false)) // already synchronized
                request.setSyncType(SyncRequest.SyncType.MERGE);
            else
                request.setSyncType(SyncRequest.SyncType.REGISTER);

            request.build().writeDelimitedTo(os); // actual sending of request
            os.flush();
        }
    }

    private Account mergeAccounts(Account remote, Account local, Account base) {
        final Account result = new Account();
        result.setId(local.getId());
        result.setDeleted(remote.isDeleted());
        result.setLastModified(remote.getLastModified());

        if(local.getName().equals(base.getName())) // name wasn't changed
            result.setName(remote.getName()); // set name to remote's
        else // name changed locally
            result.setName(local.getName()); // set name to local

        if(local.getDescription().equals(base.getDescription()))
            result.setDescription(remote.getDescription());
        else
            result.setDescription(local.getDescription());

        if(local.getColor().equals(base.getColor()))
            result.setColor(remote.getColor());
        else
            result.setColor(local.getColor());

        if(local.getCurrency().equals(base.getCurrency()))
            result.setCurrency(remote.getCurrency());
        else
            result.setCurrency(local.getCurrency());

        if(local.getAmount().equals(base.getAmount())) // amount wasn't changed locally
            result.setAmount(remote.getAmount());
        else // amount changed locally and remotely - get diff!
            result.setAmount(local.getAmount().subtract(base.getAmount()).add(remote.getAmount()));

        return result;
    }

    private void clearAccountInfo() {
        mPreferences.edit()
                .remove(WalletConstants.ACCOUNT_SYNC_KEY)
                .remove(WalletConstants.ACCOUNT_NAME_KEY)
                .remove(WalletConstants.ACCOUNT_PASSWORD_KEY)
                .commit();
    }
}
