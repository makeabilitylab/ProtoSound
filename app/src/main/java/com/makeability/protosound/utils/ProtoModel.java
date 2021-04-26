package com.makeability.protosound.utils;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.PyApplication;
import com.makeability.protosound.MainActivity;

import org.pytorch.IValue;
import org.pytorch.Module;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ProtoModel extends PyApplication {
    private static final String TAG = "ProtoModel";
    private Module mModuleEncoder;
    private PyObject protosoundApp;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "ProtoModel initialized.");
        String moduleFileAbsoluteFilePath = "";
        if (mModuleEncoder == null) {
            moduleFileAbsoluteFilePath = new File(
                    assetFilePath(this, "protosound_10_classes_scripted.pt")).getAbsolutePath();
            mModuleEncoder = Module.load(moduleFileAbsoluteFilePath);	// Have a ScriptModule now
        }

        Python py = Python.getInstance();
        protosoundApp = py.getModule("app");
        if (protosoundApp != null) {
            Log.d(TAG, "MODEL LOADED");
        }
    }

    public Module getModule() {
        return mModuleEncoder;
    }

    public PyObject getProtosoundApp() {
        return protosoundApp;
    }

    private String assetFilePath(Context context, String assetName) {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        } catch (IOException e) {
            Log.e(TAG, assetName + ": " + e.getLocalizedMessage());
        }
        return null;
    }
}
