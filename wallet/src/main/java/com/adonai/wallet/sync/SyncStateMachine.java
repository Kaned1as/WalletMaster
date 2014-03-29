package com.adonai.wallet.sync;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import static com.adonai.wallet.sync.SyncProtocol.SyncRequest;

/**
 * @author adonai on 25.03.14.
 */
public class SyncStateMachine {
    enum State {
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
    private Looper mLooper;
    private Handler mHandler;
    private Socket mSocket;
    private SyncListener listener;

    public SyncStateMachine(SyncListener context) {
        HandlerThread thr = new HandlerThread("ServiceThread");
        thr.start();
        mLooper = thr.getLooper();
        mHandler = new Handler(mLooper, new SocketCallback());

        listener = context;
    }

    public void startSync() {
        setState(State.AUTH);
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
            listener.handleSyncMessage(state.ordinal(), null);
    }

    public void setState(State state, String errorMsg) {
        this.state = state;
        if(state.isActionNeeded())
            mHandler.sendEmptyMessage(state.ordinal());
        else
            listener.handleSyncMessage(state.ordinal(), errorMsg);
    }

    public State getState() {
        return state;
    }

    private class SocketCallback implements Handler.Callback {
        @Override
        public boolean handleMessage(Message msg) {
            State state = State.values()[msg.what];
            try {
                switch (state) {
                    case AUTH:
                        mSocket = new Socket(); // creating socket here!
                        mSocket.connect(new InetSocketAddress("192.168.1.165", 17001));
                        final SyncRequest request = SyncRequest.newBuilder().setAccount("aahahahh").setPassword("pass").setSyncType(SyncRequest.SyncType.AUTHORIZE).build();
                        final OutputStream os = mSocket.getOutputStream();
                        request.writeTo(os); // actual sending of request
                        os.close();
                        break;
                }
            } catch (IOException io) {
                setState(State.INIT, io.getMessage());
            }
            return true;
        }
    }
}
