package com.makeability.protosound.ui.dashboard;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioFormat;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TableRow;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import org.greenrobot.eventbus.EventBus;
import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import com.chaquo.python.PyObject;
import com.google.android.material.textfield.TextInputEditText;
import com.makeability.protosound.MainActivity;
import com.makeability.protosound.R;
import com.makeability.protosound.utils.ProtoApp;
import com.makeability.protosound.utils.SoundRecorder;
import com.makeability.protosound.utils.ProtoModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
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


import static com.makeability.protosound.MainActivity.TEST_END_TO_END_TRAINING_LATENCY_MODE;

public class DashboardFragment extends Fragment {
    private String TAG = "Dashboard";
    private ProtoApp model;
    private PyObject protosoundApp;
    private Module module;
    public String location;
    public String submitAudioTime;

    private static final boolean TEST = true;
    private DashboardViewModel dashboardViewModel;
    private static final int RECORDER_SAMPLE_RATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    public static final String VOICE_FILE_NAME = "audiorecord_";
    SoundRecorder.OnVoicePlaybackStateChangedListener mListener;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    private String portNumber;
    private String testingLocation = "";
    private String[] labelList = {"", "", "", "", ""};
    private int countUserClass = 0;
    SoundRecorder recorder;
    int[] recordButtonList = {R.id.record_1, R.id.record_2, R.id.record_3, R.id.record_4, R.id.record_5,
            R.id.record_6, R.id.record_7, R.id.record_8, R.id.record_9, R.id.record_10,
            R.id.record_11, R.id.record_12, R.id.record_13, R.id.record_14, R.id.record_15,
            R.id.record_16, R.id.record_17, R.id.record_18, R.id.record_19, R.id.record_20,
            R.id.record_21, R.id.record_22, R.id.record_23, R.id.record_24, R.id.record_25, R.id.record_bg};

    int[] playButtonList = {R.id.play_1, R.id.play_2, R.id.play_3, R.id.play_4, R.id.play_5,
            R.id.play_6, R.id.play_7, R.id.play_8, R.id.play_9, R.id.play_10,
            R.id.play_11, R.id.play_12, R.id.play_13, R.id.play_14, R.id.play_15,
            R.id.play_16, R.id.play_17, R.id.play_18, R.id.play_19, R.id.play_20,
            R.id.play_21, R.id.play_22, R.id.play_23, R.id.play_24, R.id.play_25, R.id.play_bg};

    int[] rowSelectAList = {R.id.row_1_select_a, R.id.row_2_select_a, R.id.row_3_select_a, R.id.row_4_select_a, R.id.row_5_select_a};
    int[] rowSelectBList = {R.id.row_1_select_b, R.id.row_2_select_b, R.id.row_3_select_b, R.id.row_4_select_b, R.id.row_5_select_b};

    int[] rowRecordList = {R.id.row_1_record, R.id.row_2_record, R.id.row_3_record, R.id.row_4_record, R.id.row_5_record};
    int[] rowPlayList = {R.id.row_1_play, R.id.row_2_play, R.id.row_3_play, R.id.row_4_play, R.id.row_5_play};

    int[] selectAButtonList = {R.id.select_1a, R.id.select_2a, R.id.select_3a, R.id.select_4a, R.id.select_5a};
    int[] selectBButtonList = {R.id.select_1b, R.id.select_2b, R.id.select_3b, R.id.select_4b, R.id.select_5b};
    int[] selection = {R.id.selection_1, R.id.selection_2, R.id.selection_3, R.id.selection_4, R.id.selection_5};
    int[] menuList = {R.id.menu_1, R.id.menu_2, R.id.menu_3, R.id.menu_4, R.id.menu_5};
    int[] classNameList = {R.id.class_1, R.id.class_2, R.id.class_3, R.id.class_4, R.id.class_5};

    boolean[] sampleRecorded = new boolean[26];
    int[] predefinedSamples = new int[26];

