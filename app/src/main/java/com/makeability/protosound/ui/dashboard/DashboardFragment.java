package com.makeability.protosound.ui.dashboard;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.makeability.protosound.R;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class DashboardFragment extends Fragment {

    private DashboardViewModel dashboardViewModel;
    private static final int RECORDER_SAMPLE_RATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    int BytesPerElement = 2; // 2 bytes in 16bit format
    String TAG = "Dashboard";

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        dashboardViewModel =
                new ViewModelProvider(this).get(DashboardViewModel.class);
        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);
        final TextView textView = root.findViewById(R.id.text_dashboard);
        dashboardViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });

        checkRecordPermission();

        Button record_t1_s1 = root.findViewById(R.id.record_t1_s1);
        record_t1_s1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("Dashboard", "t1_s1 called");
                startRecording();
                new CountDownTimer(1000, 250) {
                    public void onTick(long millisUntilFinished) {
                        Log.d(TAG,"seconds remaining: " + millisUntilFinished / 250);
                    }

                    public void onFinish() {
                        Log.d(TAG,"DONE");
                    }
                }.start();
                stopRecording();
            }
        });
        return root;
    }

    private void startRecording() {
        int minBufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLE_RATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLE_RATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, minBufferSize);

        recorder.startRecording();
        isRecording = true;
        recordingThread = new Thread(this::writeAudioDataToFile, "AudioRecorder Thread");
        recordingThread.start();
    }

    private void writeAudioDataToFile() {
        // Write the output audio in byte
        String filePath = "/sdcard/8k16bitMono.pcm";

        short sData[] = new short[BufferElements2Rec];

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        while (isRecording) {
            // gets the voice output from microphone to byte format
            recorder.read(sData, 0, BufferElements2Rec);
            System.out.println("Short wirting to file" + sData.toString());
            try {
                // writes the data to file from buffer stores the voice buffer
                byte bData[] = short2byte(sData);

                os.write(bData, 0, BufferElements2Rec * BytesPerElement);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopRecording() {
        // stops the recording activity
        if (null != recorder) {
            isRecording = false;


            recorder.stop();
            recorder.release();

            recorder = null;
            recordingThread = null;
        }
    }

    private void checkRecordPermission() {

        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.RECORD_AUDIO},
                    PERMISSIONS_REQUEST_CODE);
        }
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
                getActivity().finish();
            }
        }
    }

}