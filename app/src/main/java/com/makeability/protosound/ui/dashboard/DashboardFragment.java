package com.makeability.protosound.ui.dashboard;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.rtp.AudioStream;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputLayout;
import com.makeability.protosound.MainActivity;
import com.makeability.protosound.R;
import com.makeability.protosound.utils.SoundRecorder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import static com.makeability.protosound.MainActivity.TEST_E2E_LATENCY;

public class DashboardFragment extends Fragment {

    private DashboardViewModel dashboardViewModel;
    private static final int RECORDER_SAMPLE_RATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    public static final String VOICE_FILE_NAME = "audiorecord_";
    SoundRecorder.OnVoicePlaybackStateChangedListener mListener;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    int BytesPerElement = 2; // 2 bytes in 16bit format
    String TAG = "Dashboard";
    SoundRecorder recorder;
    int[] recordButtonList = {R.id.record_1, R.id.record_2, R.id.record_3, R.id.record_4,
            R.id.record_5, R.id.record_6, R.id.record_7, R.id.record_8, R.id.record_9,
            R.id.record_10, R.id.record_11, R.id.record_12, R.id.record_13, R.id.record_14, R.id.record_15, R.id.record_bg};

    int[] playButtonList = {R.id.play_1, R.id.play_2, R.id.play_3, R.id.play_4, R.id.play_5,
    R.id.play_6, R.id.play_7, R.id.play_8, R.id.play_9, R.id.play_10, R.id.play_11,
            R.id.play_12, R.id.play_13, R.id.play_14, R.id.play_15, R.id.play_bg};

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        dashboardViewModel =
                new ViewModelProvider(this).get(DashboardViewModel.class);
        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);
//        final TextView textView = root.findViewById(R.id.text_dashboard);
//        dashboardViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
//            @Override
//            public void onChanged(@Nullable String s) {
//                textView.setText(s);
//            }
//        });

        Map<Integer, Integer> recordPlayMap = new HashMap<>();
        for (int i = 0; i < playButtonList.length; i++) {
            if (!recordPlayMap.containsKey(playButtonList[i])) {
                recordPlayMap.put(playButtonList[i], recordButtonList[i]);
            }
        }

        checkRecordPermission();

        // setup Listener for 15 record and playback buttons
        for (int rid : recordButtonList) {
            Button record = root.findViewById(rid);
            setOnClickRecord(record, rid);
        }

        for (int pid: playButtonList) {
            Button play = root.findViewById(pid);
            setOnClickPlay(play, recordPlayMap.get(pid));
        }

        // setup 2 Spinner drop-down lists for pre-determined sounds
        AutoCompleteTextView spinner = (AutoCompleteTextView) root.findViewById(R.id.menu);
        AutoCompleteTextView spinner2 = (AutoCompleteTextView) root.findViewById(R.id.menu2);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireActivity(),
                R.array.sound_array, android.R.layout.simple_spinner_dropdown_item);
        // Specify the layout to use when the list of choices appears
//        adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        spinner2.setAdapter(adapter);

        Button submit = (Button) root.findViewById(R.id.submit);
        setOnClickTestSubmit(submit, R.id.submit);
        return root;
    }

    private void setOnClickRecord(final Button btn, final int id){

        btn.setOnClickListener(v -> {
            btn.setBackgroundColor(Color.GREEN);

            Log.d(TAG, "record:"+ id + " called");
            startRecording(id);
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    stopRecording();
                    timer.cancel();
                }
            }, 1000);
        });
    }

    private void setOnClickPlay(final Button btn, final int id){

        btn.setOnClickListener(v -> {
            Log.d(TAG, "play:" + id + " called");
            startPlay(id);
        });
    }

    private void setOnClickTestSubmit(final Button btn, final int id){

        btn.setOnClickListener(v -> {
            Log.d(TAG, "Test Submit to Server");
            List<Short> test = new ArrayList<>();
            test.add((short) 69);
            test.add((short) 69);
            test.add((short) 69);
            test.add((short) 69);
            test.add((short) 69);
            Log.d(TAG, "setOnClickTestSubmit: " + test);
            sendRawAudioToServer(test);
        });
    }

    private void setOnClickSubmit(final Button btn, final int id){

        btn.setOnClickListener(v -> {
            Log.d(TAG, "Submit to Server");
            for (int rid: recordButtonList) {
                try {
                    byte[] byteArray = getBytesFromWave(VOICE_FILE_NAME + rid + ".pcm");
                    short[] shortArray = convertByteArrayToShortArray(byteArray);
                    List<Short> soundBuffer = new ArrayList<>();
                    for (short num : shortArray) {
                        soundBuffer.add(num);
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void startRecording(int id) {
        Log.d(TAG, "startRecording: ");
        recorder = new SoundRecorder(this.requireContext(), VOICE_FILE_NAME + id + ".pcm", mListener);
        recorder.startRecording();
    }

    private void stopRecording() {
        // stops the recording activity
        Log.d(TAG, "stopRecording: ");
        if (null != recorder) {
            recorder.stopRecording();
        }
    }

    private void startPlay(int id) {
        String audioFile = VOICE_FILE_NAME + id + ".pcm";
        if (null != recorder) {
            recorder.changeOutputFileName(audioFile);
            Log.d(TAG, "startPlay: ");
            recorder.startPlay();
        }
    }

    private byte[] getBytesFromWave(String filename) throws FileNotFoundException {
        if (!new File(requireActivity().getFilesDir(), filename).exists()) {
            return new byte[0];
        }
        FileInputStream in = requireActivity().openFileInput(filename);
        BufferedInputStream bis = new BufferedInputStream(in);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] bytes = new byte[1];
        byte[] dataBuffer = new byte[1024 * 16];
        int size = 0;
        try {
            while ((size = bis.read(dataBuffer)) != -1) {
//                Log.d(TAG, "getBytesFromWave: " + size +":"+ Arrays.toString(dataBuffer));
                baos.write(dataBuffer, 0, size);
            }
            bytes = baos.toByteArray();
        } catch (IOException e) {
            Log.e(TAG, "getBytesFromWave: Failed to read the sound file into a byte array", e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (bis != null) {
                    bis.close();
                }
                if (baos != null) {
                    baos.close();
                }
            } catch (IOException e) { /* ignore */}
        }
        return bytes;
    }

    private void sendRawAudioToServer(List<Short> soundBuffer) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("data", new JSONArray(soundBuffer));
            jsonObject.put("time", "" + System.currentTimeMillis());
            Log.i(TAG, "Send raw audio to server");
            Log.i(TAG, "Connected: " + MainActivity.mSocket.connected());
            MainActivity.mSocket.emit("audio_data", jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void checkRecordPermission() {

        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.RECORD_AUDIO},
                    PERMISSIONS_REQUEST_CODE);
        }
    }


    private short[] convertByteArrayToShortArray(byte[] bytes) {
//        Log.i(TAG, "convertByteArrayToShortArray()");
        short[] result = new short[bytes.length / 2];
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(result);
        return result;
    }


    //Conversion of short to byte
    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];

        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            } else {
                // Permission has been denied before. At this point we should show a dialog to
                // user and explain why this permission is needed and direct him to go to the
                // Permissions settings for the app in the System settings. For this sample, we
                // simply exit to get to the important part.
                Toast.makeText(getActivity(), "Need Audio access to start streaming and recognizing surrounding voices", Toast.LENGTH_LONG).show();
                requireActivity().finish();
            }
        }
    }

}