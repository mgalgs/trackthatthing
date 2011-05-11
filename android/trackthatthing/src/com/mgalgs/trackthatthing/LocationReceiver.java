/***
	Copyright (c) 2010 CommonsWare, LLC
	
	 Licensed under the Apache License, Version 2.0 (the "License"); you may
	 not use this file except in compliance with the License. You may obtain
	 a copy of the License at
		 http://www.apache.org/licenses/LICENSE-2.0
	 Unless required by applicable law or agreed to in writing, software
	 distributed under the License is distributed on an "AS IS" BASIS,
	 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	 See the License for the specific language governing permissions and
	 limitations under the License.
 */
 
 package com.mgalgs.trackthatthing;
 
 //import org.json.JSONException;
 //import org.json.JSONObject;
 
 import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import com.commonsware.cwac.locpoll.LocationPoller;
 
public class LocationReceiver extends BroadcastReceiver {

	// need a handler for callbacks to the ui
	final Handler mHandler = new Handler();

	Context mContext;
	String msg;

	// create runnable for posting
	final Runnable mUpdateResults = new Runnable() {
		public void run() {
			if (msg == null) {
				msg = "Invalid broadcast received!";
			} else {
				updateResultsInUi();
			} // eo else
		}
	};
	
	public static long mLastLocTime = -1;
	
	public long getTimeSinceLastLoc_MS() {
		return (SystemClock.elapsedRealtime() - mLastLocTime);
	}

	public long getTimeSinceLastLoc_S() {
		return getTimeSinceLastLoc_MS() / 1000;
	}


	@Override
	public void onReceive(Context context, Intent intent) {
		mContext = context;
		Location loc = (Location) intent.getExtras().get(
				LocationPoller.EXTRA_LOCATION);

		if (loc == null) {
			msg = intent.getStringExtra(LocationPoller.EXTRA_ERROR);
		} else {
			SharedPreferences settings = context.getSharedPreferences(
					TrackThatThing.PREFS_NAME, Context.MODE_PRIVATE);
			
			long sleep_period = settings.getLong(TrackThatThing.PREF_SLEEP_TIME, TrackThatThing.DEFAULT_SLEEP_TIME);
			if (getTimeSinceLastLoc_S() > sleep_period - 3 || mLastLocTime == -1) {
				// it has been long enough
			} else {
				// it hasn't been long enough!
				Log.d(TrackThatThing.TAG, "It has only been " + getTimeSinceLastLoc_S() + " seconds since the last location update, not long enough!");
				return;
			}
			
			mLastLocTime = SystemClock.elapsedRealtime();
			
			
			float acc = loc.getAccuracy();
			double lat = loc.getLatitude();
			double lon = loc.getLongitude();
			float speed = loc.getSpeed();

			String secret_code = settings.getString(
					TrackThatThing.PREF_SECRET_CODE, null);

			QueryString qs = new QueryString(TrackThatThing.BASE_URL + "/put");
			qs.add("secret", secret_code);
			qs.add("lat", Double.toString(lat));
			qs.add("lon", Double.toString(lon));
			qs.add("acc", Float.toString(acc));
			qs.add("speed", Float.toString(speed));

			Runnable r = new MyInternetThread(qs);
			new Thread(r).start();

			msg = loc.toString();
		}

		Log.d(TrackThatThing.TAG, "got this location: " + msg);
	}

	// A runnable for the HTTP request
	public class MyInternetThread implements Runnable {
		public final QueryString qs;

		public MyInternetThread(QueryString qs_) {
			qs = qs_;
		}

		public void run() {
			try {
				JSONObject json = RestClient.connect(qs.toString());
				Log.i(TrackThatThing.TAG,
						"Got the following response from the server: "
								+ json.getString("msg"));
				mHandler.post(mUpdateResults);
			} catch (JSONException e) {
				Log.e(TrackThatThing.TAG,
						"couldn't get \"msg\" out of JSON object...");
				e.printStackTrace();
			} catch (Exception e) {
				Log.e(TrackThatThing.TAG,
						"Something went wrong while trying to make the JSON object...");
				e.printStackTrace();
			}
		}
	}
	
	public void updateResultsInUi() {
		SharedPreferences settings = mContext
				.getSharedPreferences(TrackThatThing.PREFS_NAME,
						android.content.Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();

		SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss a yyyy-MM-dd");
		Calendar cal = Calendar.getInstance();

		editor.putString(TrackThatThing.PREF_LAST_LOC_TIME,
				sdf.format(cal.getTime()));
		editor.commit();

		Intent i = new Intent(TheTracker.IF_LOC_UPDATE);
		// i.putExtra(TimerDB.KEY_ID, id);
		Log.d(TrackThatThing.TAG, "Pinging TheTracker...");
		mContext.sendOrderedBroadcast(i, null, new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				int result = getResultCode();
				if (result != Activity.RESULT_CANCELED) {
					Log.d(TrackThatThing.TAG,
							"TheTracker caught the broadcast, result " + result);
					return; // Activity caught it
				}
				Log.d(TrackThatThing.TAG,
						"TimerActivity did not catch the broadcast");
			}
		}, null, Activity.RESULT_CANCELED, null, null);
	}
}