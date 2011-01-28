package com.mgalgs.trackthatthing;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class TheTracker extends Activity {
	private String secret_code;
	private Button toggleTrackingButton;
	private boolean currently_tracking = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TrackThatThing.TAG, "this is inside the new screen.");

		setContentView(R.layout.the_tracker);

		// the change secret button:
		Button btn = (Button) findViewById(R.id.btn_change_secret);
		btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				launchSecretGetter();
			}
		});

		// the toggle tracking button:
		toggleTrackingButton = (Button) findViewById(R.id.btn_toggle_tracking);
		toggleTrackingButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				toggleTracking();
			}
		});
		
		startTracking();

        refreshDisplay();
	}
	
    public void launchSecretGetter() {
        Intent i = new Intent(this, TheSecretGetter.class);
        startActivityForResult(i, TrackThatThing.GET_SECRET);
    }
    
    public void refreshDisplay() {
        TextView tv = (TextView) findViewById(R.id.the_secret_code);
        
		SharedPreferences settings = getSharedPreferences(TrackThatThing.PREFS_NAME, MODE_PRIVATE);
		secret_code = settings.getString(TrackThatThing.PREF_SECRET_CODE, null);

		if (secret_code != null) {
			tv.setText(secret_code);
		} else {
			Log.e(TrackThatThing.TAG, "Weird, they got to the tracker screen without a secret code...");
		}
    }
    
    public void startTracking() {
    	toggleTrackingButton.setText("Stop tracking");
    	Toast.makeText(this, "Tracking started.", Toast.LENGTH_SHORT).show();
    	
    	currently_tracking = true;
    }
    public void stopTracking() {
    	toggleTrackingButton.setText("Start tracking");
    	Toast.makeText(this, "Tracking stopped.", Toast.LENGTH_SHORT).show();
    	
    	currently_tracking = false;
    }
    public void toggleTracking() {
    	if (currently_tracking) {
    		stopTracking();
    	} else {
    		startTracking();
    	}
    }
    
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
        switch (requestCode) {
        case TrackThatThing.GET_SECRET:
        	refreshDisplay();
        	break;
        }
	}



}
