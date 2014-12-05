package com.mgalgs.trackthatthing;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;


public class MainActivity extends Activity implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        LocationListener {
    public static final int ACTIVITY_RESULT_GET_SECRET = 0;
    /*
     * Define a request code to send to Google Play services
     * This code is returned in Activity.onActivityResult
     */
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    private static final String DIALOG_ERROR = "dialog_error";
    private boolean mResolvingError = false;
    private static final String STATE_RESOLVING_ERROR = "resolving_error";
    private LocationClient mLocationClient;
    LocationRequest mLocationRequest;

    Handler mHandler = new Handler();


    // Milliseconds per second
    private static final int MILLISECONDS_PER_SECOND = 1000;
    // Update frequency in seconds
    public static final int UPDATE_INTERVAL_IN_SECONDS = 5;
    // Update frequency in milliseconds
    private static final long UPDATE_INTERVAL =
            MILLISECONDS_PER_SECOND * UPDATE_INTERVAL_IN_SECONDS;
    // The fastest update frequency, in seconds
    private static final int FASTEST_INTERVAL_IN_SECONDS = 3;
    // A fast frequency ceiling in milliseconds
    private static final long FASTEST_INTERVAL =
            MILLISECONDS_PER_SECOND * FASTEST_INTERVAL_IN_SECONDS;

