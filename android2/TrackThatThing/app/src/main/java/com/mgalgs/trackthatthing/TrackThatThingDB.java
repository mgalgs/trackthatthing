package com.mgalgs.trackthatthing;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by mitchhumpherys on 1/4/15.
 */
public final class TrackThatThingDB extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "TrackThatThing.db";

    private SQLiteDatabase mDb;

    /*
     * This could take a while due to the getWritableDatabase...
     * Please do it on a background thread.
     */
    public TrackThatThingDB(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mDb = getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TrackingCodeEntry.SQL_CREATE_TRACKING_CODE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // implement when needed in the future...
    }

    public static final class TrackingCodeEntry implements BaseColumns {
        public static final String TABLE_NAME = "tracking_code";
        public static final String COLUMN_NAME_TRACKING_CODE = "tracking_code";
        public static final String COLUMN_NAME_LAST_USE = "last_use";
        // hacky?
        public final String[] ALL_COLUMNS = {
                _ID,
                COLUMN_NAME_TRACKING_CODE,
                COLUMN_NAME_LAST_USE
        };
        public String mTrackingCode;
        public String mLastUse;
        public long mId;

        TrackingCodeEntry() {}

        TrackingCodeEntry(long id, String trackingCode, String lastUse) {
            mId = id;
            mTrackingCode = trackingCode;
            mLastUse = lastUse;
        }

        private static final String SQL_CREATE_TRACKING_CODE_TABLE =
                "CREATE TABLE " + TrackingCodeEntry.TABLE_NAME + " (" +
                        TrackingCodeEntry._ID + " INTEGER PRIMARY KEY," +
                        TrackingCodeEntry.COLUMN_NAME_TRACKING_CODE + " TEXT UNIQUE," +
                        TrackingCodeEntry.COLUMN_NAME_LAST_USE + " INTEGER" +
                        " )";

        private static final String SQL_DELETE_TRACKING_CODE_TABLE =
                "DROP TABLE IF EXISTS " + TrackingCodeEntry.TABLE_NAME;
    }

    public TrackingCodeEntry getTrackingCodeEntry(String trackingCode) {
        Cursor c = mDb.query(TrackingCodeEntry.TABLE_NAME,
                (new TrackingCodeEntry()).ALL_COLUMNS,
                TrackingCodeEntry.COLUMN_NAME_TRACKING_CODE + " = ?",
                new String[]{trackingCode},
                null, null, null);
        c.moveToFirst();
        try {
            return new TrackingCodeEntry(
                    c.getLong(c.getColumnIndexOrThrow(TrackingCodeEntry._ID)),
                    c.getString(c.getColumnIndexOrThrow(TrackingCodeEntry.COLUMN_NAME_TRACKING_CODE)),
                    c.getString(c.getColumnIndexOrThrow(TrackingCodeEntry.COLUMN_NAME_LAST_USE))
            );
        } catch (CursorIndexOutOfBoundsException e) {
            return null;
        }
    }

    public Cursor getTrackingCodesCursor() {
        return mDb.query(TrackingCodeEntry.TABLE_NAME,
                (new TrackingCodeEntry()).ALL_COLUMNS,
                null, null, null, null, TrackingCodeEntry.COLUMN_NAME_LAST_USE + " DESC");
    }

    public long addTrackingCode(String trackingCode) {
        if (getTrackingCodeEntry(trackingCode) != null) {
            Log.e(TrackThatThing.TAG, "tracking code already in database: " + trackingCode);
            return -1;
        }

        ContentValues values = new ContentValues();
        values.put(TrackingCodeEntry.COLUMN_NAME_TRACKING_CODE, trackingCode);
        values.put(TrackingCodeEntry.COLUMN_NAME_LAST_USE, System.currentTimeMillis());

        return mDb.insert(TrackingCodeEntry.TABLE_NAME, null, values);
    }

    public void deleteTrackingCode(String trackingCode) {
        int count = mDb.delete(TrackingCodeEntry.TABLE_NAME,
                TrackingCodeEntry.COLUMN_NAME_TRACKING_CODE + " = ?",
                new String[]{trackingCode});
        if (count != 1) {
            Log.e(TrackThatThing.TAG, "tried to delete code " + trackingCode +
                    " but got count: " + String.valueOf(count));
        }
    }

    public void updateTrackingCodeLastUse(String trackingCode) {
        ContentValues values = new ContentValues();
        values.put(TrackingCodeEntry.COLUMN_NAME_LAST_USE, System.currentTimeMillis());

        int count = mDb.update(TrackingCodeEntry.TABLE_NAME,
                values, TrackingCodeEntry.COLUMN_NAME_TRACKING_CODE + " = ?",
                new String[] {trackingCode});
        if (count != 1) {
            Log.e(TrackThatThing.TAG, "tried to update last use of code " + trackingCode +
                    " but got count: " + String.valueOf(count));
        }
    }
}