    // Override onAttach to make sure Fragment is attached to an Activity first
    // to avoid getapplicationcontext returning null
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.model = (ProtoApp) context.getApplicationContext();
        this.protosoundApp = model.getProtosoundApp();
        this.module = model.getModule();
    }

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
        TextInputEditText locationEditText = (TextInputEditText) root.findViewById(R.id.testing_location);
        Button confirmLocation = root.findViewById(R.id.confirm_location);
        setLocation(locationEditText, confirmLocation);

        for (int i = 0; i < recordButtonList.length; i++) {
            Button record = root.findViewById(recordButtonList[i]);
            setOnClickRecord(record, recordButtonList[i], i);
        }

        for (int pid : playButtonList) {
            Button play = root.findViewById(pid);
            setOnClickPlay(play, recordPlayMap.get(pid));
        }


        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireActivity(),
                R.array.sound_array, android.R.layout.simple_spinner_dropdown_item);
        // setup Spinner drop-down lists for pre-determined sounds
        for (int i = 0; i < menuList.length; i++) {
            AutoCompleteTextView spinner = (AutoCompleteTextView) root.findViewById(menuList[i]);
            // Specify the layout to use when the list of choices appears
            // Apply the adapter to the spinner
            spinner.setAdapter(adapter);
            // Set Listener for spinner
            setSelectItemList(spinner, i+1);
        }

        // Setup 3 edit texts for 3 classes
        for (int i = 0; i < classNameList.length; i++) {
            TextInputEditText classTextField = (TextInputEditText) root.findViewById(classNameList[i]);
            setOnClickText(classTextField, i+1);
        }

        // Setup submit button
        Button submit = (Button) root.findViewById(R.id.submit);
        setOnClickSubmit(submit, R.id.record_1, root);

        // hide all selection UI until user choose an option
        hideUIOnCreate(root);

        // set Listener for all UI selection
        for (int i = 0; i < selectAButtonList.length; i++) {
            Button userChoice = root.findViewById(selectAButtonList[i]);
            Button preDefined = root.findViewById(selectBButtonList[i]);
            setUIVisibility(userChoice, preDefined, i);
        }

        // Disable the submit button if the port hasn't been establish
