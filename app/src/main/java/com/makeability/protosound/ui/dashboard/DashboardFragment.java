package com.makeability.protosound.ui.dashboard;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.makeability.protosound.MainActivity;
import com.makeability.protosound.R;
import com.makeability.protosound.ui.SharedViewModel;
import com.makeability.protosound.utils.ProtoApp;
import com.makeability.protosound.utils.SoundRecorder;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.pytorch.Module;

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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.makeability.protosound.MainActivity.TEST_END_TO_END_TRAINING_LATENCY_MODE;

public class DashboardFragment extends Fragment {
    private String TAG = "Dashboard";
    private ProtoApp model;
    private Module module;
    public String location;
    public String locationChoice = "none";
    public String submitAudioTime;
    private boolean trainingComplete = false;
    private int BACKGROUND_COLOR;
    private int THEME_COLOR;

    private static final boolean TEST = true;
    private SharedViewModel sharedViewModel;
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
    private final List<String> givenPredefinedLabels = new ArrayList<String>(Arrays.asList(
            "Appliances", "Baby Cry", "Cat Meow", "Dog Bark", "Doorbell",
            "Fire Alarm", "Knocking", "Siren", "Water Running"));
    SoundRecorder recorder;

    private ArrayAdapter<CharSequence> locationAdapter;
    private List<String> savedLocations;

    private ArrayAdapter<CharSequence> adapter;
    private List<CharSequence> availPredefinedSamples;
    private Map<Integer, CharSequence> spinnerSelection;

    // Saved state
    private boolean locationSubmitted;
    private int[] scrollPos;
    private int[] horizontalScrollPos;
    private Map<Integer, Integer> userChoiceMap;    // 0 is YOUR CHOICE chosen, 1 is PRE-DEFINED, 2 is "Choose again
                                                    // so displays YOUR CHOICE and PRE-DEFINED
    private Map<Integer, String> selectANameMap;    // Name for YOUR CHOICE sounds


    int[] recordButtonList = {R.id.record_1, R.id.record_2, R.id.record_3, R.id.record_4, R.id.record_5,
            R.id.record_6, R.id.record_7, R.id.record_8, R.id.record_9, R.id.record_10,
            R.id.record_11, R.id.record_12, R.id.record_13, R.id.record_14, R.id.record_15,
            R.id.record_16, R.id.record_17, R.id.record_18, R.id.record_19, R.id.record_20,
            R.id.record_21, R.id.record_22, R.id.record_23, R.id.record_24, R.id.record_25, R.id.record_bg};

    int[] resetButtonList = {R.id.reset_1, R.id.reset_2, R.id.reset_3, R.id.reset_4, R.id.reset_5,
            R.id.reset_6, R.id.reset_7, R.id.reset_8, R.id.reset_9, R.id.reset_10,
            R.id.reset_11, R.id.reset_12, R.id.reset_13, R.id.reset_14, R.id.reset_15,
            R.id.reset_16, R.id.reset_17, R.id.reset_18, R.id.reset_19, R.id.reset_20,
            R.id.reset_21, R.id.reset_22, R.id.reset_23, R.id.reset_24, R.id.reset_25, R.id.reset_bg};

    int[] rowSelectAList = {R.id.row_1_select_a, R.id.row_2_select_a, R.id.row_3_select_a, R.id.row_4_select_a, R.id.row_5_select_a};
    int[] rowSelectBList = {R.id.row_1_select_b, R.id.row_2_select_b, R.id.row_3_select_b, R.id.row_4_select_b, R.id.row_5_select_b};   // predefined

    int[] rowRecordList = {R.id.row_1_record, R.id.row_2_record, R.id.row_3_record, R.id.row_4_record, R.id.row_5_record};
    int[] rowResetList = {R.id.row_1_reset, R.id.row_2_reset, R.id.row_3_reset, R.id.row_4_reset, R.id.row_5_reset};

    int[] selectAButtonList = {R.id.select_1a, R.id.select_2a, R.id.select_3a, R.id.select_4a, R.id.select_5a};
    int[] selectBButtonList = {R.id.select_1b, R.id.select_2b, R.id.select_3b, R.id.select_4b, R.id.select_5b};
    int[] selectAgainAButtonList = {R.id.select_again_1a, R.id.select_again_2a, R.id.select_again_3a, R.id.select_again_4a, R.id.select_again_5a};
    int[] selectAgainBButtonList = {R.id.select_again_1b, R.id.select_again_2b, R.id.select_again_3b, R.id.select_again_4b, R.id.select_again_5b};
    int[] selection = {R.id.selection_1, R.id.selection_2, R.id.selection_3, R.id.selection_4, R.id.selection_5};
    int[] menuList = {R.id.menu_1, R.id.menu_2, R.id.menu_3, R.id.menu_4, R.id.menu_5};
    int[] classNameList = {R.id.class_1, R.id.class_2, R.id.class_3, R.id.class_4, R.id.class_5};
    int[] finishSoundList = {R.id.check_mark_1, R.id.check_mark_2, R.id.check_mark_3, R.id.check_mark_4, R.id.check_mark_5};
    boolean[] sampleRecorded = new boolean[26];
    int[] predefinedSamples = new int[26];

