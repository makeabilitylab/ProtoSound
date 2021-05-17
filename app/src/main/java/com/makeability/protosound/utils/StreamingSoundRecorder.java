package com.makeability.protosound.utils;

/*
 * Copyright (C) 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.makeability.protosound.MainActivity;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.makeability.protosound.MainActivity.TEST_END_TO_END_PREDICTION_LATENCY_MODE;
import static com.makeability.protosound.utils.HelperUtils.convertByteArrayToShortArray;
import static com.makeability.protosound.utils.HelperUtils.db;

/**
 * Stream sound from recorder to socketio server
 */
public class StreamingSoundRecorder {
	private static final String TAG = "StreamingSoundRecorder";
	private static final int RECORDING_RATE = 44100; // can go up to 44K, if needed
	private static final int CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO;
	private static final int CHANNELS_OUT = AudioFormat.CHANNEL_OUT_MONO;
	private static final int FORMAT = AudioFormat.ENCODING_PCM_16BIT;
	private static int BUFFER_SIZE = AudioRecord.getMinBufferSize(RECORDING_RATE, CHANNEL_IN, FORMAT);
	private static final double DB_THRESHOLD = 50;

	private List<Short> soundBuffer = new ArrayList<>();
	private final String mOutputFileName;
	private List<String> labels = new ArrayList<String>();

	private static final int RECORDER_SAMPLERATE = 16000;
	private static final float PREDICTION_THRES = 0.5F;
	private static final String CHANNEL_ID = "SOUNDWATCH";
	private final Context mContext;
	private State mState = State.IDLE;

	private AsyncTask<Void, Void, Void> mRecordingAsyncTask;

	private Set<String> connectedHostIds;
	private int ONE_SECOND_SOUND_COUNTER = 0;
	private ProtoApp model;

	enum State {
		IDLE, RECORDING, PLAYING
	}

	/**
	 *
	 * @param context
	 * @param outputFileName
	 */
	public StreamingSoundRecorder(Context context, String outputFileName) {
		mOutputFileName = outputFileName;
		mContext = context;
		model = (ProtoApp) context.getApplicationContext();
	}


	/**
	 * Starts recording from the MIC.
	 */
	public void startRecording() {
		if (mState != State.IDLE) {
			return;
		}
		mRecordingAsyncTask = new RecordAudioAsyncTask(this, model);
		mRecordingAsyncTask.execute();
	}


	public void stopRecording() {
		if (mRecordingAsyncTask != null) {
			mRecordingAsyncTask.cancel(true);
		}
	}

	public static class RecordAudioAsyncTask extends AsyncTask<Void, Void, Void> {
		private WeakReference<StreamingSoundRecorder> mSoundRecorderWeakReference;
		private AudioRecord mAudioRecord;
		private ProtoApp model;
		public String label;
		public String confidence;
		public String db;

		RecordAudioAsyncTask(StreamingSoundRecorder context, ProtoApp model) {
			mSoundRecorderWeakReference = new WeakReference<>(context);
			this.model = model;
			}

		@Override
		protected void onPreExecute() {
			StreamingSoundRecorder soundRecorder = mSoundRecorderWeakReference.get();

			if (soundRecorder != null) {
				soundRecorder.mState = State.RECORDING;
			}
		}

