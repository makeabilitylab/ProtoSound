package com.makeability.protosound.ui;

import android.util.Log;
import android.widget.ArrayAdapter;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.makeability.protosound.MainActivity;

import java.util.List;
import java.util.Map;

public class SharedViewModel extends ViewModel {

    private static final String TAG = "DashboardViewModel";
    private MutableLiveData<String> mText = new MutableLiveData<>();
    private MutableLiveData<MainActivity.TimelineAdapter> mAdapter = new MutableLiveData<>();   // predictions timeline
    private MutableLiveData<Map<Integer, Boolean>> mUserChoiceMap = new MutableLiveData<>();  // user choice for sounds
    private MutableLiveData<int[]> mScrollPos = new MutableLiveData<>();    // ScrollView position
    private MutableLiveData<boolean[]> mSampleRecorded = new MutableLiveData<>();   // 26 recorded states

    private MutableLiveData<String[]> mLabelList = new MutableLiveData<>(); // sound labels

    // pre-defined states
    private MutableLiveData<Map<Integer, CharSequence>> mSpinnerSelection = new MutableLiveData<>();
    private MutableLiveData<List<CharSequence>> mAvailPredefinedSamples = new MutableLiveData<>();
    public void setText(String text) {
        mText.setValue(text);
        Log.d(TAG, "VIEWMODEL TEXT VALUE: " + mText.getValue());
    }

    public LiveData<String> getText() {
        return mText;
    }

    public void setAdapter(MainActivity.TimelineAdapter adapter) {
        mAdapter.setValue(adapter);
    }
    public LiveData<MainActivity.TimelineAdapter> getAdapter() {
        return mAdapter;
    }


    public LiveData<Map<Integer, Boolean>> getMUserChoiceMap() {
        return mUserChoiceMap;
    }

    public void setMUserChoiceMap(Map<Integer, Boolean> map) {
        mUserChoiceMap.setValue(map);
    }

    public LiveData<int[]> getMScrollPos() {
        return mScrollPos;
    }

    public void setMScrollPos(int[] scrollPos) {
        mScrollPos.setValue(scrollPos);
    }

    public LiveData<boolean[]> getMSampleRecorded() {
        return mSampleRecorded;
    }

    public void setMSampleRecorded(boolean[] mSampleRecorded) {
        this.mSampleRecorded.setValue(mSampleRecorded);
    }

    public LiveData<String[]> getMLabelList() {
        return mLabelList;
    }

    public void setMLabelList(String[] mLabelList) {
        this.mLabelList.setValue(mLabelList);
    }


    public LiveData<Map<Integer, CharSequence>> getMSpinnerSelection() {
        return mSpinnerSelection;
    }

    public void setMSpinnerSelection(Map<Integer, CharSequence> mSpinnerSelection) {
        this.mSpinnerSelection.setValue(mSpinnerSelection);
    }

    public LiveData<List<CharSequence>> getMAvailPredefinedSamples() {
        return mAvailPredefinedSamples;
    }

    public void setMAvailPredefinedSamples(List<CharSequence> mAvailPredefinedSamples) {
        this.mAvailPredefinedSamples.setValue(mAvailPredefinedSamples);
    }
}