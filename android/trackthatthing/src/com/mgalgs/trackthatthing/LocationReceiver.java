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
 
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.SystemClock;
import android.util.Log;

import com.commonsware.cwac.locpoll.LocationPoller;
 
public class LocationReceiver extends BroadcastReceiver {

	Context mContext;
	String msg;

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

			// TODO: Notify a Service of this location and let it do the asynch saving.
			LocationWrapper l = new LocationWrapper(loc);
			l.saveToCloud(mContext);
			
			msg = loc.toString();
		}

		Log.d(TrackThatThing.TAG, "got this location: " + msg);
	}
}