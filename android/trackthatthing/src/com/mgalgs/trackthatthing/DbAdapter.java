package com.mgalgs.trackthatthing;

import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


public class DbAdapter {
	private static final String DATABASE_NAME = "TrackThatThing";
	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_TABLE = "Locations";

	private static final String KEY_ROW_ID = "_id";
	private static final String KEY_DATE_RECORDED = "date_recorded";
	private static final String KEY_LATITUDE = "latitude";
	private static final String KEY_LONGITUDE = "longitude";
	private static final String KEY_SPEED = "speed";
	private static final String KEY_SAVED_TO_CLOUD = "saved_to_cloud";

	private static final String ARTICLES_TABLE_CREATE = "CREATE TABLE "
			+ DATABASE_TABLE + " (" + KEY_ROW_ID
			+ " INTEGER PRIMARY KEY AUTOINCREMENT, " + KEY_DATE_RECORDED
			+ " INTEGER NOT NULL, " + KEY_LATITUDE + " FLOAT NOT NULL, "
			+ KEY_LONGITUDE + " FLOAT NOT NULL, " + KEY_SPEED
			+ " FLOAT NOT NULL, " + KEY_SAVED_TO_CLOUD + " INTEGER DEFAULT 0"
			+ ");";

	private final Context mCtx;
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;
	
	private static class DatabaseHelper extends SQLiteOpenHelper {
		public DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			Log.i(TrackThatThing.TAG, "In onCreate of DatabaseHelper");
			db.execSQL(ARTICLES_TABLE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.i(TrackThatThing.TAG, "Doing a DB upgrade on TrackThatThing!");
		}
	}
	
	public DbAdapter(Context context) {
		mCtx = context;
	}
	
	public DbAdapter open() throws SQLException {
		Log.i(TrackThatThing.TAG, "opening DataManager");
		mDbHelper = new DatabaseHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		Log.i(TrackThatThing.TAG, "closing DataManager");
		mDbHelper.close();
	}

	/**
	 * Saves the given LocationWrapper to the database.
	 * @param l LocationWrapper instance to save. Will not be modified.
	 * @return the row id of the database row, or -1 on error.
	 */
	public int SaveLocationToDb(LocationWrapper l) {
		ContentValues cv = new ContentValues();
		cv.put(KEY_DATE_RECORDED, l.mDateRecorded.getTime());
		cv.put(KEY_LATITUDE, l.mLatitude);
		cv.put(KEY_LONGITUDE, l.mLongitude);
		cv.put(KEY_SAVED_TO_CLOUD, false);
		try {
			mDb.insertOrThrow(DATABASE_TABLE, null, cv);
		} catch (Exception e) {
			Log.e(TrackThatThing.TAG, "Error saving location from " + l.getDateString());
		}
		return 0;
	}
	
	/**
	 * Gets the time of the last save
	 * @return Time of last save
	 */
	public String getTimeOfLastSave() {
		Cursor c = null;
		String cols[] = new String[] {KEY_ROW_ID, KEY_DATE_RECORDED, KEY_SAVED_TO_CLOUD};
		
		try {
			c = mDb.query(DATABASE_TABLE, cols, null, null, null, null,
					KEY_ROW_ID + " DESC", "1");
			if (c != null && c.getCount() > 0) {
				c.moveToFirst();
				long theDate = c.getLong(c.getColumnIndexOrThrow(KEY_DATE_RECORDED));
				return LocationWrapper.mSimpleDateFormat.format(new Date(theDate));
			}
		} catch (Exception e) {
			Log.e(TrackThatThing.TAG, "Error getting last location!");
		}
		return "Waiting for GPS lock...";
	}
} // eo class DbAdapter
