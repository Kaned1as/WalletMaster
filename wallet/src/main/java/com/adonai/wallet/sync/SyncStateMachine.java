package com.adonai.wallet.sync;

import android.content.SharedPreferences;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import static com.adonai.wallet.sync.SyncProtocol.AccountRequest;
import static com.adonai.wallet.sync.SyncProtocol.AccountResponse;
import static com.adonai.wallet.sync.SyncProtocol.SyncRequest;
import static com.adonai.wallet.sync.SyncProtocol.SyncResponse;

/**
 * @author adonai on 25.03.14.
 */
public class SyncStateMachine {
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
        ACC_REQ_ACK;

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
        void handleSyncMessage(int what, String errorMsg);
    }

    private State state;
    private final Looper mLooper;
    private final Handler mHandler;
    private Socket mSocket;
    private final List<SyncListener> mListeners = new ArrayList<>(2);
    private final WalletBaseActivity mContext;
    private final SharedPreferences mPreferences;

    public SyncStateMachine(WalletBaseActivity context) {
        HandlerThread thr = new HandlerThread("ServiceThread");
        thr.start();
        mLooper = thr.getLooper();
        mHandler = new Handler(mLooper, new SocketCallback());

        mListeners.add(context);
        mContext = context;
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void registerSyncListener(SyncListener listener) {
        mListeners.add(listener);
    }

    public void unregisterSyncListener(SyncListener listener) {
        mListeners.remove(listener);
    }

    public void notifyListeners(int what, String errorString) {
        for(final SyncListener lsnr : mListeners)
            lsnr.handleSyncMessage(what, errorString);
    }

    public void shutdown() {
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
            notifyListeners(state.ordinal(), null);
    }

    public void setState(State state, String errorMsg) {
        this.state = state;
        if(state.isActionNeeded())
            mHandler.sendEmptyMessage(state.ordinal());
        else
            notifyListeners(state.ordinal(), errorMsg);
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
                    case AUTH: // at this state, account should be already configured and accessible from preferences!
                        if(!mPreferences.contains(WalletConstants.ACCOUNT_NAME_KEY))
                            throw new RuntimeException("No account configured! Can't sync!");

                        mSocket = new Socket(); // creating socket here!
                        mSocket.connect(new InetSocketAddress("10.0.2.2", 17001));

                        sendAuthRequest();
                        handleAuthResponse();
                        break;
                    case ACC_REQ: // at this state we must be authorized on server
                        final InputStream is = mSocket.getInputStream(); // try receive response
                        final OutputStream os = mSocket.getOutputStream();

                        // send account request with all already synced accounts
                        final AccountRequest.Builder request = AccountRequest.newBuilder()
                                .addAllKnownID(mContext.getEntityDAO().getSyncHelper().getKnownGUIDs(DatabaseDAO.ACCOUNTS_TABLE_NAME));
                        request.build().writeDelimitedTo(os); // actual sending of request
                        os.flush();
                        setState(State.ACC_REQ_SENT);

                        // accept account response
                        final AccountResponse response = AccountResponse.parseDelimitedFrom(is);
                        // delete accounts (that were deleted on server) locally
                        final List<Long> deletedAccounts = response.getDeletedIDList();
                        for(final Long deleted : deletedAccounts)
                            mContext.getEntityDAO().getSyncHelper().deleteByGuid(DatabaseDAO.ACCOUNTS_TABLE_NAME, deleted);
                        // add accounts (that were added on server) locally
                        final List<SyncProtocol.Account> accounts = response.getAccountList();
                        for(SyncProtocol.Account acc : accounts) {
                            final Account toDatabase = Account.fromProtoAccount(acc);
                            mContext.getEntityDAO().addAccount(toDatabase);
                        }
                        setState(State.ACC_REQ_ACK);

                        // send non-synced accounts to server
                        final List<Account> nsAccs = mContext.getEntityDAO().getSyncHelper().getNonSyncedAccounts();
                        final AccountResponse.Builder toSend = AccountResponse.newBuilder();
                        for(final Account acc : nsAccs)
                            toSend.addAccount(Account.toProtoAccount(acc));
                        // send deleted accounts to server
                        final List<Long> delAccs = mContext.getEntityDAO().getSyncHelper().getDeletedGUIDs(DatabaseDAO.TYPE_ACCOUNT);
                        toSend.addAllDeletedID(delAccs);
                        toSend.build().writeDelimitedTo(os);
                        os.flush();

                        // get confirmation of sync
                        final SyncProtocol.AccountAck ack = SyncProtocol.AccountAck.parseDelimitedFrom(is);
                        final List<Long> ackAccounts = ack.getWrittenGuidList();
                        assert nsAccs.size() == ackAccounts.size();
                        for(int i = 0; i < nsAccs.size(); ++i) { // add GUIDs to previously non-synced accs
                            final Account curr = nsAccs.get(i);
                            curr.setGuid(ackAccounts.get(i));
                            mContext.getEntityDAO().updateAccount(curr);
                        }
                        mContext.getEntityDAO().getSyncHelper().purgeDeletedGUIDs(DatabaseDAO.TYPE_ACCOUNT);
                        break;
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

        private void handleAuthResponse() throws IOException {
            final InputStream is = mSocket.getInputStream(); // try receive response
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

        private void sendAuthRequest() throws IOException {
            // fill request
            final SyncRequest.Builder request = SyncRequest.newBuilder()
                    .setAccount(mPreferences.getString(WalletConstants.ACCOUNT_NAME_KEY, ""))
                    .setPassword(mPreferences.getString(WalletConstants.ACCOUNT_PASSWORD_KEY, ""));
            if(mPreferences.getBoolean(WalletConstants.ACCOUNT_SYNC_KEY, false)) // already synchronized
                request.setSyncType(SyncRequest.SyncType.MERGE);
            else
                request.setSyncType(SyncRequest.SyncType.REGISTER);

            final OutputStream os = mSocket.getOutputStream(); // send request
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
}