    // Override onAttach to make sure Fragment is attached to an Activity first
    // to avoid getapplicationcontext returning null
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.model = (ProtoApp) context.getApplicationContext();
        this.module = model.getModule();
//        Intent serviceIntent = new Intent(context, ForegroundService.class);
//        context.stopService(serviceIntent);
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);

        BACKGROUND_COLOR = getResources().getColor(R.color.material_on_primary_emphasis_high_type, requireActivity().getTheme());
        THEME_COLOR = getResources().getColor(R.color.purple_200, requireActivity().getTheme());
        location = null;
        locationSubmitted = false;
        spinnerSelection = new HashMap<>();
        scrollPos = new int[2];
        horizontalScrollPos = new int[2];
        userChoiceMap = new HashMap<>();
        selectANameMap = new HashMap<>();
        TextView tutorial_5 = root.findViewById(R.id.tutorial_5);
        tutorial_5.setVisibility(View.GONE);
        Map<Integer, Integer> recordResetMap = new HashMap<>();
        for (int i = 0; i < resetButtonList.length; i++) {
            if (!recordResetMap.containsKey(resetButtonList[i])) {
                recordResetMap.put(resetButtonList[i], recordButtonList[i]);
            }
        }

        checkRecordPermission();

        // setup port number edit text
        // setup Listener for 15 record and playback buttons

