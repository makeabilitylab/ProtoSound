package com.makeability.protosound.ui.dashboard;

import android.Manifest;
import android.content.pm.PackageManager;
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
import com.makeability.protosound.utils.SoundRecorder;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class DashboardFragment extends Fragment {

    private DashboardViewModel dashboardViewModel;
    private static final int RECORDER_SAMPLE_RATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    public static final String VOICE_FILE_NAME = "audiorecord.pcm";
    SoundRecorder.OnVoicePlaybackStateChangedListener mListener;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    int BytesPerElement = 2; // 2 bytes in 16bit format
    String TAG = "Dashboard";
    SoundRecorder recorder;

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
        record_t1_s1.setOnClickListener(v -> {
            Log.i("Dashboard", "t1_s1 called");
            startRecording();
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    stopRecording();
                    timer.cancel();
                }
            }, 2000);
        });

        Button test_button = root.findViewById(R.id.test_button);
        test_button.setOnClickListener(v -> {
            startPlay();
        });
        return root;
    }

    private void startRecording() {
        Log.d(TAG, "startRecording: ");
        recorder = new SoundRecorder(this.requireContext(), VOICE_FILE_NAME, mListener);
        recorder.startRecording();
    }

    private void stopRecording() {
        // stops the recording activity
        Log.d(TAG, "stopRecording: ");
        if (null != recorder) {
            recorder.stopRecording();
        }
    }

    private void startPlay() {
        Log.d(TAG, "startPlay: ");
        if (null != recorder) {
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
            } catch (IOException e) { /* ignore */}
        }
        return bytes;
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