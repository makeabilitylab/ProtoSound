package com.makeability.protosound;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.kuassivi.component.RipplePulseRelativeLayout;
import com.makeability.protosound.ui.home.models.AudioLabel;
import com.makeability.protosound.ui.home.service.ForegroundService;

import androidx.annotation.LongDef;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.github.nkzawa.socketio.client.Socket.EVENT_CONNECT;
import static com.makeability.protosound.utils.Constants.PREDICTION_CHANNEL_ID;
import static com.makeability.protosound.utils.Constants.TEST_NUMBER_EXTRA;

public class MainActivity extends AppCompatActivity {

	public static Socket mSocket;
	private static final String DEBUG_TAG = "NetworkStatusExample";
	public static final boolean TEST_MODEL_LATENCY = false;
	public static final boolean TEST_E2E_LATENCY = false;
	private static final String TEST_E2E_LATENCY_SERVER = "http://128.208.49.41:8789";
	private static final String MODEL_LATENCY_SERVER = "http://128.208.49.41:8790";
	private static final String DEFAULT_SERVER = "http://128.208.49.41:8788";
	private static final String TEST_SERVER = "http://128.208.49.41:5000";
	private static final String TAG = "MainActivity";
	public static boolean notificationChannelIsCreated = false;
	private String db = "";
	public static int evalCount = 0;
	private static final int DELAY_IN_SECOND = 4;
	private static final int TOTAL_EVAL_PER_TESTER = 100;
	private Map<String, Long> soundLastTime = new HashMap<>();
	private List<AudioLabel> timeLine = new ArrayList<>();
	private List<Integer> ratedLabels = new ArrayList<>();

	private static final int NORMAL_MODE = 0;
	public static final int TEST_END_TO_END_PREDICTION_LATENCY_MODE = 1;
	public static final int TEST_END_TO_END_TRAINING_LATENCY_MODE = 2;
	private static final int MAX_TIMELINE_SIZE = 10;

	public ListView listView;
	public static int currentMode = NORMAL_MODE;


	/**
	 * Adapter to draw the timeline view for user to submit audio feedback
	 */
	public class TimelineAdapter extends BaseAdapter implements ListAdapter {
		private List<AudioLabel> list = new ArrayList<>();
		private Context context;


		public TimelineAdapter(List<AudioLabel> list, Context context) {
			this.list = list;
			this.context = context;
		}

		@Override
		public int getCount() {
			return list.size();
		}

		@Override
		public Object getItem(int pos) {
			return list.get(pos);
		}

		@Override
		public long getItemId(int pos) {
			//just return 0 because we don't have id for each position
			return 0;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			View view = convertView;
			if (view == null) {
				LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = inflater.inflate(R.layout.custom_dialog_layout, null);
			}

			//Handle TextView and display string from your list
			TextView listItemText = (TextView)view.findViewById(R.id.soundLabel);
			Log.d(TAG, "getView: evalCount" + evalCount);
			if (evalCount == TOTAL_EVAL_PER_TESTER) {
				setAlert(evalCount);
				evalCount = 0;
			}
			listItemText.setText(list.get(position).getTimeAndLabel());

			//Handle buttons and add onClickListeners
			ImageButton trueButton = (ImageButton) view.findViewById(R.id.trueButton);
			ImageButton falseButton = (ImageButton) view.findViewById(R.id.falseButton);
			// weird bugs with position, this is a temporary fix
			if (position < this.getCount() - 1) {
				if (ratedLabels.get(position) == 1) {
					Log.i(TAG, "remove position " + position);
					trueButton.setEnabled(false);
					falseButton.setEnabled(false);
					falseButton.setVisibility(View.INVISIBLE);
				} else if (ratedLabels.get(position) == -1) {
					Log.i(TAG, "remove position " + position);
					falseButton.setEnabled(false);
					trueButton.setEnabled(false);
					trueButton.setVisibility(View.INVISIBLE);
				}
			}
			// unless this is called specifically, the last position always show incorrect UI
			if (position == this.getCount() - 1) {
				listItemText.setVisibility(View.GONE);
				trueButton.setVisibility(View.GONE);
				falseButton.setVisibility(View.GONE);
			}
			trueButton.setOnClickListener(v -> {
				Log.i(TAG, "Submit true button feedback " + position);
				reportUserPredictionFeedback(list.get(position).label, true, list.get(position).getTime());
				trueButton.setEnabled(false);
				falseButton.setEnabled(false);
				falseButton.setVisibility(View.INVISIBLE);
				ratedLabels.set(position, 1);
				evalCount++;
			});
			falseButton.setOnClickListener(v -> {
				Log.i(TAG, "Submit false button feedback" + position);
				reportUserPredictionFeedback(list.get(position).label, false, list.get(position).getTime());
				trueButton.setVisibility(View.INVISIBLE);
				trueButton.setEnabled(false);
				falseButton.setEnabled(false);
				ratedLabels.set(position, -1);
				evalCount++;
			});

			return view;
		}
	}