    private NotTrackingFragment mNotTrackingFragment = new NotTrackingFragment();
    private YesTrackingFragment mYesTrackingFragment = new YesTrackingFragment();


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
    }

    private boolean mTracking;
    private String mSecretCode;

    public static class NotTrackingFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            // Inflate the layout for this fragment
            return inflater.inflate(R.layout.fragment_layout_not_tracking, container, false);
        }
    }

    public static class YesTrackingFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            // Inflate the layout for this fragment
            View rootView = inflater.inflate(R.layout.fragment_layout_yes_tracking, container, false);

            MainActivity mainActivity = (MainActivity) getActivity();
            updateSecretCode(mainActivity.mSecretCode, rootView);
            updateLastLoc(mainActivity.getApplicationContext(), rootView);

            return rootView;
        }

        public void updateSecretCode(String secretCode, View rootView) {
            // update the secret code text view
            TextView tv = (TextView) rootView.findViewById(R.id.tv_with_code);
            tv.setText(getString(R.string.with_code) + " " + secretCode);
        }

        public void updateLastLoc(Context context, View view) {
            SharedPreferences settings = context.getSharedPreferences(TrackThatThing.PREFS_NAME,
                    android.content.Context.MODE_PRIVATE);
            String last = settings.getString(TrackThatThing.PREF_LAST_LOC_TIME, "a long time ago...");
            TextView tv = (TextView) view.findViewById(R.id.tv_last_update);
            tv.setText(getString(R.string.last_update) + " " + last);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_tracking);
        mTracking = false;
        UI_notTracking();

        mLocationClient = new LocationClient(this, this, this);

        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create();
        // Use high accuracy
        mLocationRequest.setPriority(
                LocationRequest.PRIORITY_HIGH_ACCURACY);
        // Set the update interval to 5 seconds
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        // Set the fastest update interval to 1 second
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);

        mResolvingError = savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);
    }

    /**
     * Verify that Google Play services is available before making a request.
     *
     * @return true if Google Play services is available, otherwise false
     */
    private boolean servicesConnected() {

        // Check that Google Play services is available
        int resultCode =
                GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // Continue
            Log.d(TrackThatThing.TAG, "Have google play services!");
            return true;
            // Google Play services was not available for some reason
        } else {
            // Display an error dialog
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this, 0);
            if (dialog != null) {
                showErrorDialog(resultCode);
            }
            return false;
        }
    }

    /* Creates a dialog for an error message */
    private void showErrorDialog(int errorCode) {
        // Create a fragment for the error dialog
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.show(getFragmentManager(), "errordialog");
    }

    /**
     * Define a DialogFragment to display the error dialog generated in
     * showErrorDialog.
     */
    /* A fragment to display an error dialog */
    public static class ErrorDialogFragment extends DialogFragment {
        public ErrorDialogFragment() {
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return GooglePlayServicesUtil.getErrorDialog(errorCode,
                    this.getActivity(), REQUEST_RESOLVE_ERROR);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            ((MainActivity) getActivity()).onDialogDismissed();
        }
    }


    private void UI_notTracking() {
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.tracking_fragment_container, mNotTrackingFragment);
        fragmentTransaction.commit();
    }

    private void UI_yesTracking() {
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.tracking_fragment_container, mYesTrackingFragment);
        fragmentTransaction.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case R.id.action_track:
                toggleTracking();
                return true;
            case R.id.action_secret:
                launchSecretGetter();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case ACTIVITY_RESULT_GET_SECRET:
                startTracking();
                break;
            case REQUEST_RESOLVE_ERROR:
                mResolvingError = false;
                if (resultCode == RESULT_OK) {
                    // Make sure the app is not already connected or attempting to connect
                    if (!mLocationClient.isConnecting() &&
                            !mLocationClient.isConnected()) {
                        mLocationClient.connect();
                    }
                }
                break;
        }
    }

    public void launchSecretGetter() {
        Intent i = new Intent(this, TheSecretGetter.class);
        startActivityForResult(i, ACTIVITY_RESULT_GET_SECRET);
    }

    private void toggleTracking() {
        if (mTracking)
            stopTracking();
        else
            startTracking();
    }

    private void stopTracking() {
        mTracking = false;
        UI_notTracking();
    }

    private void startTracking() {
        if (!servicesConnected())
            return;

        SharedPreferences settings = getSharedPreferences(TrackThatThing.PREFS_NAME, MODE_PRIVATE);
        mSecretCode = settings.getString(TrackThatThing.PREF_SECRET_CODE, null);

        if (mSecretCode == null) {
            launchSecretGetter();
            return;
        }

        mLocationClient.connect();
        mTracking = true;
        UI_yesTracking();
    }


    /*
     * Called by Location Services when the request to connect the
     * client finishes successfully. At this point, you can
    * request the current location or start periodic updates
    */
    @Override
    public void onConnected(Bundle dataBundle) {
        // Display the connection status
        Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
        mLocationClient.requestLocationUpdates(mLocationRequest, this);
    }

    /*
     * Called by Location Services if the connection to the
     * location client drops because of an error.
     */
    @Override
    public void onDisconnected() {
        // Display the connection status
        Toast.makeText(this, "Disconnected. Please re-connect.",
                Toast.LENGTH_SHORT).show();
    }

    /*
     * Called by Location Services if the attempt to
     * Location Services fails.
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (result.hasResolution()) {
            try {
                mResolvingError = true;
                result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mLocationClient.connect();
            }
        } else {
            // Show dialog using GooglePlayServicesUtil.getErrorDialog()
            showErrorDialog(result.getErrorCode());
            mResolvingError = true;
        }
    }

    @Override
    protected void onStop() {
        if (mLocationClient.isConnected()) {
            mLocationClient.removeLocationUpdates(this);
        }
        mLocationClient.disconnect();
        super.onStop();
    }

    /* Called from ErrorDialogFragment when the dialog is dismissed. */
    public void onDialogDismissed() {
        mResolvingError = false;
    }

    @Override
    public void onLocationChanged(Location location) {
        // Report to the UI that the location was updated
        String msg = "Updated Location: " +
                Double.toString(location.getLatitude()) + "," +
                Double.toString(location.getLongitude());
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        Log.d(TrackThatThing.TAG, msg);

        SharedPreferences settings = getSharedPreferences(TrackThatThing.PREFS_NAME, MODE_PRIVATE);

        long lastLocTime = SystemClock.elapsedRealtime();

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
                Log.i(TrackThatThing.TAG,
                        "Got the following response from the server: "
                                + json.getString("msg"));
                SharedPreferences settings = getSharedPreferences(TrackThatThing.PREFS_NAME,
                        android.content.Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = settings.edit();

                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss a yyyy-MM-dd");
                Calendar cal = Calendar.getInstance();

                editor.putString(TrackThatThing.PREF_LAST_LOC_TIME,
                        sdf.format(cal.getTime()));
                editor.commit();

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mYesTrackingFragment.updateLastLoc(getApplicationContext(), getWindow().getDecorView().getRootView());
                    }
                });
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
