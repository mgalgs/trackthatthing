package com.mgalgs.trackthatthing;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Reference:
 * https://github.com/googlesamples/android-play-location/blob/master/LocationUpdates/app/src/main/java/com/google/android/gms/location/sample/locationupdates/MainActivity.java
 */

public class MyLocationService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private LocationRequest mLocationRequest;

    // Milliseconds per second
    private static final int MILLISECONDS_PER_SECOND = 1000;
    // Update frequency in seconds
    public static final int UPDATE_INTERVAL_IN_SECONDS = 10;
    // Update frequency in milliseconds
    private static final long UPDATE_INTERVAL =
            MILLISECONDS_PER_SECOND * UPDATE_INTERVAL_IN_SECONDS;
    // The fastest update frequency, in seconds
    private static final int FASTEST_INTERVAL_IN_SECONDS = 7;
    // A fast frequency ceiling in milliseconds
    private static final long FASTEST_INTERVAL =
            MILLISECONDS_PER_SECOND * FASTEST_INTERVAL_IN_SECONDS;
    private boolean mResolvingError = false;
    private GoogleApiClient mGoogleApiClient;
    public static boolean tracking;


    public MyLocationService() {
    }

    private NotificationManager mNM;

    // Unique Identification Number for the Notification.
    // We use it on Notification start, and to cancel it.
    private int NOTIFICATION = R.string.location_service_started;

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        MyLocationService getService() {
            return MyLocationService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TrackThatThing.TAG, "onCreate()'ing. connecting location client...");

        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(
                LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);

        buildGoogleApiClient();

        // Display a notification about us starting.  We put an icon in the status bar.
        showNotification();
        MyLocationService.tracking = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("MyLocationService", "Received start id " + startId + ": " + intent);
        mGoogleApiClient.connect();
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        MyLocationService.tracking = false;
        // Cancel the persistent notification.
        mNM.cancel(NOTIFICATION);

        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }

        // Tell the user we stopped.
        Toast.makeText(this, "Tracking stopped", Toast.LENGTH_SHORT).show();

        super.onDestroy();
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();

    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence title = getText(R.string.location_service_started);
        CharSequence text = getText(R.string.touch_to_stop);

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_ttt_icon)
                .setContentTitle(title)
                .setContentText(text)
                .setOngoing(true)
                .setContentIntent(contentIntent)
                .build();
//        Notification notification = new Notification(R.drawable.stat_sample, text,
//                System.currentTimeMillis());

        // Send the notification.
        mNM.notify(NOTIFICATION, notification);
    }


    /*
     * Called by Location Services when the request to connect the
     * client finishes successfully. At this point, you can
    * request the current location or start periodic updates
    */
    @Override
    public void onConnected(Bundle connectionHint) {
        // Display the connection status
        Log.d(TrackThatThing.TAG, "location services connected!");
        startLocationUpdates();
    }

    /**
     * Requests location updates from the FusedLocationApi.
     */
    protected void startLocationUpdates() {
        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TrackThatThing.TAG, "Connection suspended");
        Toast.makeText(this, "Location services connection suspended!", Toast.LENGTH_SHORT).show();
        mGoogleApiClient.connect();
    }

    /*
     * Called by Location Services if the attempt to
     * Location Services fails.
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TrackThatThing.TAG, "Location services connection failed!");
        Toast.makeText(this, "Location services connection failed!!!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocationChanged(Location location) {
        // Report to the UI that the location was updated
        String msg = "Updated Location: " +
                Double.toString(location.getLatitude()) + "," +
                Double.toString(location.getLongitude());
        Log.d(TrackThatThing.TAG, msg);

        SharedPreferences settings = getSharedPreferences(TrackThatThing.PREFS_NAME, MODE_PRIVATE);

        // long lastLocTime = SystemClock.elapsedRealtime();

        float acc = location.getAccuracy();
        double lat = location.getLatitude();
        double lon = location.getLongitude();
        float speed = location.getSpeed();

        String secret_code = settings.getString(TrackThatThing.PREF_SECRET_CODE, null);

        QueryString qs = new QueryString(TrackThatThing.BASE_URL + "/put");
        qs.add("secret", secret_code);
        qs.add("lat", Double.toString(lat));
        qs.add("lon", Double.toString(lon));
        qs.add("acc", Float.toString(acc));
        qs.add("speed", Float.toString(speed));

        Runnable r = new MyInternetThread(qs);
        new Thread(r).start();

        Log.d(TrackThatThing.TAG, "got this location: " + location.toString());
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
                Log.d(TrackThatThing.TAG,
                        "Got the following response from the server: "
                                + json.getString("msg"));
                SharedPreferences settings = getSharedPreferences(TrackThatThing.PREFS_NAME,
                        android.content.Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = settings.edit();

                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss a yyyy-MM-dd");
                Calendar cal = Calendar.getInstance();

                editor.putString(TrackThatThing.PREF_LAST_LOC_TIME,
                        sdf.format(cal.getTime()));
                editor.apply();

                Intent i = new Intent(TrackThatThing.IF_LOC_UPDATE);
                LocalBroadcastManager.getInstance(MyLocationService.this).sendBroadcast(i);

//                mHandler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        mYesTrackingFragment.updateLastLoc(getApplicationContext(), getWindow().getDecorView().getRootView());
//                    }
//t                });
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
}
