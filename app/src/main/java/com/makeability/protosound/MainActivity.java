package com.makeability.protosound;

import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.makeability.protosound.utils.SocketUtil;

import androidx.annotation.LongDef;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.github.nkzawa.socketio.client.Socket.EVENT_CONNECT;

public class MainActivity extends AppCompatActivity {

    public static Socket mSocket;
    private static final String DEBUG_TAG = "NetworkStatusExample";
    public static final boolean TEST_MODEL_LATENCY = false;
    public static final boolean TEST_E2E_LATENCY = false;
    private static final String TEST_E2E_LATENCY_SERVER = "http://128.208.49.41:8789";
    private static final String MODEL_LATENCY_SERVER = "http://128.208.49.41:8790";
    private static final String DEFAULT_SERVER = "http://128.208.49.41:8788";
    private static final String TEST_SERVER = "http://128.208.49.41:5000";
    private static final String TAG = "MainActivity";

    {
//        String SERVER_URL;
//        if (TEST_E2E_LATENCY) {
//            SERVER_URL = TEST_E2E_LATENCY_SERVER;
//        } else if (TEST_MODEL_LATENCY) {
//            SERVER_URL = MODEL_LATENCY_SERVER;
//        } else {
////            SERVER_URL = DEFAULT_SERVER;
//            SERVER_URL = TEST_SERVER;
//        }
        try {
            mSocket = IO.socket(TEST_SERVER);
            Log.i(TAG, "Does this thing really run?");
        } catch (URISyntaxException e) {
            Log.e(TAG, "Failed to init Socket");
            e.printStackTrace();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
//         connect socketIO
        mSocket.connect();
        mSocket.on("audio_data", onNewMessage);
        mSocket.on("android_test", onTestMessage);

        mSocket.once(EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.i(TAG, "call: " + args);
                Log.d(TAG, "call: " + mSocket.connected());
            }
        });

        Log.d(TAG, "connected: " + mSocket.connected());

        List<Short> test = new ArrayList<>();
        test.add((short) 12);
        test.add((short) 23);
        test.add((short) 34);
        test.add((short) 45);
        test.add((short) 56);
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("data", new JSONArray(test));
            jsonObject.put("time", "" + System.currentTimeMillis());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        MainActivity.mSocket.emit("android_test");
        MainActivity.mSocket.emit("audio_data", jsonObject);


        setContentView(R.layout.activity_main);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

    }


    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy: ");
        super.onDestroy();
        mSocket.disconnect();
    }

    private Emitter.Listener onNewMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            Log.i(TAG, "Received socket event");
            JSONObject data = (JSONObject) args[0];
            String db;
            String audio_label;
            String accuracy;
            String recordTime = "";
            try {
                audio_label = data.getString("label");
                accuracy = data.getString("accuracy");
                db = data.getString("db");
                if (TEST_E2E_LATENCY) {
                    recordTime = data.getString("record_time");
                }
            } catch (JSONException e) {
                Log.i(TAG, "JSON Exception failed: " + data.toString());
                return;
            }
            Log.i(TAG, "received sound label from Socket server: " + audio_label + ", " + accuracy + ", " + db);
        }
    };


    private Emitter.Listener onTestMessage = args -> {
        Log.i(TAG, "Received socket event");
    };
}