package com.makeability.protosound.ui;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.makeability.protosound.MainActivity;

public class SharedViewModel extends ViewModel {

    private static final String TAG = "DashboardViewModel";
    private MutableLiveData<String> mText = new MutableLiveData<>();
    private MutableLiveData<MainActivity.TimelineAdapter> mAdapter = new MutableLiveData<>();

//    public DashboardViewModel() {
//        mText = new MutableLiveData<>();
//        mText.setValue("WTF");
//    }

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

}