//        TextInputEditText portNumberEditText = (TextInputEditText) root.findViewById(R.id.port_number);
//        Button confirmPort = (Button) root.findViewById(R.id.confirm_port);
        TextInputEditText locationEditText = (TextInputEditText) root.findViewById(R.id.input_location);

        Button confirmLocation = root.findViewById(R.id.confirm_location);
        setLocation(locationEditText, confirmLocation);

        // Set "Back" functionality for location selection
        setOnClickAgainLocationNew(root.findViewById(R.id.select_again_location_1));
        setOnClickAgainLocationExisting(root.findViewById(R.id.select_again_location_2));


        for (int i = 0; i < recordButtonList.length; i++) {
            Button record = root.findViewById(recordButtonList[i]);
            setOnClickRecord(record, recordButtonList[i], i);
        }


        for (int i = 0; i < resetButtonList.length; i++) {
            Button reset = root.findViewById(resetButtonList[i]);
            setOnClickResetRecording(reset, recordResetMap.get(resetButtonList[i]), i);
        }

        for (int i = 0; i < selectAgainAButtonList.length; i++) {
            Button again = root.findViewById(selectAgainAButtonList[i]);
            setOnClickAgainSoundNew(again, i);
        }

        for (int i = 0; i < selectAgainBButtonList.length; i++) {
            Button again = root.findViewById(selectAgainBButtonList[i]);
            setOnClickAgainSoundExisting(again, i);
        }

        savedLocations = model.getSavedLocations();
        Log.d(TAG,"LOCATIONS: "  + Arrays.toString(savedLocations.toArray()));
        locationAdapter = new ArrayAdapter(requireActivity(), android.R.layout.simple_spinner_dropdown_item,savedLocations);
        AutoCompleteTextView userLocationSelection = root.findViewById(R.id.location_menu);
        userLocationSelection.setAdapter(locationAdapter);
        setOnClickUserLocationSelection(userLocationSelection);


        // Create a dummyAdapter to get use getAutofillOptions() to return array of labels
        ArrayAdapter dummyAdapter = ArrayAdapter.createFromResource(requireActivity(),
                R.array.sound_array, android.R.layout.simple_spinner_dropdown_item);

        availPredefinedSamples = new ArrayList<>(Arrays.asList(dummyAdapter.getAutofillOptions()));
        // Create an ArrayAdapter using the string array and a default spinner layout.
        // Register it with availPredefinedSamples to update changes
        adapter = new ArrayAdapter(requireActivity(),android.R.layout.simple_spinner_dropdown_item,  availPredefinedSamples);

        Log.d(TAG, "PREDEFINED, GET ITEM AT id: " + 0 +", which is: " + adapter.getItem(0));

        // Setup 3 edit texts for 3 classes
        for (int i = 0; i < classNameList.length; i++) {
            TextInputEditText classTextField = (TextInputEditText) root.findViewById(classNameList[i]);
            setOnClickText(classTextField, i);
        }

        // Setup submit button
        Button submit = (Button) root.findViewById(R.id.submit);
        setOnClickSubmit(submit, R.id.record_1, root);
        submit.setText(R.string.submit_to_server);

        // hide all selection UI until user choose an option
        hideUIOnCreate(root);

        // set Listener for Location UI selection
        Button locationNewBtn = root.findViewById(R.id.location_new_btn);
        Button locationExistingBtn = root.findViewById(R.id.location_existing_btn);
        setLocationUIVisibility(locationNewBtn, locationExistingBtn);

        // set Listener for all UI selection
        for (int i = 0; i < selectAButtonList.length; i++) {
            Button userChoice = root.findViewById(selectAButtonList[i]);
            Button preDefined = root.findViewById(selectBButtonList[i]);
            setSoundUIVisibility(userChoice, preDefined, i);
        }

        Button resetAllBtn = (Button) root.findViewById(R.id.reset_all);
        setOnClickResetAll(resetAllBtn);

        return root;
    }


    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Restore ScrollView position
        sharedViewModel.getMScrollPos().observe(requireActivity(), new Observer<int[]>() {
            @Override
            public void onChanged(@Nullable int[] mScrollPos) {
                scrollPos = mScrollPos;
            }
        });

        // Restore HorizontalScrollView position
        sharedViewModel.getMHorizontalScrollPos().observe(requireActivity(), new Observer<int[]>() {
            @Override
            public void onChanged(@Nullable int[] mHorizontalPos) {
                horizontalScrollPos = mHorizontalPos;
            }
        });
        ScrollView scrollView = requireActivity().findViewById(R.id.scrollView2);
        HorizontalScrollView horizontalScrollView = requireActivity().findViewById(R.id.horizontalScrollView);
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.scrollTo(scrollPos[0], scrollPos[1]);
                horizontalScrollView.scrollTo(horizontalScrollPos[0], horizontalScrollPos[1]);
            }
        });

        // Restore saved locations
        sharedViewModel.getMSavedLocations().observe(requireActivity(), new Observer<List<String>>() {
            @Override
            public void onChanged(@Nullable List<String> mSavedLocationsChoice) {
                savedLocations = mSavedLocationsChoice;
            }
        });

        // Restore location adapter
        locationAdapter = new ArrayAdapter(requireActivity(),android.R.layout.simple_spinner_dropdown_item,  savedLocations);

        // Restore location choice
        sharedViewModel.getMLocationChoice().observe(requireActivity(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String mLocationChoice) {
                locationChoice = mLocationChoice;
            }
        });

        // Restore location entered
        TextInputEditText locationEditText = (TextInputEditText) view.findViewById(R.id.input_location);
        sharedViewModel.getText().observe(requireActivity(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String mLocation) {
                location = mLocation;
            }
        });
        locationEditText.setText(location);


        // Restore location button
        sharedViewModel.getMLocationSubmitted().observe(requireActivity(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean mLocationSubmitted) {
                locationSubmitted = mLocationSubmitted;
            }
        });

        if (locationChoice.equals("new")) {
            displayLocationNewUI();

            if (locationSubmitted) {
                Button confirmLocation = requireActivity().findViewById(R.id.confirm_location);
                confirmLocation.setBackgroundColor(THEME_COLOR);
                confirmLocation.setText(R.string.submitted);
                confirmLocation.setTextColor(Color.BLACK);
            }
        } else if (locationChoice.equals("existing")) {
            displayLocationExistingUI();
            AutoCompleteTextView locationSpinner = view.findViewById(R.id.location_menu);
            locationSpinner.setText(location);
            locationSpinner.setAdapter(locationAdapter);
        }

        if (locationSubmitted) {
            HorizontalScrollView step2 = view.findViewById((R.id.horizontalScrollView));
            step2.setVisibility(View.VISIBLE);
            LinearLayout step3 = requireActivity().findViewById((R.id.step3));
            step3.setVisibility(View.VISIBLE);
        }

        // Restore labelList of sound names
        sharedViewModel.getMLabelList().observe(requireActivity(), new Observer<String[]>() {
            @Override
            public void onChanged(String[] mLabelList) {
                labelList = mLabelList;
            }
        });

        // Restore spinnerSelection, helper variable for pre-defined samples
        sharedViewModel.getMSpinnerSelection().observe(requireActivity(), new Observer<Map<Integer, CharSequence>>() {
            @Override
            public void onChanged(Map<Integer, CharSequence> mSpinnerSelection) {
                spinnerSelection = mSpinnerSelection;
            }
        });

        // Restore availPredefinedSamples, helper variable for pre-defined samples
        sharedViewModel.getMAvailPredefinedSamples().observe(requireActivity(), new Observer<List<CharSequence>>() {
            @Override
            public void onChanged(List<CharSequence> mAvailPredefinedSamples) {
                availPredefinedSamples = mAvailPredefinedSamples;
            }
        });
        // Restore array adapter
        adapter = new ArrayAdapter(requireActivity(),android.R.layout.simple_spinner_dropdown_item,  availPredefinedSamples);

        // Restore user choice for each sound
        sharedViewModel.getMUserChoiceMap().observe(requireActivity(), new Observer<Map<Integer, Integer>>() {
            @Override
            public void onChanged(@Nullable Map<Integer, Integer> map) {
                userChoiceMap = map;
            }
        });
        for (Integer id: userChoiceMap.keySet()) {
            Integer choice = userChoiceMap.get(id);
            if (choice!= null && choice == 0) {
                // Restore YOUR CHOICE's UI
                LinearLayout rowRecord = requireActivity().findViewById(rowRecordList[id]);
                LinearLayout rowReset = requireActivity().findViewById(rowResetList[id]);
                LinearLayout rowSelectA = requireActivity().findViewById(rowSelectAList[id]);
                LinearLayout rowSelection = requireActivity().findViewById(selection[id]);
                Button selectAgainA = requireActivity().findViewById(selectAgainAButtonList[id]);
                rowRecord.setVisibility(View.VISIBLE);
                rowReset.setVisibility(View.VISIBLE);
                rowSelectA.setVisibility(View.VISIBLE);
                rowSelection.setVisibility(View.GONE);
                selectAgainA.setVisibility(View.VISIBLE);

                // Restore YOUR CHOICE sound name
                String classID = "class_" + (id+1);
                TextInputEditText className = requireActivity().findViewById(getResources().getIdentifier(classID, "id", getContext().getPackageName()));
                className.setText(labelList[id]);
            } else if (choice!= null && choice == 1) {
                // Restore PRE-DEFINED's UI
                LinearLayout rowSelectB = requireActivity().findViewById(rowSelectBList[id]);
                LinearLayout rowSelection = requireActivity().findViewById(selection[id]);
                Button selectAgainB = requireActivity().findViewById(selectAgainBButtonList[id]);
                rowSelectB.setVisibility(View.VISIBLE);
                rowSelection.setVisibility(View.GONE);
                selectAgainB.setVisibility(View.VISIBLE);
                int predefinedType = givenPredefinedLabels.contains(labelList[id]) ? 1 : 2;
                for (int i = id * 5; i < id * 5 + 5; i++) {
                    sampleRecorded[i] = true;
                    predefinedSamples[i] = predefinedType;
                }

                AutoCompleteTextView spinner = (AutoCompleteTextView) requireActivity().findViewById(menuList[id]);
                spinner.setText(spinnerSelection.get(id));
                spinner.setAdapter(adapter);
            } else {
                LinearLayout rowSelection = requireActivity().findViewById(selection[id]);
                rowSelection.setVisibility(View.VISIBLE);
            }
        }

        // setup Spinner drop-down lists for pre-determined sounds
        for (int i = 0; i < menuList.length; i++) {
            AutoCompleteTextView spinner = (AutoCompleteTextView) requireActivity().findViewById(menuList[i]);
            // Specify the layout to use when the list of choices appears
            // Apply the adapter to the spinner
            spinner.setAdapter(adapter);
            // Set Listener for spinner
            setSelectItemList(spinner, i);
        }

        // Restore "Record" states
        sharedViewModel.getMSampleRecorded().observe(requireActivity(), new Observer<boolean[]>() {
            @Override
            public void onChanged(boolean[] mSampleRecorded) {
                sampleRecorded = mSampleRecorded;
            }
        });

        // Restore "Record" states
        for (int i = 0; i < recordButtonList.length; i++) {
            int row = i / 5;
            Integer choice = userChoiceMap.get(row);
            if (sampleRecorded[i] && (choice != null &&  choice == 0 || i == 25) )
            { // Only restore "Record" for YOUR CHOICE, or when
                Button recordBtn = requireActivity().findViewById(recordButtonList[i]);
                recordBtn.setBackgroundColor(THEME_COLOR);
                recordBtn.setText(R.string.play);
                recordBtn.setTextColor(Color.BLACK);
                setOnClickPlay(recordBtn, recordButtonList[i]);
                recorder = new SoundRecorder(this.requireContext(), VOICE_FILE_NAME + i + ".pcm", mListener);

            }
            checkUserChoiceComplete(i / 5);    // Check completion for "Sound i"
        }


        // Restore "TRAIN" button state
        boolean trainingComplete = true;
        for (int i = 0; i < labelList.length; i++) {
            if (labelList[i].equals("") || !sampleRecorded[i * 5]) {
                trainingComplete = false;
                break;
            }
        }
        trainingComplete &= sampleRecorded[25]; // background noise recorded?
        if (trainingComplete) {
            Button submit = requireActivity().findViewById(R.id.submit);
            submit.setBackgroundColor(THEME_COLOR);
            submit.setText(R.string.training_complete);
            submit.setTextColor(Color.BLACK);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        sharedViewModel.setMLocationSubmitted(locationSubmitted);   // save locationSubmitted

        ScrollView scrollView = requireActivity().findViewById(R.id.scrollView2);
        int[] pos = new int[]{scrollView.getScrollX(), scrollView.getScrollY()};
        sharedViewModel.setMScrollPos(pos); // save ScrollView pos

        HorizontalScrollView horizontalScrollView = requireActivity().findViewById(R.id.horizontalScrollView);
        int[] horizontalPos = new int[]{horizontalScrollView.getScrollX(), horizontalScrollView.getScrollY()};
        sharedViewModel.setMHorizontalScrollPos(horizontalPos); // save HorizontalScrollView pos
    }

    private void hideUIOnCreate(View root) {
        // Location
        LinearLayout locationNewInput = root.findViewById(R.id.location_selection_new);
        locationNewInput.setVisibility(View.GONE);
        LinearLayout locationExistingInput = root.findViewById(R.id.location_selection_existing);
        locationExistingInput.setVisibility(View.GONE);

        HorizontalScrollView step2 = root.findViewById((R.id.horizontalScrollView));
        step2.setVisibility(View.GONE);

        LinearLayout step3 = root.findViewById((R.id.step3));
        step3.setVisibility(View.GONE);

        // Sound selection
        for (int rowID : rowSelectAList) {
            LinearLayout ll = root.findViewById(rowID);
            ll.setVisibility(View.GONE);
        }
        for (int rowID : rowSelectBList) {
            LinearLayout ll = root.findViewById(rowID);
            ll.setVisibility(View.GONE);
        }
        for (int rowID : rowRecordList) {
            LinearLayout ll = root.findViewById(rowID);
            ll.setVisibility(View.GONE);
        }
        for (int rowID : rowResetList) {
            LinearLayout ll = root.findViewById(rowID);
            ll.setVisibility(View.GONE);
        }

        for (int btnID : selectAgainAButtonList) {
            Button btn = root.findViewById(btnID);
            btn.setVisibility(View.GONE);
        }

        for (int btnID : selectAgainBButtonList) {
            Button btn = root.findViewById(btnID);
            btn.setVisibility(View.GONE);
        }

        for (int finishID: finishSoundList) {
            ImageView finish = root.findViewById(finishID);
            finish.setVisibility(View.GONE);
        }
    }

    private void setLocationUIVisibility(Button locationNewBtn, Button locationExistingBtn) {
        locationNewBtn.setOnClickListener(v->{
            displayLocationNewUI();

        });

        locationExistingBtn.setOnClickListener(v-> {
            displayLocationExistingUI();

        });
    }

    private void displayLocationNewUI() {
        LinearLayout locationSelection = requireActivity().findViewById(R.id.selection_location);
        LinearLayout locationNewInput = requireActivity().findViewById(R.id.location_selection_new);
        locationSelection.setVisibility(View.GONE);
        locationNewInput.setVisibility(View.VISIBLE);

        locationChoice = "new";
        sharedViewModel.setMLocationChoice(locationChoice);
    }

    private void displayLocationExistingUI() {
        LinearLayout locationSelection = requireActivity().findViewById(R.id.selection_location);
        LinearLayout locationExistingInput = requireActivity().findViewById(R.id.location_selection_existing);
        locationSelection.setVisibility(View.GONE);
        locationExistingInput.setVisibility(View.VISIBLE);

        locationChoice = "existing";
        sharedViewModel.setMLocationChoice(locationChoice);
    }

    private void setSoundUIVisibility(Button userChoice, Button preDefined, int id) {
        userChoice.setOnClickListener(v -> {
            LinearLayout rowRecord = requireActivity().findViewById(rowRecordList[id]);
            LinearLayout rowPlay = requireActivity().findViewById(rowResetList[id]);
            LinearLayout rowSelectA = requireActivity().findViewById(rowSelectAList[id]);
            LinearLayout rowSelection = requireActivity().findViewById(selection[id]);
            Button selectAgainA = requireActivity().findViewById(selectAgainAButtonList[id]);
            rowRecord.setVisibility(View.VISIBLE);
            rowPlay.setVisibility(View.VISIBLE);
            rowSelectA.setVisibility(View.VISIBLE);
            rowSelection.setVisibility(View.GONE);
            selectAgainA.setVisibility(View.VISIBLE);
            userChoiceMap.put(id, 0);
            sharedViewModel.setMUserChoiceMap(userChoiceMap);
        });
        preDefined.setOnClickListener(v -> {
            LinearLayout rowSelectB = requireActivity().findViewById(rowSelectBList[id]);
            LinearLayout rowSelection = requireActivity().findViewById(selection[id]);
            Button selectAgainB = requireActivity().findViewById(selectAgainBButtonList[id]);
            rowSelectB.setVisibility(View.VISIBLE);
            rowSelection.setVisibility(View.GONE);
            selectAgainB.setVisibility(View.VISIBLE);

            userChoiceMap.put(id, 1);
            sharedViewModel.setMUserChoiceMap(userChoiceMap);
            sharedViewModel.setMSampleRecorded(sampleRecorded);


        });
    }

    // listener for "Back" for NEW Location
    private void setOnClickAgainLocationNew(Button btn) {
        btn.setOnClickListener(v->{
            hideLocationNewSelection();
        });
    }

    // listener for "Back" for Existing Location
    private void setOnClickAgainLocationExisting(Button btn) {
        btn.setOnClickListener(v->{
            hideLocationExistingSelection();


        });
    }

    private void hideLocationNewSelection(){
        resetTrainBtn();
        resetSoundBackgroundUI();
        LinearLayout locationSelection = requireActivity().findViewById(R.id.selection_location);
        LinearLayout locationNewInput = requireActivity().findViewById(R.id.location_selection_new);
        HorizontalScrollView step2 = requireActivity().findViewById((R.id.horizontalScrollView));
        LinearLayout step3 = requireActivity().findViewById((R.id.step3));
        locationSelection.setVisibility(View.VISIBLE);
        locationNewInput.setVisibility(View.GONE);
        step2.setVisibility(View.GONE);
        step3.setVisibility(View.GONE);

        locationSubmitted = false;
        location = null;
        TextInputEditText locationEditText = (TextInputEditText) requireActivity().findViewById(R.id.input_location);
        locationEditText.setText(null);

        // Save data
        sharedViewModel.setMLocationSubmitted(false);
        sharedViewModel.setText(location);
        sharedViewModel.setMLocationChoice("none");

        for (int i = 0; i < menuList.length; i++) {
            Button againBtn = requireActivity().findViewById(selectAgainAButtonList[i]);
            resetSoundNewUI(againBtn, i);
        }

        for (int i = 0; i < menuList.length; i++) {
            Button againBtn = requireActivity().findViewById(selectAgainBButtonList[i]);
            resetSoundExistingUI(againBtn, i);
        }
    }

    private void hideLocationExistingSelection(){
        resetTrainBtn();
        resetSoundBackgroundUI();
        LinearLayout locationSelection = requireActivity().findViewById(R.id.selection_location);
        LinearLayout locationExisting = requireActivity().findViewById(R.id.location_selection_existing);
        HorizontalScrollView step2 = requireActivity().findViewById((R.id.horizontalScrollView));
        LinearLayout step3 = requireActivity().findViewById((R.id.step3));
        locationSelection.setVisibility(View.VISIBLE);
        locationExisting.setVisibility(View.GONE);
        step2.setVisibility(View.GONE);
        step3.setVisibility(View.GONE);

        savedLocations.add(location);
        AutoCompleteTextView locationSpinner = requireActivity().findViewById(R.id.location_menu);
        locationSpinner.setText("");
        locationSubmitted = false;
        location = null;

        // Save data
        sharedViewModel.setMLocationSubmitted(false);
        sharedViewModel.setText(location);
        sharedViewModel.setMLocationChoice("none");
        sharedViewModel.setMSavedLocations(savedLocations);

        for (int i = 0; i < menuList.length; i++) {
            Button againBtn = requireActivity().findViewById(selectAgainAButtonList[i]);
            resetSoundNewUI(againBtn, i);
        }

        for (int i = 0; i < menuList.length; i++) {
            Button againBtn = requireActivity().findViewById(selectAgainBButtonList[i]);
            resetSoundExistingUI(againBtn, i);
        }
    }


    // listener for "Back" for NEW Sound
    private void setOnClickAgainSoundNew(Button againA, int i) {
        againA.setOnClickListener(v-> {
            resetTrainBtn();
            resetSoundNewUI(againA, i);
        });
    }

    // listener for "Back" for EXISTING Sound
    private void setOnClickAgainSoundExisting(Button againB, int i) {
        againB.setOnClickListener(v-> {
            resetTrainBtn();
            resetSoundExistingUI(againB, i);
        });
    }
    private void resetSoundBackgroundUI() {
        Button recordBtn = requireActivity().findViewById(recordButtonList[25]);
        recordBtn.setTextColor(THEME_COLOR);
        recordBtn.setBackgroundColor(BACKGROUND_COLOR);
        recordBtn.setText(R.string.record);
        setOnClickRecord(recordBtn, recordButtonList[25], 25);

        sampleRecorded[25] = false;
        sharedViewModel.setMSampleRecorded(sampleRecorded);
    }
    private void resetSoundNewUI(Button againA, int i) {
        // Reset state
        userChoiceMap.put(i, 2);
        labelList[i] = "";
        // Clear sound name
        TextInputEditText classTextField = (TextInputEditText) requireActivity().findViewById(classNameList[i]);
        classTextField.setText("");
        for (int j = i * 5; j < i * 5 + 5; j++) {
            sampleRecorded[j] = false;
            Button recordBtn = requireActivity().findViewById(recordButtonList[j]);
            recordBtn.setBackgroundColor(Color.TRANSPARENT);
            String record_id = "record_" + (j % 5 + 1);
            recordBtn.setTextColor(THEME_COLOR);
            recordBtn.setText(getResources().getIdentifier(record_id, "string", getContext().getPackageName()));
            setOnClickRecord(recordBtn, recordButtonList[j], j);
        }
        checkUserChoiceComplete(i);
        sharedViewModel.setMLabelList(labelList);   // save labelList
        sharedViewModel.setMSampleRecorded(sampleRecorded); // save sampleRecorded

        LinearLayout rowSelectA = requireActivity().findViewById(rowSelectAList[i]);
        LinearLayout rowSelection = requireActivity().findViewById(selection[i]);
        LinearLayout rowReset = requireActivity().findViewById(rowResetList[i]);
        LinearLayout rowRecord = requireActivity().findViewById(rowRecordList[i]);
        rowSelectA.setVisibility(View.GONE);
        rowSelection.setVisibility(View.VISIBLE);
        rowReset.setVisibility(View.GONE);
        rowRecord.setVisibility(View.GONE);
        againA.setVisibility(View.GONE);
    }

    private void resetSoundExistingUI(Button againB, int i) {
        userChoiceMap.put(i, 2);
        labelList[i] = "";
        for (int j = i * 5; j < i * 5 + 5; j++) {
            sampleRecorded[j] = false;
            predefinedSamples[j] = 0;
        }
        checkUserChoiceComplete(i);

        if (spinnerSelection.containsKey(i)) {
            CharSequence item = spinnerSelection.get(i);

                availPredefinedSamples.add(item);

            spinnerSelection.remove(i);


        }
        // reset adapter to this new availPredefinedSamples
        adapter = new ArrayAdapter<>(requireActivity(),android.R.layout.simple_spinner_dropdown_item,  availPredefinedSamples);
        adapter.sort(new Comparator<CharSequence>() {
            @Override
            public int compare(CharSequence s1, CharSequence s2) {
                if (Character.isLowerCase(s1.charAt(0)) &&
                        (!Character.isLowerCase(s2.charAt(0)))) {
                    return -1;
                } else if (!Character.isLowerCase(s1.charAt(0)) &&
                        (Character.isLowerCase(s2.charAt(0)))) {
                    return 1;
                } else {
                    return s1.toString().compareTo(s2.toString());
                }
            }
        });

        AutoCompleteTextView spinner = requireActivity().findViewById(menuList[i]);
        spinner.setText("");

        // need to reset spinner's listener
        for (int j = 0; j < menuList.length; j++) {
            AutoCompleteTextView spinner2 = (AutoCompleteTextView) requireActivity().findViewById(menuList[j]);
            spinner2.setAdapter(adapter);
            setSelectItemList(spinner2, j);
        }
        sharedViewModel.setMAvailPredefinedSamples(availPredefinedSamples);
        sharedViewModel.setMSpinnerSelection(spinnerSelection);

        sharedViewModel.setMLabelList(labelList);   // save labelList
        sharedViewModel.setMSampleRecorded(sampleRecorded); // save sampleRecorded

        LinearLayout rowSelectB = requireActivity().findViewById(rowSelectBList[i]);
        LinearLayout rowSelection = requireActivity().findViewById(selection[i]);
        LinearLayout rowSelectA = requireActivity().findViewById(rowSelectAList[i]);
        LinearLayout rowPlay = requireActivity().findViewById(rowResetList[i]);
        rowSelectB.setVisibility(View.GONE);
        rowSelectA.setVisibility(View.GONE);
        rowPlay.setVisibility(View.GONE);
        rowSelection.setVisibility(View.VISIBLE);
        againB.setVisibility(View.GONE);
    }

    private void setOnClickResetAll(Button resetAllBtn) {
        resetAllBtn.setOnClickListener(v->{
            if (locationChoice.equals("new")) {
                hideLocationNewSelection();
            } else {
                hideLocationExistingSelection();
            }
        });

    }

    private void setViewFocusable(View v) {
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                v.setFocusableInTouchMode(true);
                v.requestFocus();
                if (v instanceof TextInputEditText) {
                    InputMethodManager inputManager = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputManager.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        });
    }

    private void setLocation(TextInputEditText locationEditText, Button confirmLocation) {
        setViewFocusable(locationEditText);
        locationEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                confirmLocation.setBackgroundColor(BACKGROUND_COLOR);
                confirmLocation.setTextColor(THEME_COLOR);
                confirmLocation.setText(R.string.submitLocation);
                locationSubmitted = false;
                sharedViewModel.setText(s.toString()); // save location

            }

            @Override
            public void afterTextChanged(Editable s) {
                testingLocation = s.toString();
                resetTrainBtn();
            }
        });

        confirmLocation.setOnClickListener(v -> {
            if (testingLocation.isEmpty()) {
                locationEditText.setError("Please enter your testing location");
            } else {
                if (model.saveLocation(testingLocation)) {
                    model.submitLocation(testingLocation);
                    // Fetch new list of saved locations
                    locationAdapter = new ArrayAdapter(requireActivity(), android.R.layout.simple_spinner_dropdown_item, savedLocations);
                    AutoCompleteTextView userLocationSelection = requireActivity().findViewById(R.id.location_menu);
                    userLocationSelection.setAdapter(locationAdapter);

                    this.location = testingLocation;
                    locationSubmitted = true;

                    confirmLocation.setBackgroundColor(THEME_COLOR);
                    confirmLocation.setTextColor(Color.BLACK);
                    confirmLocation.setText(R.string.submitted);

                    HorizontalScrollView step2 = requireActivity().findViewById((R.id.horizontalScrollView));
                    step2.setVisibility(View.VISIBLE);

                    LinearLayout step3 = requireActivity().findViewById((R.id.step3));
                    step3.setVisibility(View.VISIBLE);

                    for (int i = 0; i < menuList.length; i++) {
                        AutoCompleteTextView spinner = requireActivity().findViewById(menuList[i]);
                        spinner.setText(null);
                    }

                    // Save data
                    sharedViewModel.setMLocationSubmitted(true);
                }


            }
        });
    }

    private void setOnClickText(TextInputEditText field, int id) {
        setViewFocusable(field);
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
                switch (id) {
                    case 0:
                        errMsg = "1st";
                        break;
                    case 1:
                        errMsg = "2nd";
                        break;
                    case 2:
                        errMsg = "3rd";
                        break;
                    case 3:
                        errMsg = "4th";
                        break;
                    case 4:
                        errMsg = "5th";
                        break;
                }

                String labelName = s.toString().trim();
                if (labelName.isEmpty()) {
                    field.setError("Please enter a name for your " + errMsg + " class.");
                }
                Log.d(TAG, "EditText: " + labelName);
                labelList[id] = labelName;
                sharedViewModel.setMLabelList(labelList);
                checkUserChoiceComplete(id);
                resetTrainBtn();
            }
        });
    }

    private void setOnClickUserLocationSelection(AutoCompleteTextView spinner) {
        setViewFocusable(spinner);
        spinner.setOnItemClickListener((parent, view, position, id) -> {
            if (location != null && !location.equals("")) {
                savedLocations.add(location);
            }
            location = (String)parent.getItemAtPosition(position);
            Log.d(TAG,"LOCATION: " + location);
            savedLocations.remove(location);
            Collections.sort(savedLocations);
            locationAdapter = new ArrayAdapter(requireActivity(),android.R.layout.simple_spinner_dropdown_item,  savedLocations);
            spinner.setAdapter(locationAdapter);

            locationSubmitted = true;
            model.submitLocation(location);

            adapter.clear();
            adapter.addAll(model.getUserSoundsForLoc());
            adapter.addAll(givenPredefinedLabels);

            HorizontalScrollView step2 = requireActivity().findViewById((R.id.horizontalScrollView));
            step2.setVisibility(View.VISIBLE);
            LinearLayout step3 = requireActivity().findViewById((R.id.step3));
            step3.setVisibility(View.VISIBLE);

            for (int i = 0; i < menuList.length; i++) {
                Button againBtn = requireActivity().findViewById(selectAgainAButtonList[i]);
                resetSoundNewUI(againBtn, i);
            }

            for (int i = 0; i < menuList.length; i++) {
                Button againBtn = requireActivity().findViewById(selectAgainBButtonList[i]);
                spinnerSelection.remove(i, spinnerSelection.get(i)); // Remove selected sound from this location

                resetSoundExistingUI(againBtn, i);
            }

            // Save data
            sharedViewModel.setMLocationSubmitted(true);
            sharedViewModel.setText(location);
            sharedViewModel.setMSavedLocations(savedLocations);
        });
    }

    private void setSelectItemList(final AutoCompleteTextView spinner, int spinner_id) {
        setViewFocusable(spinner);
        spinner.setOnItemClickListener((parent, view, position, id) -> {
            trainingComplete = false;
            resetTrainBtn();
            spinner.clearFocus();
            ImageView finish = requireActivity().findViewById(finishSoundList[spinner_id]);
            finish.setVisibility(View.VISIBLE);
            finish.setColorFilter(Color.GREEN);

            String selection = (String)parent.getItemAtPosition(position);

            // If this spinner already selects an item,
            // add this item back to adapter
            if (spinnerSelection.containsKey(spinner_id)) {
                availPredefinedSamples.add(spinnerSelection.get(spinner_id));
                sharedViewModel.setMAvailPredefinedSamples(availPredefinedSamples); // Save availPredefinedSamples

                adapter.notifyDataSetChanged(); // sync availPredefinedSamples and adapter

                spinnerSelection.remove(spinner_id);
                sharedViewModel.setMSpinnerSelection(spinnerSelection); // Save spinnerSelection
            }
            // Add this new item to spinnerSelection
            spinnerSelection.put(spinner_id, selection);
            sharedViewModel.setMSpinnerSelection(spinnerSelection);  // Save spinnerSelection

            labelList[spinner_id] = selection;
            sharedViewModel.setMLabelList(labelList);   // Save labelList
            //Log.d(TAG, "PREDEF BEFORE REMOVE " + Arrays.toString(availPredefinedSamples.toArray()));
            availPredefinedSamples.remove(selection);
            //Log.d(TAG, "PREDEF AFTER REMOVE " + Arrays.toString(availPredefinedSamples.toArray()));
            sharedViewModel.setMAvailPredefinedSamples(availPredefinedSamples); // Save availPredefinedSamples
            adapter.notifyDataSetChanged();
            // sort just to keep original alphabetical order
            adapter.sort(new Comparator<CharSequence>() {
                @Override
                public int compare(CharSequence s1, CharSequence s2) {
                    if (Character.isLowerCase(s1.charAt(0)) &&
                            (!Character.isLowerCase(s2.charAt(0)))) {
                        return -1;
                    } else if (!Character.isLowerCase(s1.charAt(0)) &&
                            (Character.isLowerCase(s2.charAt(0)))) {
                        return 1;
                    } else {
                        return s1.toString().compareTo(s2.toString());
                    }
                }
            });
            spinner.setAdapter(adapter);

            int predefinedType = givenPredefinedLabels.contains(selection) ? 1 : 2;
            for (int i = spinner_id * 5; i < spinner_id * 5 + 5; i++) {
                sampleRecorded[i] = true;
                predefinedSamples[i] = predefinedType;
            }
        });
    }


    private void setOnClickRecord(final Button btn, final int id, int order){

        btn.setOnClickListener(v -> {
            Log.d(TAG, "record:"+ id + " called");
            resetTrainBtn();
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
            sharedViewModel.setMSampleRecorded(sampleRecorded); // save sampleRecorded

            final Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(() -> {
                //Do something after 1200ms
                btn.setBackgroundColor(THEME_COLOR);
                btn.setText("Play");
                btn.setTextColor(Color.BLACK);
                for (int rid : recordButtonList) {
                    if (rid != id) {
                        Button record = requireActivity().findViewById(rid);
                        record.setEnabled(true);
                    }
                }
            }, 1200);

            setOnClickPlay(btn, id);

            int row = order / 5;
            checkUserChoiceComplete(row);
        });
    }


    // Check if user has recorded 5 sounds and given a label, then
    // set the title (e.g. Sound 1) to be green
    private void checkUserChoiceComplete(int row) {
        if (row == 5) return;   // row is only from 0->4. Row 5 is background noise
        ImageView finish = requireActivity().findViewById(finishSoundList[row]);
        for (int i = row * 5; i < row * 5 + 5; i++) {
            if (!sampleRecorded[i] || Objects.equals(labelList[row], "")) {
                String selection_title_id = "selection_title_" + (row+1);
                TextView selection_title = requireActivity().findViewById(getResources().getIdentifier(selection_title_id, "id", getContext().getPackageName()));
                if (selection_title != null && finish != null)  {

                    finish.setVisibility(View.GONE);
                }
                return;
            }
        }
        String selection_title_id = "selection_title_" + (row+1);
        TextView selection_title = requireActivity().findViewById(getResources().getIdentifier(selection_title_id, "id", getContext().getPackageName()));
//        selection_title.setTextColor(Color.GREEN);

        finish.setVisibility(View.VISIBLE);
        finish.setColorFilter(Color.GREEN);
    }

    private void setOnClickPlay(final Button btn, final int id){

        btn.setOnClickListener(v -> {
            Log.d(TAG, "play:" + id + " called");
            Toast.makeText(requireActivity(), "Playing.." , Toast.LENGTH_SHORT).show();
            startPlay(id);
        });
    }

    // "Reset" button makes the user record sound again
    private void setOnClickResetRecording(final Button btn, final int id, final int order){

        btn.setOnClickListener(v -> {
            resetTrainBtn();
            Button recordBtn = requireActivity().findViewById(id);
            String record_id = "record_" + (order % 5 + 1);
            recordBtn.setText(getResources().getIdentifier(record_id, "string", getContext().getPackageName()));
            recordBtn.setTextColor(THEME_COLOR);
            recordBtn.setBackgroundColor(BACKGROUND_COLOR);
            setOnClickRecord(recordBtn, id, order);
            sampleRecorded[order] = false;
            // Save data
            sharedViewModel.setMSampleRecorded(sampleRecorded);
        });
    }

    // reset "TRAIN MODEL" button UI to original after user changes something
    private void resetTrainBtn() {
        Button btn = requireActivity().findViewById(R.id.submit);
        btn.setBackgroundColor(BACKGROUND_COLOR);
        btn.setText(R.string.submit_to_server);
        btn.setTextColor(THEME_COLOR);
    }
    private void setOnClickSubmit(final Button btn, final int id, View root) {

        btn.setOnClickListener(v -> {
            if (!checkAllFieldsExisted() ) {
                return;
            }
            trainingComplete = true;
            Log.d(TAG, "Submit to Server");

            btn.setBackgroundColor(Color.GRAY);
            btn.setText(R.string.training_data);
            ProgressBar progressBar = root.findViewById(R.id.progressBar);
            progressBar.setVisibility(View.VISIBLE);
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(() -> {
                try {
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

                model.submitData(soundPackage, map);

                this.submitAudioTime = submitAudioTime;
                EventBus.getDefault().post(this);
            } catch (FileNotFoundException | JSONException e) {
                e.printStackTrace();
            }},100);
        });
    }



    private void startRecording(int id) {
        Log.d(TAG, "startRecording: ");
        recorder = new SoundRecorder(this.requireContext(), VOICE_FILE_NAME + id + ".pcm", mListener);
        recorder.startRecording(id);
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
        if (!locationSubmitted) {
            Toast.makeText(requireActivity(), "Missing location from Step 1.", Toast.LENGTH_SHORT).show();
            return false;
        }

        for (int i = 0; i < labelList.length; i++) {
            if (labelList[i].isEmpty()) {
                Log.d(TAG, "Label not exist: " + i);
                Toast.makeText(requireActivity(), "Missing sound label from Step 2 for sound " + (i + 1), Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        for (int i = 0; i < sampleRecorded.length; i++) {
            if (!sampleRecorded[i]) {
                if (i != 25) {
                    Toast.makeText(requireActivity(), "Missing recording from Step 2 for sound " + (i / 5 + 1), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireActivity(), "Missing recording for Step 3.", Toast.LENGTH_SHORT).show();
                }

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
