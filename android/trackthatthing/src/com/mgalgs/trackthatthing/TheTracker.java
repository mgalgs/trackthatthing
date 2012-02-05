package com.mgalgs.trackthatthing;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.commonsware.cwac.locpoll.LocationPoller;

public class TheTracker extends Activity {
	private String secret_code;
	private Button toggleTrackingButton;
	private TextView theStatusTV;
	private PendingIntent pi = null;
	private AlarmManager mgr = null;
	private boolean mCurrentlyTracking = false;
	static final String IF_LOC_UPDATE = "com.mgalgs.trackthatthing.LOC_UPDATE";
	private DbAdapter mDbAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.the_tracker);
		
		mDbAdapter = new DbAdapter(this);
		mDbAdapter.open();

		// set up the LocationPoller:
		mgr = (AlarmManager) getSystemService(ALARM_SERVICE);

		Intent i = new Intent(this, LocationPoller.class);

		i.putExtra(LocationPoller.EXTRA_INTENT, new Intent(this,
				LocationReceiver.class));
		i.putExtra(LocationPoller.EXTRA_PROVIDER, LocationManager.GPS_PROVIDER);

		pi = PendingIntent.getBroadcast(this, 0, i, 0);
		mgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
				SystemClock.elapsedRealtime(), getSleepTime_MS(), pi);
		mCurrentlyTracking = true;
		Toast.makeText(this, "Tracking started", Toast.LENGTH_LONG).show();

		// the change secret button:
		Button btn = (Button) findViewById(R.id.btn_change_secret);
		btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				launchSecretGetter();
			}
		});

		Button shareBtn = (Button) findViewById(R.id.btn_share_location);
		shareBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				shareSecret();
			}
		});

		// the toggle tracking button:
		toggleTrackingButton = (Button) findViewById(R.id.btn_toggle_tracking);
		toggleTrackingButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				toggleTracking();
			}
		});
		toggleTrackingButton.setText("Stop tracking");

		theStatusTV = (TextView) findViewById(R.id.the_status_label);

		// the sleep time spinner:
		Spinner spinner = (Spinner) findViewById(R.id.spinner_update_rate);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				this, R.array.array_update_rates,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		spinner.setOnItemSelectedListener(new MyOnItemSelectedListener());

		refreshDisplay();
	}

	@Override
	protected void onStart() {
		// Log.i(TrackThatThing.TAG, "TheTracker.onStart being called");
		super.onStart();
	}

	public void launchSecretGetter() {
		Intent i = new Intent(this, TheSecretGetter.class);
		startActivityForResult(i, TrackThatThing.GET_SECRET);
	}

	public void refreshDisplay() {
		TextView tv = (TextView) findViewById(R.id.the_secret_code);

		theStatusTV.setText(mCurrentlyTracking ? "Tracking" : "Not Tracking");

		SharedPreferences settings = getSharedPreferences(
				TrackThatThing.PREFS_NAME, MODE_PRIVATE);
		secret_code = settings.getString(TrackThatThing.PREF_SECRET_CODE, null);
		if (secret_code != null) {
			tv.setText(secret_code);
		} else {
			Log.e(TrackThatThing.TAG,
					"Weird, they got to the tracker screen without a secret code...");
		}

		tv = (TextView) findViewById(R.id.the_last_update_label);
		tv.setText(mDbAdapter.getTimeOfLastSave());
	}

	public void toggleTracking() {
		if (mCurrentlyTracking) {
			mgr.cancel(pi);
			mCurrentlyTracking = false;
			toggleTrackingButton.setText("Start tracking");
			Toast.makeText(this, "Tracking stopped", Toast.LENGTH_LONG).show();
		} else {
			mgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
					SystemClock.elapsedRealtime(), getSleepTime_MS(), pi);
			mCurrentlyTracking = true;
			toggleTrackingButton.setText("Stop tracking");
			Toast.makeText(this, "Tracking started", Toast.LENGTH_LONG).show();
		}
		refreshDisplay();
	}

	public void updateTrackingPeriod() {
		if (mCurrentlyTracking) {
			mgr.cancel(pi);
			mgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
					SystemClock.elapsedRealtime(), getSleepTime_MS(), pi);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mDbAdapter.close();
	}

	@Override
	protected void onPause() {
		unregisterReceiver(locUpdateReceiver);
		super.onPause();
	}

	@Override
	protected void onRestart() {
		super.onRestart();
	}

	@Override
	protected void onResume() {
		registerReceiver(locUpdateReceiver, new IntentFilter(IF_LOC_UPDATE));
		super.onResume();

		refreshDisplay();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		switch (requestCode) {
		case TrackThatThing.GET_SECRET:
			refreshDisplay();
			break;
		}
	}

	private long getSleepTime_S() {
		SharedPreferences settings = getSharedPreferences(
				TrackThatThing.PREFS_NAME, MODE_PRIVATE);
		return settings.getLong(TrackThatThing.PREF_SLEEP_TIME,
				TrackThatThing.DEFAULT_SLEEP_TIME);
	}

	private long getSleepTime_MS() {
		return getSleepTime_S() * 1000;
	}

	public class MyOnItemSelectedListener implements OnItemSelectedListener {
		public void onItemSelected(AdapterView<?> parent, View view, int pos,
				long id) {
			long new_sleep_time_s = 30;
			String choice = parent.getItemAtPosition(pos).toString();
			String[] stuff = choice.split(" ");
			long multiplier;
			if (stuff[1].equals("seconds")) {
				multiplier = 1;
			} else if (stuff[1].equals("minutes")) {
				multiplier = 60;
			} else if (stuff[1].equals("hours")) {
				multiplier = 60 * 60;
			} else {
				Log.e(TrackThatThing.TAG, "why didn't we find a multiplier???");
				multiplier = 1; // we really shouldn't get here...
			}
			new_sleep_time_s = Long.valueOf(stuff[0]) * multiplier;

			SharedPreferences settings = getSharedPreferences(
					TrackThatThing.PREFS_NAME, MODE_PRIVATE);
			SharedPreferences.Editor editor = settings.edit();
			editor.putLong(TrackThatThing.PREF_SLEEP_TIME, new_sleep_time_s);
			editor.commit();

			updateTrackingPeriod();
		}

		public void onNothingSelected(AdapterView<?> parent) {

		}
	}

	private void shareSecret() {
		String subject = "See where I'm at in real-time!";
		String bodyText = String
				.format("<div>Hey! I'm using <a href=\"http://www.trackthatthing.com\">TrackThatThing</a> "
						+ "to track my location.</div><div>Check out the real-time map of my location "
						+ "<a href=\"http://www.trackthatthing.com/live?secret=%s\">here</a>.</div>",
						secret_code);

		final Intent emailIntent = new Intent(
				android.content.Intent.ACTION_SEND);
		emailIntent.setType("text/html");
		emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
		emailIntent.putExtra(android.content.Intent.EXTRA_TEXT,
				Html.fromHtml(bodyText));
		startActivity(Intent.createChooser(emailIntent, "Email:"));
	}

	public BroadcastReceiver locUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			refreshDisplay();
		}
	};
}
