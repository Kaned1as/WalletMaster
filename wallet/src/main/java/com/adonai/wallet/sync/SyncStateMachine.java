package com.adonai.wallet.sync;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.adonai.wallet.R;
import com.adonai.wallet.WalletBaseActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

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
    private final Context mContext;

    public SyncStateMachine(WalletBaseActivity context) {
        HandlerThread thr = new HandlerThread("ServiceThread");
        thr.start();
        mLooper = thr.getLooper();
        mHandler = new Handler(mLooper, new SocketCallback());

        mListeners.add(context);
        mContext = context;
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
                    case AUTH:
                        mSocket = new Socket(); // creating socket here!
                        mSocket.connect(new InetSocketAddress("192.168.1.165", 17001));
                        final SyncRequest request = SyncRequest.newBuilder().setAccount("aahahahh").setPassword("pass").setSyncType(SyncRequest.SyncType.REGISTER).build();
                        final OutputStream os = mSocket.getOutputStream(); // send request
                        request.writeDelimitedTo(os); // actual sending of request
                        os.flush();

                        final InputStream is = mSocket.getInputStream(); // try receive response
                        final SyncResponse response = SyncResponse.parseDelimitedFrom(is);
                        is.close();
                        os.close();
                        switch (response.getSyncAck()) {
                            case OK:
                                setState(State.AUTH_ACK);
                                break;
                            case AUTH_WRONG:
                                setState(State.INIT, mContext.getString(R.string.auth_invalid));
                                mSocket.close();
                                break;
                            case ACCOUNT_EXISTS:
                                setState(State.INIT, mContext.getString(R.string.account_already_exist));
                                mSocket.close();
                                break;
                        }
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
    }
}