//        if (MainActivity.mSocket == null) {
//            submit.setEnabled(false);
//            submit.setText("Please complete all steps to submit");
//        } else {

        submit.setText(R.string.submit_to_server);
        //}
        return root;
    }

    private void setLocation(TextInputEditText locationEditText, Button confirmLocation) {
        locationEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                testingLocation = s.toString();
            }
        });

        confirmLocation.setOnClickListener(v -> {
            //JSONObject locationPackage = new JSONObject();
//            try {
//                locationPackage.put("location", testingLocation);
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//            if (MainActivity.mSocket == null) {
//                Toast.makeText(getContext(), "Please connect to a port from step 1", Toast.LENGTH_SHORT).show();
//                return;
//            }
            if (testingLocation.isEmpty()) {
                locationEditText.setError("Please enter your testing location");
            } else {
                //MainActivity.mSocket.emit("submit_location", locationPackage);
                // protosoundApp.callAttr("submit_location", testingLocation);
                model.submitLocation(testingLocation);
                this.location = testingLocation;
            }
        });
    }


    private void hideUIOnCreate(View root) {
        for (int rowID : rowSelectAList) {
            TableRow tableRow = root.findViewById(rowID);
            tableRow.setVisibility(View.GONE);
        }
        for (int rowID : rowSelectBList) {
            TableRow tableRow = root.findViewById(rowID);
            tableRow.setVisibility(View.GONE);
        }
        for (int rowID : rowRecordList) {
            TableRow tableRow = root.findViewById(rowID);
            tableRow.setVisibility(View.GONE);
        }
        for (int rowID : rowPlayList) {
            TableRow tableRow = root.findViewById(rowID);
            tableRow.setVisibility(View.GONE);
        }
    }

    private void setUIVisibility(Button userChoice, Button preDefined, int id) {
        userChoice.setOnClickListener(v -> {
            TableRow rowRecord = requireActivity().findViewById(rowRecordList[id]);
            TableRow rowPlay = requireActivity().findViewById(rowPlayList[id]);
            TableRow rowSelectA = requireActivity().findViewById(rowSelectAList[id]);
            TableRow rowSelection = requireActivity().findViewById(selection[id]);
            rowRecord.setVisibility(View.VISIBLE);
            rowPlay.setVisibility(View.VISIBLE);
            rowSelectA.setVisibility(View.VISIBLE);
            rowSelection.setVisibility(View.GONE);
        });
        preDefined.setOnClickListener(v -> {
            TableRow rowSelectB = requireActivity().findViewById(rowSelectBList[id]);
            TableRow rowSelection = requireActivity().findViewById(selection[id]);
            TableRow rowPlay = requireActivity().findViewById(rowPlayList[4]);
            rowPlay.setVisibility(View.INVISIBLE);
            rowSelectB.setVisibility(View.VISIBLE);
            rowSelection.setVisibility(View.GONE);
            for (int i = id * 5; i < id * 5 + 5; i++) {
                sampleRecorded[i] = true;
                predefinedSamples[i] = 1;
            }
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

    private void setSelectItemList(final AutoCompleteTextView spinner, int spinner_id) {
        spinner.setOnItemClickListener((parent, view, position, id) -> {
            String selection = (String)parent.getItemAtPosition(position);
            Log.d(TAG, "spinner " + spinner_id + ": " + selection);
            labelList[spinner_id-1] = selection;
        });
    }


    private void setOnClickRecord(final Button btn, final int id, int order){

        btn.setOnClickListener(v -> {
            Log.d(TAG, "record:"+ id + " called");
            btn.setBackgroundColor(Color.DKGRAY);
            btn.setText(R.string.recording);
            startRecording(id);
            for (int rid : recordButtonList) {
                if (rid != id) {
                    Button record = requireActivity().findViewById(rid);
                    record.setEnabled(false);
                }
            }
            sampleRecorded[order] = true;
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(() -> {
                //Do something after 1200ms
                btn.setBackgroundColor(Color.GREEN);
                btn.setText(R.string.done);
                for (int rid : recordButtonList) {
                    if (rid != id) {
                        Button record = requireActivity().findViewById(rid);
                        record.setEnabled(true);
                    }
                }
            }, 1200);
        });
    }

    private void setOnClickPlay(final Button btn, final int id){

        btn.setOnClickListener(v -> {
            Log.d(TAG, "play:" + id + " called");
            Toast.makeText(requireActivity(), "Playing.." , Toast.LENGTH_SHORT).show();
            startPlay(id);
        });
    }


    private void setOnClickSubmit(final Button btn, final int id, View root) {

        btn.setOnClickListener(v -> {
            Log.d(TAG, "Submit to Server");

            btn.setBackgroundColor(Color.GRAY);
            btn.setText(R.string.submitted_to_server);
            ProgressBar progressBar = root.findViewById(R.id.progressBar);
            progressBar.setVisibility(View.VISIBLE);
            Log.d(TAG, "SUBMIT PROGESSBAR SHOWN");

            try {

                ConnectivityManager cm =
                        (ConnectivityManager)getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                boolean isConnected = activeNetwork != null &&
                        activeNetwork.isConnectedOrConnecting();
//                if (!isConnected) {
//                    Toast.makeText(getActivity(), "Please check your internet connection and try again", Toast.LENGTH_SHORT).show();
//                    return;
//                }
                if (!checkAllFieldsExisted()) {
                    return;
                }

                //if (MainActivity.mSocket != null) {

                //btn.setText(R.string.submit_to_server);
//                } else {
//                    Toast.makeText(getActivity(), "Not connect to a socket. Please connect to a socket on step 1", Toast.LENGTH_SHORT).show();
//                    return;
//                }



                JSONObject soundPackage = new JSONObject();
                Map<Integer, List<Short>> map = new HashMap<>();
                for (int i = 0; i < recordButtonList.length; i++) {
                    if (sampleRecorded[i]) {
                        byte[] byteArray = getBytesFromPCM(VOICE_FILE_NAME + recordButtonList[i] + ".pcm");
                        short[] shortArray = convertByteArrayToShortArray(byteArray);
                        List<Short> soundBuffer = new ArrayList<>();
                        for (short num : shortArray) {
                            soundBuffer.add(num);
                        }
                        map.put(i, soundBuffer);
                        soundPackage.put("data_" + i, new JSONArray(soundBuffer));
                    }
                }
                List<String> labels = new ArrayList<>(Arrays.asList(labelList));
                soundPackage.put("label", new JSONArray(labels));
                //Log.d(TAG, "Socket connect:" + MainActivity.mSocket.connected());

                if (MainActivity.currentMode == TEST_END_TO_END_TRAINING_LATENCY_MODE) {
                    // If test end to end training time, need to start recording the time when the submit audio starts
                    soundPackage.put("submitAudioTime", "" + System.currentTimeMillis());
                } else {
                    soundPackage.put("submitAudioTime", 0);
                }
                soundPackage.put("predefinedSamples", new JSONArray(predefinedSamples));

                String submitAudioTime = model.submitData(soundPackage, map);

                this.submitAudioTime = submitAudioTime;
                EventBus.getDefault().post(this);
            } catch (FileNotFoundException | JSONException e) {
                e.printStackTrace();
            }
        });

    }



    private void startRecording(int id) {
        Log.d(TAG, "startRecording: ");
        recorder = new SoundRecorder(this.requireContext(), VOICE_FILE_NAME + id + ".pcm", mListener);
        recorder.startRecording(id);
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
//        for (int rid : recordButtonList) {
//            String filename = VOICE_FILE_NAME + rid + ".pcm";
//            if (!new File(requireActivity().getFilesDir(), filename).exists()) {
//                Log.d(TAG, "File not exist: " + filename);
//                Toast.makeText(requireActivity(), "Missing one or more record samples", Toast.LENGTH_SHORT).show();
//                return false;
//            }
//        }
        for (boolean recordedSample : sampleRecorded) {
            if (!recordedSample) {
                Log.d(TAG, "record not exist");
                Toast.makeText(requireActivity(), "Missing one or more recordings", Toast.LENGTH_SHORT).show();
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

//    private void sendRawAudioToServer(List<Short> soundBuffer, int id) {
//        try {
//            JSONObject jsonObject = new JSONObject();
//
//            jsonObject.put("data", new JSONArray(soundBuffer));
//            jsonObject.put("time", "" + System.currentTimeMillis());
//            Log.i(TAG, "Send raw audio to server:");
//            Log.i(TAG, "Connected: " + MainActivity.mSocket.connected());
////            emitter
//            MainActivity.mSocket.emit("android_test");
//            MainActivity.mSocket.emit("audio_data", jsonObject);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//    }

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
