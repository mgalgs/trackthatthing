package com.mgalgs.trackthatthing;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class TheSecretGetter extends Activity {
	public Button btn;
	public EditText et;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.the_secret_getter);

		btn = (Button) findViewById(R.id.btn_save_secret);
		et = (EditText) findViewById(R.id.txt_save_secret);

		SharedPreferences settings = getSharedPreferences(TrackThatThing.PREFS_NAME, MODE_PRIVATE);
		String secret_code = settings.getString(TrackThatThing.PREF_SECRET_CODE, null);
		if (secret_code != null) {
			et.setText(secret_code);
			et.requestFocus();
		}

		btn.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				String secret_code = et.getText().toString().trim();

	        	if (secret_code != null) {
	            	SharedPreferences settings = getSharedPreferences(TrackThatThing.PREFS_NAME, MODE_PRIVATE);
	            	SharedPreferences.Editor editor = settings.edit();
	            	editor.putString(TrackThatThing.PREF_SECRET_CODE, secret_code);
	            	editor.apply();
	        	}

                Bundle bundle = new Bundle();
                bundle.putString(TrackThatThing.PREF_SECRET_CODE, secret_code);

                Intent mIntent = new Intent();
                mIntent.putExtras(bundle);
                setResult(RESULT_OK, mIntent);
				finish();
			}
		});

		((Button) findViewById(R.id.btn_get_secret)).setOnClickListener(new MyOnClickGetSecret());
	} /* eo onCreate */

	private class MyOnClickGetSecret implements OnClickListener {
		public void onClick(View v) {
			Intent webIntent = new Intent(Intent.ACTION_VIEW,
					Uri.parse(TrackThatThing.BASE_URL + "/secret"));
			startActivity(webIntent);
		}
	}

}
