package com.mgalgs.trackthatthing;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.media.RingtoneManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FriendProximityService extends Service
    implements GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final long CHECK_FOR_FRIEND_PROXIMITY_S = 20;
    public static boolean isRunning;
    private final ScheduledExecutorService mScheduler =
            Executors.newScheduledThreadPool(1);
    private double mFriendDistanceNotificationThreshold = 804; // ~1/2 mile


    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();

    private LatLng mFriendLastLatLng;
    private GoogleApiClient mGoogleApiClient;
    private Location mMyLastLocation;


    private NotificationManager mNM;

    // Unique Identification Number for the Notification.
    // We use it on Notification start, and to cancel it.
    private int NOTIFICATION = R.string.friend_approaching;

    public FriendProximityService() {
    }

    public void updateFriendLocation(Location location) {
        mFriendLastLatLng = new LatLng(location.getLatitude(), location.getLongitude());
    }

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        FriendProximityService getService() {
            return FriendProximityService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TrackThatThing.TAG, "FriendProximityService->onCreate");

        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        buildGoogleApiClient();
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TrackThatThing.TAG, "FriendProximityService->onStartCommand");
        FriendProximityService.isRunning = true;
        mGoogleApiClient.connect();
        mScheduler.scheduleWithFixedDelay(mCheckForProximityTask, 5,
                CHECK_FOR_FRIEND_PROXIMITY_S, TimeUnit.SECONDS);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TrackThatThing.TAG, "FriendProximityService->onDestroy");
        mScheduler.shutdownNow();

        if (mGoogleApiClient.isConnected())
            mGoogleApiClient.disconnect();

        FriendProximityService.isRunning = false;
        super.onDestroy();
    }

    @Override
    public void onConnected(Bundle bundle) {
        updateMyLocation();
    }

    @Override
    public void onConnectionSuspended(int i) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TrackThatThing.TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onDisconnected() {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TrackThatThing.TAG, "FriendProximityService->onConnectionFailed. Maps is busted :(");
    }

    private void updateMyLocation() {
        mMyLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
    }

    private final Runnable mCheckForProximityTask = new Runnable() {
        @Override
        public void run() {
            doCheckForProximity();
        }
    };

    private void doCheckForProximity() {
        if (mFriendLastLatLng == null) {
            Log.e(TrackThatThing.TAG, "Don't have a friend('s location) :(");
            return;
        }
        if (mMyLastLocation == null) {
            Log.e(TrackThatThing.TAG, "Don't have my own location :(");
            return;
        }

        double metersToFriend = com.google.maps.android.SphericalUtil.computeDistanceBetween(
                mFriendLastLatLng,
                new LatLng(mMyLastLocation.getLatitude(), mMyLastLocation.getLongitude()));

        if (metersToFriend < mFriendDistanceNotificationThreshold) {
            notifyProximity();
            stopSelf();
        } else {
            Log.d(TrackThatThing.TAG,
                    "Friend not close enough to notify of proximity (" +
                            String.valueOf(metersToFriend) +
                            " meters away, threshold = " +
                            String.valueOf(mFriendDistanceNotificationThreshold) + ")");
        }
        updateMyLocation();
    }

    private void notifyProximity() {
        String text = "Your friend is < 1/2 mile away!";
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_ttt_icon_alpha)
                .setContentTitle(getString(R.string.friend_approaching))
                .setContentText(text)
                .setVibrate(new long[]{0, 200, 200, 200, 200, 200, 200, 200})
                .setDefaults(Notification.DEFAULT_SOUND|Notification.DEFAULT_LIGHTS)
                .setOnlyAlertOnce(true)
                .build();
        mNM.notify(NOTIFICATION, notification);
    }
}
