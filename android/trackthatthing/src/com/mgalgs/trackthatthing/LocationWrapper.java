package com.mgalgs.trackthatthing;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.util.Log;

public class LocationWrapper {
	float mAccuracy;
	double mLongitude;
	double mLatitude;
	float mSpeed;
	Date mDateRecorded;
	public static final SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat(
			"hh:mm:ss a yyyy-MM-dd");
	public final Calendar mCal = Calendar.getInstance();
	public boolean mSavedToCloud = false;
	public boolean mSavedToDb = false;

	// Some database-related vars
	int mRowId; // row in database

	/**
	 * Constructor for a cloud-and-database-backed Location object.
	 * 
	 * @param accuracy Accuracy of this location
	 * @param longitude Longitude of this location
	 * @param latitude Latitude of this location
	 * @param speed Speed at measurement of this location
	 */
	public LocationWrapper(Location l) {
		mAccuracy = l.getAccuracy();
		mLongitude = l.getLongitude();
		mLatitude = l.getLatitude();
		mSpeed = l.getSpeed();
		mDateRecorded = mCal.getTime();
	}

	/**
	 * @return formatted date string for when this location was recorded
	 */
	String getDateString() {
		return mSimpleDateFormat.format(mDateRecorded);
	}

	/**
	 * Saves this location to the database
	 * 
	 * @return true on success, else false
	 */
	public boolean saveToDb() {
//		mRowId =  .SaveLocationToDb(this);
//		if (mRowId >= 0) {
//			mSavedToDb = true;
//		}
		return true;
	}

	/**
	 * Saves this location instance to the cloud. Will also save to database if
	 * needed.
	 * 
	 * @return
	 */
	public boolean saveToCloud(Context context) {
		if (!mSavedToDb) {
			saveToDb();
		}

		SharedPreferences settings = context.getSharedPreferences(
				TrackThatThing.PREFS_NAME, Context.MODE_PRIVATE);

		String secret_code = settings.getString(
				TrackThatThing.PREF_SECRET_CODE, null);

		QueryString qs = new QueryString(TrackThatThing.BASE_URL + "/put");
		qs.add("secret", secret_code);
		qs.add("lat", Double.toString(mLatitude));
		qs.add("lon", Double.toString(mLongitude));
		qs.add("acc", Float.toString(mAccuracy));
		qs.add("speed", Float.toString(mSpeed));
		qs.add("date", Long.toString(mDateRecorded.getTime()));

		Runnable r = new MyInternetThread(context, qs);
		new Thread(r).start();

		return true;
	}

	// A runnable for the HTTP request
	public class MyInternetThread implements Runnable {
		public final QueryString qs;
		public final Context mContext;

		public MyInternetThread(Context context, QueryString qs_) {
			qs = qs_;
			mContext = context;
		}

		public void run() {
			try {
				JSONObject json = RestClient.connect(qs.toString());
				Log.i(TrackThatThing.TAG,
						"Got the following response from the server: "
								+ json.getString("msg"));
				// succesfully
				mSavedToCloud = true;
				Intent i = new Intent(TheTracker.IF_LOC_UPDATE);
				mContext.sendBroadcast(i);
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
} // eo class Location
