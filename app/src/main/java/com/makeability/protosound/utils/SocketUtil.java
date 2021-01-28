package com.makeability.protosound.utils;


import android.util.Log;

import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import java.net.URISyntaxException;

public class SocketUtil {
    private Socket mSocket;
    private String TAG = "SocketUtil";
    private static final String SERVER_URL = "http://128.208.49.41:";
    private String port;

    public SocketUtil(String port) {
        this.port = port;
        try {
            mSocket = IO.socket(SERVER_URL + this.port);
        } catch (URISyntaxException e) {
            Log.e(TAG, "Failed to init Socket");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public Socket getSocket() {
        return mSocket;
    }
}
