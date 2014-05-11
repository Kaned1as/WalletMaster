package com.adonai.wallet.sync;

import android.content.SharedPreferences;
import android.database.Observable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;

import com.adonai.wallet.DatabaseDAO;
import com.adonai.wallet.R;
import com.adonai.wallet.WalletBaseActivity;
import com.adonai.wallet.WalletConstants;
import com.adonai.wallet.entities.Account;
import com.adonai.wallet.entities.Entity;
import com.adonai.wallet.entities.Operation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
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
 *| *** Server parses account request and makes a list of added/removed IDs on server based on its data          |
 *| *** Added accounts are transferred fully and contain GUIDs if present, removed accounts contain only GUIDs   |
 *|                                                                                                              |
 *| <--------------------------------- Server replies with account response containing added/removed accounts    |
 *|                                                                                                              |
 *| *** Client adds and removes mentioned accounts locally. Thus on client we are up-to-date with server         |
 *| *** Client makes his own account response of data that was added/removed locally                             |
 *|                                                                                                              |
 *| Client sends account response containing locally deleted/added accounts ------------------------------------>|
 *|                                                                                                              |
 *| *** Server adds or removes the client data to/from server database. Thus we are now almost synced            |
 *| *** At te end we must send to the client data about GUIDs of newly added accounts                            |
 *| *** Server expects no more data about current entities                                                       |
 *|                                                                                                              |
 *| <-------------------------------- Server replies with account acknowledge containing GUIDs of added accounts.|
 *|                                                                                                              |
 *| *** Client updates its accounts with the data from last packet.                                              |
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

        OP_REQ(true),
        OP_REQ_SENT,
        OP_REQ_ACK,

        CAT_REQ(true),
        CAT_REQ_SENT,
        CAT_REQ_ACK;


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
    private final Looper mLooper;
    private final Handler mHandler;
    private Socket mSocket;

    private final WalletBaseActivity mContext;
    private final SharedPreferences mPreferences;


    public SyncStateMachine(WalletBaseActivity context) {
        final HandlerThread thr = new HandlerThread("ServiceThread");
        thr.start();
        mLooper = thr.getLooper();
        mHandler = new Handler(mLooper, new SocketCallback());

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
        if(mSocket != null)
            try {
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        mLooper.quit();
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

    public State getState() {
        return state;
    }

    private class SocketCallback implements Handler.Callback {
        @Override
        public boolean handleMessage(Message msg) {
            final State state = State.values()[msg.what];
            try {
                switch (state) {
                    case AUTH: { // at this state, account should be already configured and accessible from preferences!
                        if (!mPreferences.contains(WalletConstants.ACCOUNT_NAME_KEY))
                            throw new RuntimeException("No account configured! Can't sync!");

                        mSocket = new Socket(); // creating socket here!
                        mSocket.connect(new InetSocketAddress("10.0.2.2", 17001));

                        final InputStream is = mSocket.getInputStream();
                        final OutputStream os = mSocket.getOutputStream();

                        sendAuthRequest(os);
                        handleAuthResponse(is);
                        break;
                    }
                    case ACC_REQ: { // at this state we must be authorized on server
                        final InputStream is = mSocket.getInputStream();
                        final OutputStream os = mSocket.getOutputStream();

                        sendKnownEntities(os, Account.class);
                        setState(State.ACC_REQ_SENT);

                        final SyncProtocol.EntityResponse serverSide = SyncProtocol.EntityResponse.parseDelimitedFrom(is);
                        setState(State.ACC_REQ_ACK);

                        final SyncProtocol.EntityResponse.Builder serverUpdate = SyncProtocol.EntityResponse.newBuilder();
                        final List<Account> addedLocally = mContext.getEntityDAO().getAdded(Account.class);
                        final List<String> deletedLocally = mContext.getEntityDAO().getDeleted(Account.class);

                        // handle modified entities - check if we updated them too...
                        for(final SyncProtocol.Entity entity : serverSide.getModifiedList()) {
                            final Account remote = Account.fromProtoAccount(entity.getAccount());
                            final Account changed = Account.getFromDB(mContext.getEntityDAO(), remote.getId()); // should not be null
                            final Account base = mContext.getEntityDAO().getBackedVersion(remote);
                            if(base == null) // we have not modified this entity locally
                                remote.update(mContext.getEntityDAO());
                            else if (changed == null) // it's modified on server and deleted locally
                                continue; // leave deleted
                            else { // changed and base are present, merge them + add to mod-list later
                                final Account result = mergeAccounts(remote, changed, base);
                                result.update(mContext.getEntityDAO());
                            }
                        }
                        // add to modified list local modifications
                        final List<Account> modifiedLocally = mContext.getEntityDAO().getModified(Account.class);
                        for(final Account acc : modifiedLocally)
                            serverUpdate.addModified(SyncProtocol.Entity.newBuilder().setAccount(Account.toProtoAccount(acc)).build());

                        // handle added entities
                        // need to delete all entities and replace it with ours...

                        // add remote
                        for(final SyncProtocol.Entity entity : serverSide.getAddedList()) {
                            final Account remote = Account.fromProtoAccount(entity.getAccount());
                            remote.persist(mContext.getEntityDAO());
                        }
                        // readd locals + add to add-list
                        for(final Account acc : addedLocally)
                            serverUpdate.addAdded(SyncProtocol.Entity.newBuilder().setAccount(Account.toProtoAccount(acc)).build());


                        // handle deleted
                        for(final String deletedID : serverSide.getDeletedIDList())
                            mContext.getEntityDAO().delete(deletedID, DatabaseDAO.EntityType.ACCOUNTS.toString());
                        // + add to delete list entities that we deleted locally
                        for(final String deletedLocal : deletedLocally)
                            serverUpdate.addDeletedID(deletedLocal);

                        serverUpdate.build().writeDelimitedTo(os);

                        final SyncProtocol.EntityAck ack = SyncProtocol.EntityAck.parseDelimitedFrom(is);
                        setState(State.OP_REQ);
                        break;
                    }
                    case CAT_REQ: {
                        final InputStream is = mSocket.getInputStream();
                        final OutputStream os = mSocket.getOutputStream();


                        break;
                    }
                    case OP_REQ: {
                        final InputStream is = mSocket.getInputStream();
                        final OutputStream os = mSocket.getOutputStream();

                        sendKnownEntities(os, Operation.class);
                        setState(State.OP_REQ_SENT);

                        final SyncProtocol.EntityResponse serverSide = SyncProtocol.EntityResponse.parseDelimitedFrom(is);
                        setState(State.OP_REQ_ACK);

                        final SyncProtocol.EntityResponse.Builder serverUpdate = SyncProtocol.EntityResponse.newBuilder();
                        final List<Operation> addedLocally = mContext.getEntityDAO().getAdded(Operation.class);
                        final List<String> deletedLocally = mContext.getEntityDAO().getDeleted(Operation.class);

                        // handle modified entities - check if we updated them too...
                        for(final SyncProtocol.Entity entity : serverSide.getModifiedList()) {
                            final Operation remote = Operation.fromProtoOperation(entity.getOperation(), mContext.getEntityDAO());
                            final Operation changed = Operation.getFromDB(mContext.getEntityDAO(), remote.getId()); // should not be null
                            final Operation base = mContext.getEntityDAO().getBackedVersion(remote);
                            if(base == null) // we have not modified this entity locally
                                remote.update(mContext.getEntityDAO());
                            else if (changed == null) // it's modified on server and deleted locally
                                continue; // leave deleted
                            else { // select changed as resulting operation
                                changed.update(mContext.getEntityDAO());
                            }
                        }
                        // add to modified list local modifications
                        final List<Operation> modifiedLocally = mContext.getEntityDAO().getModified(Operation.class);
                        for(final Operation op : modifiedLocally)
                            serverUpdate.addModified(SyncProtocol.Entity.newBuilder().setOperation(Operation.toProtoOperation(op)).build());

                        // handle added entities
                        // need to delete all entities and replace it with ours...

                        // add remote
                        for(final SyncProtocol.Entity entity : serverSide.getAddedList()) {
                            final Operation remote = Operation.fromProtoOperation(entity.getOperation(), mContext.getEntityDAO());
                            remote.persist(mContext.getEntityDAO());
                        }
                        // readd locals + add to add-list
                        for(final Operation op : addedLocally)
                            serverUpdate.addAdded(SyncProtocol.Entity.newBuilder().setOperation(Operation.toProtoOperation(op)).build());

                        // handle deleted
                        for(final String deletedID : serverSide.getDeletedIDList())
                            mContext.getEntityDAO().delete(deletedID, DatabaseDAO.EntityType.OPERATIONS.toString());
                        // + add to delete list entities that we deleted locally
                        for(final String deletedLocal : deletedLocally)
                            serverUpdate.addDeletedID(deletedLocal);

                        serverUpdate.build().writeDelimitedTo(os);

                        final SyncProtocol.EntityAck ack = SyncProtocol.EntityAck.parseDelimitedFrom(is);
                        mContext.getEntityDAO().clearActions();
                        mPreferences.edit().putLong(WalletConstants.ACCOUNT_LAST_SYNC, ack.getNewServerTimestamp()).commit();
                        setState(State.INIT, mContext.getString(R.string.sync_completed));
                        break;
                    }
                }
            } catch (IOException io) {
                setState(State.INIT, io.getMessage());
                if(!mSocket.isClosed())
                    try {
                        mSocket.close();
                    } catch (IOException e) { throw new RuntimeException(e); } // should not happen
            }
            return true;
        }

        private void sendKnownEntities(OutputStream os, Class<? extends Entity> clazz) throws IOException {
            final Long lastServerTime = mPreferences.getLong(WalletConstants.ACCOUNT_LAST_SYNC, 0);
            final List<String> knownIDs = mContext.getEntityDAO().getKnownIDs(clazz);

            SyncProtocol.EntityRequest.newBuilder()
                    .setLastKnownServerTimestamp(lastServerTime)
                    .addAllKnownID(knownIDs)
                    .build().writeDelimitedTo(os); // sent account request
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
                    setState(State.INIT, mContext.getString(R.string.auth_invalid));
                    clearAccountInfo();
                    mSocket.close();
                    break;
                case ACCOUNT_EXISTS:
                    setState(State.INIT, mContext.getString(R.string.account_already_exist));
                    clearAccountInfo();
                    mSocket.close();
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

        private void clearAccountInfo() {
            mPreferences.edit()
                    .remove(WalletConstants.ACCOUNT_SYNC_KEY)
                    .remove(WalletConstants.ACCOUNT_NAME_KEY)
                    .remove(WalletConstants.ACCOUNT_PASSWORD_KEY)
                    .commit();
        }
    }

    private Account mergeAccounts(Account remote, Account local, Account base) {
        final Account result = new Account();
        result.setId(local.getId());

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

}
