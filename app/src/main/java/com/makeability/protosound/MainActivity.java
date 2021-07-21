package com.makeability.protosound;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.kuassivi.component.RipplePulseRelativeLayout;
import com.makeability.protosound.ui.SharedViewModel;
import com.makeability.protosound.ui.dashboard.DashboardFragment;
import com.makeability.protosound.ui.home.models.AudioLabel;
import com.makeability.protosound.ui.home.service.ForegroundService;
import com.makeability.protosound.ui.tutorial.Tutorial;
import com.makeability.protosound.utils.StreamingSoundRecorder;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.makeability.protosound.utils.Constants.TEST_NUMBER_EXTRA;

;

public class MainActivity extends AppCompatActivity {
	private static final String TAG = "MainActivity";
	public static int evalCount = 0;
	private static final int TOTAL_EVAL_PER_TESTER = 100;
	private Map<String, Long> soundLastTime = new HashMap<>();
	private List<AudioLabel> timeLine = new ArrayList<>();
	private List<Integer> ratedLabels = new ArrayList<>();

	private static final int NORMAL_MODE = 0;
	private static final int MAX_TIMELINE_SIZE = 10;
	private String location = "";
	//	public ListView listView;
	public static int currentMode = NORMAL_MODE;
	private SharedViewModel sharedViewModel;

	private SharedPreferences prefs = null;

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
		EventBus.getDefault().register(this);
		sharedViewModel = new ViewModelProvider(this).get(SharedViewModel.class);
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
//		listView = findViewById(R.id.listView);
		// Passing each menu ID as a set of Ids because each
		// menu should be considered as top level destinations.
		AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
				R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_tutorial)
				.build();
		NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
		//NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
		NavigationUI.setupWithNavController(navView, navController);
		navView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
			@Override
			public boolean onNavigationItemSelected(@NonNull MenuItem item) {
				if (item.getItemId() != navView.getSelectedItemId()) {
					NavigationUI.onNavDestinationSelected(item, navController);
				}
				return true;
			}
		}
		);

		prefs = getPreferences(MODE_PRIVATE);
	}
	@Override
	protected void onResume() {
		super.onResume();
		if (prefs.getBoolean("firstrun", true)) {
			startActivity(new Intent(this, Tutorial.class));
			prefs.edit().putBoolean("firstrun", false).apply();
		}

	}

	// https://stackoverflow.com/questions/4828636/edittext-clear-focus-on-touch-outside/8766475
	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			View v = getCurrentFocus();
			if (v instanceof EditText) {
				Rect outRect = new Rect();
				v.getGlobalVisibleRect(outRect);
				if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {	// Outside EditText
					v.clearFocus();
					v.setFocusableInTouchMode(false);	// Avoid focusing on EditText when scrolling is focused
					// Hide keyboard
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
				}
			}
		}
		return super.dispatchTouchEvent( event );
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onReceiveLocation (DashboardFragment event){
		Log.i(TAG, "Received location event");

		location = event.location;
	};

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onReceiveTrainingComplete (DashboardFragment event){
		Log.i(TAG, "Received training complete!");
		Log.i(TAG, "Received audio label event");

		Button submitButton = (Button) findViewById(R.id.submit);
		ProgressBar progressBar = findViewById(R.id.progressBar);
		submitButton.setBackgroundColor(getResources().getColor(R.color.purple_200));
		submitButton.setText(R.string.training_complete);
		submitButton.setTextColor(Color.BLACK);
		new Handler(Looper.getMainLooper()).post(() -> {
			progressBar.setVisibility(View.INVISIBLE);
		});

		TextView tutorial_4 = findViewById(R.id.tutorial_4);
		tutorial_4.setVisibility(View.GONE);
		TextView tutorial_5 = findViewById(R.id.tutorial_5);
		tutorial_5.setVisibility(View.VISIBLE);
	}


	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onReceiveAudioLabelEvent(StreamingSoundRecorder.RecordAudioAsyncTask event) {
		Log.i(TAG, "Received audio label event");

		String db = event.db;
		String audio_label = event.label;
		String accuracy = event.confidence;
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
		LocalTime localTime = LocalTime.now();
		String time = formatter.format(localTime);

		Log.i(TAG, "received sound label from Socket server: " + audio_label + ", " + accuracy);
		AudioLabel audioLabel = new AudioLabel(audio_label, accuracy, time, db, null);;

		Log.d(TAG, "onAudioLabelUIViewMessage " + audioLabel.getShortenLabel());

		timeLine.add(audioLabel);
		ratedLabels.add(0);
		if (timeLine.size() > MAX_TIMELINE_SIZE) {
			timeLine.remove(0);
			ratedLabels.remove(0);
		}
		soundLastTime.put(audioLabel.label, System.currentTimeMillis());
		ListView listView = findViewById(R.id.listView);
		new Handler(Looper.getMainLooper()).post(() -> {
			TimelineAdapter adapter = new TimelineAdapter(timeLine, getApplicationContext());
			sharedViewModel.setAdapter(adapter);
			listView.setAdapter(adapter);
			// Disable scrolling
			listView.setEnabled(false);
//				listView.setSelection(adapter.getCount() - 1);
//				adapter.notifyDataSetChanged();
		});
	}


	@Override
	protected void onDestroy() {
		Log.i(TAG, "onDestroy: ");
		EventBus.getDefault().unregister(this);
		super.onDestroy();
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
