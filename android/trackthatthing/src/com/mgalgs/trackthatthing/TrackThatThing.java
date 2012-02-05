package com.mgalgs.trackthatthing;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class TrackThatThing extends Activity {
	public static final String TAG = "TrackThatThing Message";
	public static final int GET_SECRET = 0;
	public static final int RESULT_CHANGE_GPS_SETTINGS = 1;
	public static final String PREFS_NAME = "TTTPrefsFile";
	public static final String PREF_SECRET_CODE = "secret_code";
	public static final String PREF_SLEEP_TIME = "sleep_time";
	public static final String PREF_FIRST_LAUNCH = "first_launch";
	public static final String PREF_LAST_LOC_TIME = "last_loc";
	public static long DEFAULT_SLEEP_TIME = 30; //seconds
	public static final String BASE_URL = Build.PRODUCT.equals("google_sdk")
			|| Build.PRODUCT.equals("sdk") ? "http://10.0.2.2:8080"
			: "http://www.trackthatthing.com";

	private String secret_code;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Show EULA:
        Eula.show(this);

		SharedPreferences settings = getSharedPreferences(
				TrackThatThing.PREFS_NAME, MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		editor.remove(PREF_LAST_LOC_TIME);
		editor.commit();
		
        setContentView(R.layout.main);
        
        Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				launchStuff();
			}
		});

    } // eo onCreate
    
    public boolean gpsIsEnabled() {
		final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		return manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    // from http://stackoverflow.com/questions/843675/how-do-i-find-out-if-the-gps-of-an-android-device-is-enabled/843716#843716
	private void buildAlertMessageNoGps() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(
				"Your GPS is disabled, do you want to enable it?")
				.setCancelable(false)
				.setPositiveButton("Yes",
						new DialogInterface.OnClickListener() {
							public void onClick(
									final DialogInterface dialog,
									final int id) {
								launchGPSOptions();
							}
						})
				.setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog,
							final int id) {
						dialog.cancel();
					}
				});
		final AlertDialog alert = builder.create();
		alert.show();
	}
    
    public void launchTracker() {
    	if (!gpsIsEnabled()) {
    		buildAlertMessageNoGps();
    		return;
    	}
    	
        Intent i = new Intent(this, TheTracker.class);
        i.putExtra("auto_start_tracking", true);
        startActivity(i);
    }
    public void launchSecretGetter() {
        Intent i = new Intent(this, TheSecretGetter.class);
        startActivityForResult(i, GET_SECRET);
    }
    /**
     * Launches either the tracker screen or the "get secret" screen.
     * @param dont_try_to_get_secret To avoid infinite trying to get secret loops,
     * 	we can call this function with dont_try_to_get_secret=false to not launch the secret getter screen 
     */
    public void launchStuff(boolean dont_try_to_get_secret) {
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
		secret_code = settings.getString(PREF_SECRET_CODE, null);
		
		if (secret_code != null) {
			launchTracker();
		} else {
			if (!dont_try_to_get_secret) {
				launchSecretGetter();
			} else {
				Toast.makeText(TrackThatThing.this, "No secret code provided. Can't start tracking.", Toast.LENGTH_LONG).show();
			}
		}
    }
    /**
     * Just call the launchStuff function with false "default value"
     */
    public void launchStuff() {
    	launchStuff(false);
    }

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
        switch (requestCode) {
        case GET_SECRET:
        	launchStuff(true);
        	break;
        case RESULT_CHANGE_GPS_SETTINGS:
//        	for some reason launching the next activity isn't working here...
//        	runOnUiThread(new Runnable() {
//				public void run() {
//					launchStuff();
//				}
//			});
        	break;
        }
	}
	
	
	private void launchGPSOptions() {
		final Intent intent = new Intent(
				Settings.ACTION_LOCATION_SOURCE_SETTINGS);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivityForResult(intent, RESULT_CHANGE_GPS_SETTINGS);
	}    

}