	public void setAlert(int evalCount) {
		// inflate the layout of the popup window
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		//Set title for AlertDialog
		builder.setTitle("Evaluations completed");

		//Set body message of Dialog
		builder.setMessage("You have completed " + evalCount + " evaluations. Do you wish to continue testing?");

		//// Is dismiss when touching outside?
		builder.setCancelable(true);

		//Positive Button and it onClicked event listener
		builder.setPositiveButton("Yes",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						Toast.makeText(MainActivity.this, "Thank you for your time!", Toast.LENGTH_SHORT).show();
					}
				});

		//Negative Button
		builder.setNegativeButton("No",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						stopRecording(MainActivity.this);
						ImageButton listeningBtn = (ImageButton) findViewById(R.id.mic);
						RipplePulseRelativeLayout pulseLayout = findViewById(R.id.pulseLayout);
						TextView soundTextView = findViewById(R.id.description);
						switchToStopRecord(listeningBtn, pulseLayout, soundTextView);
						Toast.makeText(MainActivity.this, "Thank you for your input!", Toast.LENGTH_SHORT).show();
					}
				});

		AlertDialog dialog = builder.create();
		dialog.show();
	}

	// Finish current activity when back button is pressed
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		this.finish();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate");

		// Get the test extra from the Entrance activity to determine which test we want to run
		Intent intent = this.getIntent();
		currentMode = intent.getIntExtra(TEST_NUMBER_EXTRA, NORMAL_MODE);

		List<Short> test = new ArrayList<>();
		test.add((short) 12);
		test.add((short) 23);
		test.add((short) 34);
		test.add((short) 45);
		test.add((short) 56);
		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put("data", new JSONArray(test));
			jsonObject.put("time", "" + System.currentTimeMillis());
		} catch (JSONException e) {
			e.printStackTrace();
		}

		setContentView(R.layout.activity_main);
		BottomNavigationView navView = findViewById(R.id.nav_view);
		listView = findViewById(R.id.listView);
		// Passing each menu ID as a set of Ids because each
		// menu should be considered as top level destinations.
		AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
				R.id.navigation_home, R.id.navigation_dashboard)
				.build();
		NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
		NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
		NavigationUI.setupWithNavController(navView, navController);

		checkNetworkConnection();
	}


	@Override
	protected void onDestroy() {
		Log.i(TAG, "onDestroy: ");
		super.onDestroy();
		if (mSocket != null) {
			mSocket.disconnect();
		}
	}

	private Emitter.Listener onAudioLabelUIViewMessage = new Emitter.Listener() {
		@Override
		public void call(Object... args) {
			Log.i(TAG, "Received audio label event");
			JSONObject data = (JSONObject) args[0];
			String db = "1.0"; // TODO: Hard code this number for now so we don't have to redesign notification
			String audio_label;
			String accuracy = "1.0";
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
			LocalTime localTime = LocalTime.now();
			String time = formatter.format(localTime);
			String recordTime = "";
			try {
				audio_label = data.getString("label");
				accuracy = data.getString("confidence");
				db = data.getString("db");
				if (currentMode == TEST_END_TO_END_PREDICTION_LATENCY_MODE) {
					// If test end2end prediction, there should be a record time to determine the original record time of this sound
					recordTime =  data.getString("recordTime");
					writeEndToEndLatencyToExternalFile(recordTime, "e2e_prediction");
				}
			} catch (JSONException e) {
				return;
			}
			Log.i(TAG, "received sound label from Socket server: " + audio_label + ", " + accuracy);
			AudioLabel audioLabel = new AudioLabel(audio_label, accuracy, time, db,
					null);;
			if (soundLastTime.containsKey(audioLabel.label)) {
				if (System.currentTimeMillis() <= (soundLastTime.get(audioLabel.label) + DELAY_IN_SECOND * 1000)) { //multiply by 1000 to get milliseconds
					Log.i(TAG, "Same sound appear in less than 5 seconds");
					return; // stop sending noti if less than 10 second
				}
			}
			timeLine.add(audioLabel);
			ratedLabels.add(0);
			if (timeLine.size() > MAX_TIMELINE_SIZE) {
				timeLine.remove(0);
			}
			soundLastTime.put(audioLabel.label, System.currentTimeMillis());
			new Handler(Looper.getMainLooper()).post(() -> {
				TimelineAdapter adapter = new TimelineAdapter(timeLine, getApplicationContext());
				listView.setAdapter(adapter);
				// Disable scrolling
				listView.setEnabled(false);
//				listView.setSelection(adapter.getCount() - 1);
//				adapter.notifyDataSetChanged();
			});
		}
	};

	/**
	 * Use this function to report checkbox feedback to user
	 * @param predictedLabel
	 * @param actualUserLabel
	 */
	private void reportUserPredictionFeedback(String predictedLabel, String actualUserLabel) {
		try {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("predictedLabel", predictedLabel);
			jsonObject.put("actualUserLabel", actualUserLabel);
			jsonObject.put("isTruePrediction", !predictedLabel.equalsIgnoreCase(actualUserLabel));
			MainActivity.mSocket.emit("audio_prediction_feedback", jsonObject);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}


	/**
	 * Use this function to report checkbox feedback to user
	 * @param predictedLabel
	 * @param isTruePrediction
	 */
	private void reportUserPredictionFeedback(String predictedLabel, boolean isTruePrediction, String time) {
		try {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("predictedLabel", predictedLabel);
//			jsonObject.put("actualUserLabel", null);
			jsonObject.put("isTruePrediction", isTruePrediction);
			jsonObject.put("time", time);
			MainActivity.mSocket.emit("audio_prediction_feedback", jsonObject);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	/**
	 *
	 * @param recordTime original record time when recorded from audiorecorder
	 * @param fileName the file name we wants to write to (o.e: e2e_pred_latency.txt, e2e_training_latency.txt)
	 */
	private void writeEndToEndLatencyToExternalFile(String recordTime, String fileName) {
		long elapsedTime = System.currentTimeMillis() - Long.parseLong(recordTime);
		Log.i(TAG, "Elapsed time: " + elapsedTime);
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm:ss");
		Date date = new Date(System.currentTimeMillis());
		String timeStamp = simpleDateFormat.format(date);

		try {
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(openFileOutput(fileName +  ".txt", Context.MODE_APPEND));
			outputStreamWriter.write(timeStamp + "," +  Long.toString(elapsedTime) + "\n");
			outputStreamWriter.close();
		}
		catch (IOException e) {
			Log.e("Exception", "File write failed: " + e.toString());
		}
	}

//	private Emitter.Listener onAudioLabelNotificationMessage = new Emitter.Listener() {
//		@Override
//		public void call(final Object... args) {
//			Log.i(TAG, "Received audio label event");
//			JSONObject data = (JSONObject) args[0];
//			String db = "1.0"; // TODO: Hard code this number for now so we don't have to redesign notification
//			String audio_label;
//			String accuracy = "1.0";
//			String record_time = "";
//			try {
//				audio_label = data.getString("label");
//				accuracy = data.getString("confidence");
//				db = data.getString("db");
//			} catch (JSONException e) {
//				return;
//			}
//			Log.i(TAG, "received sound label from Socket server: " + audio_label + ", " + accuracy);
//			AudioLabel audioLabel;
//			audioLabel = new AudioLabel(audio_label, accuracy, null, db,
//					null);
//			createAudioLabelNotification(audioLabel);
//		}
//	};

	public void initSocket(String port) {
		mSocket.connect();
		mSocket.on("android_test_2", onTestMessage);

		// The server will return an audio_label to the phone with label
		mSocket.on("audio_data_s2c", onAudioLabelUIViewMessage);
		mSocket.on("training_complete", onTrainingCompleteMessage);

		mSocket.on("receive_location", onReceiveLocation);

		mSocket.once(EVENT_CONNECT, (Emitter.Listener) args -> {
			new Handler(Looper.getMainLooper()).post(() -> {
				// Update UI here
				Log.i(TAG, "call: " + args);
				Log.d(TAG, "call: " + mSocket.connected());
				Button confirmPort = (Button) findViewById(R.id.confirm_port);
				new Handler(Looper.getMainLooper()).post(() -> {
					confirmPort.setBackgroundColor(Color.GREEN);
					confirmPort.setText("CONFIRMED");
				});
				confirmPort.setBackgroundColor(Color.GREEN);
				TextView tick = (TextView) findViewById(R.id.tick);
				tick.setText( R.string.port_connected);
				Button submit = (Button) findViewById(R.id.submit);
				submit.setEnabled(true);
				submit.setBackgroundColor(Color.parseColor("#FF4972"));
				submit.setText(R.string.submit_to_server);
				ProgressBar progressBar = findViewById(R.id.progressBar);
				progressBar.setVisibility(View.GONE);
			});

//				JSONObject data = (JSONObject) args[0];
//				String userPrototypeAvailable;
//				try {
//					userPrototypeAvailable = data.getString("label");
//					if (userPrototypeAvailable.equals("True")) {
//
//					}
//
//				} catch (JSONException e) {
//					Log.i(TAG, "JSON Exception failed: " + data.toString());
//					return;
//				}

		});
		Log.d(TAG, "connected: " + mSocket.connected());
	}


	private Emitter.Listener onTestMessage = args -> {
		System.out.println(args[0]);
		Log.i(TAG, "Received socket event");
	};

	private Emitter.Listener onReceiveLocation = args -> {
		Log.i(TAG, "Received location event");
		Button confirmLocation = (Button) findViewById(R.id.confirm_location);

		new Handler(Looper.getMainLooper()).post(() -> {
			confirmLocation.setBackgroundColor(Color.GREEN);
			confirmLocation.setText("Sent");
		});
	};

	private Emitter.Listener onTrainingCompleteMessage = args -> {
		Log.i(TAG, "Received training complete!");
		Log.i(TAG, "Received audio label event");
		if (currentMode == TEST_END_TO_END_TRAINING_LATENCY_MODE) {
			JSONObject data = (JSONObject) args[0];
			String submitAudioTime = ""; // original submit audio time when starts training the audio
			try {
				submitAudioTime = data.getString("submitAudioTime");
				// If test end2end training, there should be a record submit time from SoundRecorder
				submitAudioTime = data.getString("submitAudioTime");
				writeEndToEndLatencyToExternalFile(submitAudioTime, "e2e_training");
			} catch (JSONException e) {
				return;
			}
		}
		Button submitButton = (Button) findViewById(R.id.submit);
		ProgressBar progressBar = findViewById(R.id.progressBar);
		submitButton.setBackgroundColor(Color.GREEN);
		submitButton.setText(R.string.training_complete);
		new Handler(Looper.getMainLooper()).post(() -> {
			progressBar.setVisibility(View.GONE);
		});

	};

	private void checkNetworkConnection() {
		ConnectivityManager connMgr =
				(ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		boolean isWifiConn = false;
		boolean isMobileConn = false;
		for (Network network : connMgr.getAllNetworks()) {
			NetworkInfo networkInfo = connMgr.getNetworkInfo(network);
			if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
				isWifiConn |= networkInfo.isConnected();
			}
			if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
				isMobileConn |= networkInfo.isConnected();
			}
		}
		Log.d(DEBUG_TAG, "Wifi connected: " + isWifiConn);
		Log.d(DEBUG_TAG, "Mobile connected: " + isMobileConn);
	}

	private void createNotificationChannel() {
		// Create the NotificationChannel, but only on API 26+ because
		// the NotificationChannel class is new and not in the support library
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			int importance = NotificationManager.IMPORTANCE_HIGH;
			NotificationChannel channel = new NotificationChannel(PREDICTION_CHANNEL_ID, PREDICTION_CHANNEL_ID, importance);
			channel.setDescription(PREDICTION_CHANNEL_ID);
			// Register the channel with the system; you can't change the importance
			// or other notification behaviors after this
			NotificationManager notificationManager = getSystemService(NotificationManager.class);
			channel.enableVibration(true);
			channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
			notificationManager.createNotificationChannel(channel);
		}
	}

//	/**
//	 *
//	 * @param audioLabel
//	 */
//	public void createAudioLabelNotification(AudioLabel audioLabel) {
//		// Unique notification for each kind of sound
//		final int NOTIFICATION_ID = getIntegerValueOfSound(audioLabel.label);
//
//		// Disable same sound for 5 seconds
//		if (soundLastTime.containsKey(audioLabel.label)) {
//			if (System.currentTimeMillis() <= (soundLastTime.get(audioLabel.label) + 5 * 1000)) { //multiply by 1000 to get milliseconds
//				Log.i(TAG, "Same sound appear in less than 5 seconds");
//				return; // stop sending noti if less than 10 second
//			}
//		}
//		soundLastTime.put(audioLabel.label, System.currentTimeMillis());
//
//		Log.d(TAG, "generateBigTextStyleNotification()");
//		if (!notificationChannelIsCreated) {
//			createNotificationChannel();
//			notificationChannelIsCreated = true;
//		}
//		int loudness = (int) Double.parseDouble(audioLabel.db);
//
//		db = Integer.toString(loudness);
//		//Log.i(TAG, "level" + audioLabel.db + " " + db);
//
//		if(loudness > 70)
//			db = "Loud, " + db;
//		else if(loudness > 60)
//			db = "Med, " + db;
//		else
//			db = "Soft, " + db;
//
//
//		Intent intent = new Intent(this, MainActivity.class);       //Just go the MainActivity for now. Replace with other activity if you want more actions.
//		String[] dataPassed = {audioLabel.label, Double.toString(audioLabel.confidence), audioLabel.time, audioLabel.db};         //Adding data to be passed back to the main activity
//		intent.putExtra("audio_label", dataPassed);
//		intent.setAction(Long.toString(System.currentTimeMillis()));
//		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
//		int uniqueInt = (int) (System.currentTimeMillis() & 0xfffffff);
//		PendingIntent pendingIntent = PendingIntent.getActivity(this, uniqueInt, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//
//		NotificationCompat.Builder notificationCompatBuilder = new NotificationCompat.Builder(getApplicationContext(), PREDICTION_CHANNEL_ID)
//				.setSmallIcon(R.drawable.circle_white)
//				.setContentTitle(audioLabel.label)
//				.setContentText("(" + db + " dB)")
//				.setPriority(NotificationCompat.PRIORITY_MAX)
//				.setCategory(NotificationCompat.CATEGORY_ALARM)
//				.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
//				.setStyle(new NotificationCompat.BigTextStyle()
//						.bigText("")
//						.setSummaryText(""))
//				.setAutoCancel(true) //Remove notification from the list after the user has tapped it
//				.setContentIntent(pendingIntent);
//
//		//NOTIFICATION ID depends on the sound and the location so a particular sound in a particular location is only notified once until dismissed
//		Log.d(TAG, "Notification Id: " + NOTIFICATION_ID);
//		NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
//		notificationManager.notify(NOTIFICATION_ID, notificationCompatBuilder.build());
//
//	}

	private void startRecording(final Context main) {
		Intent serviceIntent = new Intent(main, ForegroundService.class);
		ContextCompat.startForegroundService(main, serviceIntent);
	}

	public void stopRecording(final Context main) {
		Intent serviceIntent = new Intent(main, ForegroundService.class);
		main.stopService(serviceIntent);
	}

	private void switchToStopRecord(ImageButton listeningBtn, RipplePulseRelativeLayout pulseLayout, TextView soundTextView) {
		listeningBtn.setBackground(getResources().getDrawable(R.drawable.rounded_background_blue, null));
		listeningBtn.setImageResource(R.drawable.ic_mic_24);
		soundTextView.setText(R.string.tap_blue);
		pulseLayout.stopPulse();
	}

	public static int getIntegerValueOfSound(String sound){
		int i = 0;
		for (char c : sound.toCharArray())
			i+=(int)c;
		return i;
	}
}
