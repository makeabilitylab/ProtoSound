package com.makeability.protosound.ui.dashboard;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioFormat;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.material.textfield.TextInputEditText;
import com.makeability.protosound.MainActivity;
import com.makeability.protosound.R;
import com.makeability.protosound.utils.SocketUtil;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardFragment extends Fragment {

    private String TAG = "Dashboard";
    private static final boolean TEST = true;
    private DashboardViewModel dashboardViewModel;
    private Socket mSocket;
    private static final int RECORDER_SAMPLE_RATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    public static final String VOICE_FILE_NAME = "audiorecord_";
    SoundRecorder.OnVoicePlaybackStateChangedListener mListener;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    private String portNumber;
    private String[] labelList = {"", "", "", "", ""};
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

        Map<Integer, Integer> recordPlayMap = new HashMap<>();
        for (int i = 0; i < playButtonList.length; i++) {
            if (!recordPlayMap.containsKey(playButtonList[i])) {
                recordPlayMap.put(playButtonList[i], recordButtonList[i]);
            }
        }

        checkRecordPermission();

        // setup port number edit text
        // setup Listener for 15 record and playback buttons

        TextInputEditText portNumberEditText = (TextInputEditText) root.findViewById(R.id.port_number);
        Button confirmPort = (Button) root.findViewById(R.id.confirm_port);
        setPortNumber(portNumberEditText, confirmPort);
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

        // Set Listener for spinner
        setOnItemClickListener(spinner, 4);
        setOnItemClickListener(spinner2, 5);

        // Setup 3 edit texts for 3 classes
        TextInputEditText field_1 = (TextInputEditText) root.findViewById(R.id.class_1);
        TextInputEditText field_2 = (TextInputEditText) root.findViewById(R.id.class_2);
        TextInputEditText field_3 = (TextInputEditText) root.findViewById(R.id.class_3);
        setOnClickText(field_1, 1);
        setOnClickText(field_2, 2);
        setOnClickText(field_3, 3);

        // Setup submit button
        Button submit = (Button) root.findViewById(R.id.submit);
        setOnClickSubmit(submit, R.id.record_1);
        return root;
    }

    private void setPortNumber(TextInputEditText portNumberEditText, Button confirmPort) {
        portNumberEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String pN = s.toString();
                if (pN.length() != 4) {
                    portNumberEditText.setError("Port number need to have the format of 'XXXX'");
                }
                portNumber = pN;
            }
        });
        confirmPort.setOnClickListener(v -> {
            if (MainActivity.mSocket != null) {
                MainActivity.mSocket.disconnect();
            }
            SocketUtil socketUtil = new SocketUtil(portNumber);
            MainActivity.mSocket = socketUtil.getSocket();
            ((MainActivity) requireActivity()).initSocket(portNumber);
        });
    }


    private void setOnClickText(TextInputEditText field, int id) {
        field.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String errMsg = "";
                if (id == 1) {
                    errMsg = "1st";
                } else if (id == 2) {
                    errMsg = "2nd";
                } else {
                    errMsg = "3rd";
                }
                String labelName = s.toString();
                if (labelName.isEmpty()) {
                    field.setError("Please enter a name for your " + errMsg + " class.");
                }
                Log.d(TAG, "EditText: " + labelName);
                labelList[id - 1] = labelName;
            }
        });
    }

    private void setOnItemClickListener(final AutoCompleteTextView spinner, int spinner_id) {
        spinner.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selection = (String)parent.getItemAtPosition(position);
                Log.d(TAG, "spinner " + spinner_id + ": " + selection);
                labelList[spinner_id - 1] = selection;
            }
        });
    }


    private void setOnClickRecord(final Button btn, final int id){

        btn.setOnClickListener(v -> {
            btn.setBackgroundColor(Color.GREEN);
            Toast.makeText(requireActivity(), "Recording.." , Toast.LENGTH_SHORT).show();
            Log.d(TAG, "record:"+ id + " called");
            startRecording(id);
//            Timer timer = new Timer();
//            timer.schedule(new TimerTask() {
//                @Override
//                public void run() {
//                    stopRecording();
//                    timer.cancel();
//                }
//            }, 1000);
        });
    }

    private void setOnClickPlay(final Button btn, final int id){

        btn.setOnClickListener(v -> {
            Log.d(TAG, "play:" + id + " called");
            Toast.makeText(requireActivity(), "Playing.." , Toast.LENGTH_SHORT).show();
            startPlay(id);
        });
    }


    private void setOnClickSubmit(final Button btn, final int id){

        btn.setOnClickListener(v -> {
            Log.d(TAG, "Submit to Server");
            try {
                if (!checkAllFieldsExisted()) {
                    return;
                }
                JSONObject soundPackage = new JSONObject();

                for (int i = 0; i < recordButtonList.length; i++) {
                    byte[] byteArray = getBytesFromPCM(VOICE_FILE_NAME + recordButtonList[i] + ".pcm");
                    short[] shortArray = convertByteArrayToShortArray(byteArray);
                    List<Short> soundBuffer = new ArrayList<>();
                    for (short num : shortArray) {
                        soundBuffer.add(num);
                    }
                    soundPackage.put("data_" + i, new JSONArray(soundBuffer));
                }
                List<String> labels = new ArrayList<>(Arrays.asList(labelList));
                soundPackage.put("label", new JSONArray(labels));
                Log.d(TAG, "Socket connect:" + MainActivity.mSocket.connected());
                MainActivity.mSocket.emit("submit_data", soundPackage);
            } catch (FileNotFoundException | JSONException e) {
                e.printStackTrace();
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

    private boolean checkAllFieldsExisted() {
        for (int rid : recordButtonList) {
            String filename = VOICE_FILE_NAME + rid + ".pcm";
            if (!new File(requireActivity().getFilesDir(), filename).exists()) {
                Log.d(TAG, "File not exist: " + filename);
                Toast.makeText(requireActivity(), "Missing one or more record samples", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        for (int i = 0; i < labelList.length; i++) {
            if (labelList[i].isEmpty()) {
                Log.d(TAG, "Label not exist: " + i);
                Toast.makeText(requireActivity(), "Missing one or more labels", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    private byte[] getBytesFromPCM(String filename) throws FileNotFoundException {
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

    private void sendRawAudioToServer(List<Short> soundBuffer, int id) {
        try {
            JSONObject jsonObject = new JSONObject();

            jsonObject.put("data", new JSONArray(soundBuffer));
            jsonObject.put("time", "" + System.currentTimeMillis());
            Log.i(TAG, "Send raw audio to server:");
            Log.i(TAG, "Connected: " + MainActivity.mSocket.connected());
//            emitter
            MainActivity.mSocket.emit("android_test");
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

    private int[] convertByteArrayToUnsignedByteArray(byte[] bytes) {
        int[] unsigned = new int[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            unsigned[i] = bytes[i] & 0xFF;
        }
        return unsigned;
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

    public Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.d(TAG, "Socket Connected!");
        }
    };

    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {


                }
            });
        }
    };
    private Emitter.Listener onDisconnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {


                }
            });
        }
    };

    /**
     * Write PCM data as WAV file
     * @param os  Stream to save file to
     * @param list List of byte array audio data
     * @param srate  Sample rate - 8000, 16000, etc.
     * @param channel Number of channels - Mono = 1, Stereo = 2, etc..
     * @param format Number of bits per sample (16 here)
     * @throws IOException
     */
    public void PCMtoFile(FileOutputStream os, List<byte[]> list, int srate, int channel, int format) throws IOException {

        // create byte[] data from a list of data from watch
        int len = 0;
        for (int i = 0; i < list.size(); i++) {
            len += list.get(i).length;
        }
        byte[] data = new byte[len];
        int k = 0;
        for (int i = 0; i < list.size();i++) {
            for (int j = 0; j <list.get(i).length; j++) {
                data[k] = list.get(i)[j];
                k++;
            }
        }

        byte[] header = new byte[44];

        long totalDataLen = data.length + 36;
        long bitrate = srate * channel * format;

        header[0] = 'R';
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = (byte) format;
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;
        header[21] = 0;
        header[22] = (byte) channel;
        header[23] = 0;
        header[24] = (byte) (srate & 0xff);
        header[25] = (byte) ((srate >> 8) & 0xff);
        header[26] = (byte) ((srate >> 16) & 0xff);
        header[27] = (byte) ((srate >> 24) & 0xff);
        header[28] = (byte) ((bitrate / 8) & 0xff);
        header[29] = (byte) (((bitrate / 8) >> 8) & 0xff);
        header[30] = (byte) (((bitrate / 8) >> 16) & 0xff);
        header[31] = (byte) (((bitrate / 8) >> 24) & 0xff);
        header[32] = (byte) ((channel * format) / 8);
        header[33] = 0;
        header[34] = 16;
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (data.length  & 0xff);
        header[41] = (byte) ((data.length >> 8) & 0xff);
        header[42] = (byte) ((data.length >> 16) & 0xff);
        header[43] = (byte) ((data.length >> 24) & 0xff);

        os.write(header, 0, 44);
        os.write(data);
        os.close();
    }

}