		@Override
		protected Void doInBackground(Void... params) {
			final StreamingSoundRecorder soundRecorder = mSoundRecorderWeakReference.get();
			mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
					RECORDING_RATE, CHANNEL_IN, FORMAT, BUFFER_SIZE * 3);
			BufferedOutputStream bufferedOutputStream = null;
			try {
				bufferedOutputStream = new BufferedOutputStream(
						soundRecorder.mContext.openFileOutput(
								soundRecorder.mOutputFileName,
								Context.MODE_PRIVATE));
				byte[] buffer = new byte[BUFFER_SIZE];
				mAudioRecord.startRecording();
				while (!isCancelled()) {
					int read = mAudioRecord.read(buffer, 0, buffer.length);
//					Log.d(TAG, "doInBackground: " + read);
					short[] shorts = convertByteArrayToShortArray(buffer);
					// buffer sounds up to 1 sec
					if (soundRecorder.soundBuffer.size() <= 44100) {
						for (short num : shorts) {
							if (soundRecorder.soundBuffer.size() == 44100) {
								final List<Short> tempBuffer = soundRecorder.soundBuffer;
								double dbLevel = db(tempBuffer);
								if (db(tempBuffer) > DB_THRESHOLD) {
									Log.i(TAG, "doInBackground: sound db < 40 " + dbLevel);
									processAudioRecognition(tempBuffer, dbLevel);
								}
								soundRecorder.soundBuffer = new ArrayList<>();
							}
							soundRecorder.soundBuffer.add(num);
						}
					}
					bufferedOutputStream.write(buffer, 0, read);
				}
			} catch (IOException | NullPointerException | IndexOutOfBoundsException e) {
				Log.e(TAG, "Failed to record data: " + e);
			} finally {
				if (bufferedOutputStream != null) {
					try {
						bufferedOutputStream.close();
					} catch (IOException e) {
						// ignore
					}
				}
				mAudioRecord.release();
				mAudioRecord = null;
			}
			return null;
		}

		private void processAudioRecognition(List<Short> soundBuffer, double db) {
			long recordTime = System.currentTimeMillis();
			sendRawAudioToServer(soundBuffer, recordTime, db);
		}

		/**
		 *
		 * @param soundBuffer
		 * @param recordTime
		 */
		private void sendRawAudioToServer(List<Short> soundBuffer, long recordTime, double db) {
			try {
				final StreamingSoundRecorder soundRecorder = mSoundRecorderWeakReference.get();
				ConnectivityManager cm =
						(ConnectivityManager)soundRecorder.mContext.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

				NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
				boolean isConnected = activeNetwork != null &&
						activeNetwork.isConnectedOrConnecting();
				if (!isConnected) {
					new Handler(Looper.getMainLooper()).post(() -> {
						Toast.makeText(soundRecorder.mContext.getApplicationContext(), "Please check your internet connection and try again", Toast.LENGTH_LONG).show();

					});
					return;
				}
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("data", new JSONArray(soundBuffer));
				if (MainActivity.currentMode == TEST_END_TO_END_PREDICTION_LATENCY_MODE) {
					jsonObject.put("recordTime", "" + System.currentTimeMillis());
				}
				jsonObject.put("db", db);
				Log.i(TAG, "Sending audio data: " + soundBuffer.size());


				List<String> result = model.handleSource(soundBuffer, db);

				if (result != null) {
					this.label = result.get(0);
					this.confidence = result.get(1);
					this.db = result.get(2);
					EventBus.getDefault().post(this);
				}

			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		static void flatten(Object object, List<Float> list) {
			if (object.getClass().isArray())
				for (int i = 0; i < Array.getLength(object); ++i)
					flatten(Array.get(object, i), list);
			else
				list.add((float)object);
		}

		static float[] flatten(Object object) {
			List<Float> list = new ArrayList<>();
			flatten(object, list);
			int size = list.size();
			float[] result = new float[size];
			for (int i = 0; i < size; ++i)
				result[i] = list.get(i);
			return result;
		}
		@Override
		protected void onPostExecute(Void aVoid) {
			StreamingSoundRecorder soundRecorder = mSoundRecorderWeakReference.get();

			if (soundRecorder != null) {
				soundRecorder.mState = State.IDLE;
				soundRecorder.mRecordingAsyncTask = null;
			}
		}

		@Override
		protected void onCancelled() {
			StreamingSoundRecorder soundRecorder = mSoundRecorderWeakReference.get();

			if (soundRecorder != null) {
				if (soundRecorder.mState == State.RECORDING) {
					Log.d(TAG, "Stopping the recording ...");
					soundRecorder.mState = State.IDLE;
				} else {
					Log.w(TAG, "Requesting to stop recording while state was not RECORDING");
				}
				soundRecorder.mRecordingAsyncTask = null;
			}
		}
	}

}
