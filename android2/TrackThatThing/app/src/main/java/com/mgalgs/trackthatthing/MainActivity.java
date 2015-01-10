package com.mgalgs.trackthatthing;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;


public class MainActivity extends Activity
        implements GetTrackingCodeDialogFragment.TrackingCodeSelectListener {
    public static final int ACTIVITY_RESULT_GET_SECRET = 0;
    public static final int ACTIVITY_RESULT_JUS_GET_SECRET_STRING = 1;
    /*
     * Define a request code to send to Google Play services
     * This code is returned in Activity.onActivityResult
     */
    private static final String DIALOG_ERROR = "dialog_error";
    private boolean mResolvingError = false;
    private static final String STATE_RESOLVING_ERROR = "resolving_error";
    private static final String STATE_TRACKING = "tracking";
    private static final int REQUEST_RESOLVE_ERROR = 1001;

    private boolean mTracking;
    private String mSecretCode;

    Handler mHandler = new Handler();

    private NotTrackingFragment mNotTrackingFragment = new NotTrackingFragment();
    private YesTrackingFragment mYesTrackingFragment = new YesTrackingFragment();
    private MenuItem mTrackMenuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_tracking);

        mTracking = (savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_TRACKING, false))
                || MyLocationService.tracking;

        Log.d(TrackThatThing.TAG, "onCreate, mTracking = " + (new Boolean(mTracking)).toString());

        if (mTracking)
            startTracking();
        else
            stopTracking();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mTracking)
            mYesTrackingFragment.updateLastLoc(this);
        LocalBroadcastManager.getInstance(this).registerReceiver(mLocUpdateReceiver, new IntentFilter(TrackThatThing.IF_LOC_UPDATE));
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mLocUpdateReceiver);
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateActionBar();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
        outState.putBoolean(STATE_TRACKING, mTracking);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mTrackMenuItem = menu.findItem(R.id.action_track);
        updateActionBar();
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
            case R.id.action_share:
                share();
                return true;
            case R.id.action_track_someone:
                (new GetTrackingCodeDialogFragment()).show(getFragmentManager(), "get tracking code");
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case ACTIVITY_RESULT_GET_SECRET:
                if (resultCode == RESULT_OK)
                    startTracking();
                break;
            // these should really be consolidated... oh well...
            case ACTIVITY_RESULT_JUS_GET_SECRET_STRING:
                if (resultCode == RESULT_OK) {
                    Intent i = new Intent(this, TrackSomeoneActivity.class);
                    i.putExtra("secret", data.getStringExtra("secret"));
                    startActivity(i);
                }
                break;
        }
    }

    @Override
    public void onFinishTrackingCodeSelect(String trackingCode) {
        Intent i = new Intent(this, TrackSomeoneActivity.class);
        i.putExtra("secret", trackingCode);
        startActivity(i);
    }

    public static class NotTrackingFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            // Inflate the layout for this fragment
            return inflater.inflate(R.layout.fragment_layout_not_tracking, container, false);
        }
    }

    public static class YesTrackingFragment extends Fragment {
        private View mView;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            // Inflate the layout for this fragment
            mView = inflater.inflate(R.layout.fragment_layout_yes_tracking, container, false);

            MainActivity mainActivity = (MainActivity) getActivity();
            updateSecretCode(mainActivity.mSecretCode);
            updateLastLoc(mainActivity.getApplicationContext());

            return mView;
        }

        public void updateSecretCode(String secretCode) {
            // update the secret code text view
            TextView tv = (TextView) mView.findViewById(R.id.tv_with_code);
            tv.setText(getString(R.string.with_code) + " " + secretCode);
        }

        public void updateLastLoc(Context context) {
            SharedPreferences settings = context.getSharedPreferences(TrackThatThing.PREFS_NAME,
                    android.content.Context.MODE_PRIVATE);
            String last = settings.getString(TrackThatThing.PREF_LAST_LOC_TIME, "a long time ago...");
            if (mView == null) {
                Log.d(TrackThatThing.TAG, "We don't have an mView yet. Not updating last loc tv.");
                return;
            }
            TextView tv = (TextView) mView.findViewById(R.id.tv_last_update);
            if (tv != null)
                tv.setText(context.getString(R.string.last_update) + " " + last);
            else
                Log.d(TrackThatThing.TAG, "tv is null.  not setting last loc tv");
        }
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
        // null-check needed since this can be called before
        // onCreateOptionsMenu
        if (mTrackMenuItem != null)
            mTrackMenuItem.setIcon(R.drawable.ic_action_location_found);
    }

    private void updateActionBar() {
        if (mTrackMenuItem != null) {
            if (mTracking)
                mTrackMenuItem.setIcon(R.drawable.ic_action_location_found);
            else
                mTrackMenuItem.setIcon(R.drawable.ic_action_location_searching);
        }
    }

    private void share()
    {
        String subject = "See where I'm at in real-time!";
        String bodyText = "http://www.trackthatthing.com";
        try {
            bodyText = String
                    .format("Hey! I'm using TrackThatThing "
                                    + "to track my location. Check out the real-time map of my location "
                                    + "here: http://www.trackthatthing.com/live?secret=%s",
                            URLEncoder.encode(mSecretCode, "ascii"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        final Intent theIntent = new Intent(android.content.Intent.ACTION_SEND);
        theIntent.setType("text/plain");
        theIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
        theIntent.putExtra(android.content.Intent.EXTRA_TEXT, bodyText);
        startActivity(Intent.createChooser(theIntent, "Send Location"));
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

    private Intent mLocationServiceIntent;

    private void startTracking() {

        SharedPreferences settings = getSharedPreferences(TrackThatThing.PREFS_NAME, MODE_PRIVATE);
        mSecretCode = settings.getString(TrackThatThing.PREF_SECRET_CODE, null);

        if (mSecretCode == null) {
            launchSecretGetter();
            return;
        }

        // we always need the Intent so that we can stop the service later
        // (it might already be started)
        mLocationServiceIntent = new Intent(MainActivity.this, MyLocationService.class);
        if (!MyLocationService.tracking) {
            MainActivity.this.startService(mLocationServiceIntent);
        }

        mTracking = true;
        UI_yesTracking();
        updateActionBar();
    }

    private void stopTracking() {
        if (mLocationServiceIntent != null) {
            stopService(mLocationServiceIntent);
            mLocationServiceIntent = null;
        }
        mTracking = false;
        UI_notTracking();
        updateActionBar();
    }

    /* Called from ErrorDialogFragment when the dialog is dismissed. */
    public void onDialogDismissed() {
        mResolvingError = false;
    }

    private BroadcastReceiver mLocUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(TrackThatThing.IF_LOC_UPDATE)) {
                mYesTrackingFragment.updateLastLoc(context);
            } else {
                Log.d(TrackThatThing.TAG, "UNKNOWN INTENT RECEIVED -- WHAT THE?!?!");
            }
        }
    };
}
