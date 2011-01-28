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

public class TheTracker extends Activity {
	private String secret_code;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TrackThatThing.TAG, "this is inside the new screen.");

		setContentView(R.layout.the_tracker);

		Button btn = (Button) findViewById(R.id.btn_change_secret);
		btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				launchSecretGetter();
			}
		});
		
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
