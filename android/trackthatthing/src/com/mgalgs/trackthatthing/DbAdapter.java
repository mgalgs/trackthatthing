package com.mgalgs.trackthatthing;

import android.content.Context;
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
	private static final String KEY_SAVED_TO_CLOUD = "saved_to_cloud";

	private static final String ARTICLES_TABLE_CREATE = "CREATE TABLE "
			+ DATABASE_TABLE + " (" + KEY_ROW_ID
			+ " INTEGER PRIMARY KEY AUTOINCREMENT," + KEY_DATE_RECORDED
			+ " TEXT NOT NULL," + KEY_LATITUDE + " FLOAT NOT NULL,"
			+ KEY_LONGITUDE + " FLOAT NOT NULL " + KEY_SAVED_TO_CLOUD + " INTEGER DEFAULT 0" + ");";

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
			// TODO Auto-generated method stub
			Log.i(TrackThatThing.TAG, "Doing a DB upgrade on WikiPaper!");
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
		// TODO: save it!
		return 0;
	}
} // eo class DbAdapter
