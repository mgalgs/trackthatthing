package com.mgalgs.trackthatthing;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class TrackThatThing extends Activity {
	public static final String TAG = "TrackThatThing Message";
	public static final int GET_SECRET = 0;
	public static final String PREFS_NAME = "TTTPrefsFile";
	public static final String PREF_SECRET_CODE = "secret_code";
	public static String PREF_SLEEP_TIME = "sleep_time";
	public static String PREF_FIRST_LAUNCH = "first_launch";
	public static long DEFAULT_SLEEP_TIME = 30; //seconds
	public static final String BASE_URL = "http://www.trackthatthing.com";
//	public static final String BASE_URL = "http://firsthome.homelinux.org:8080";
//	public static final String BASE_URL = "http://sonch:8080";
	
	private String secret_code;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Show EULA:
        Eula.show(this);

        setContentView(R.layout.main);
        
        Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Log.i(TAG, "Thanks for clicking our little tracker friend man");
				launchStuff();
			}
		});
    }
    
    public void launchTracker() {
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
//			Log.i(TAG, "Got secret code: " + secret_code);
			launchTracker();
		} else {
//			Log.i(TAG, "No secret code. Launching secret getter: " + new Boolean(!dont_try_to_get_secret).toString());
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
        }
	}
    
    
    

}