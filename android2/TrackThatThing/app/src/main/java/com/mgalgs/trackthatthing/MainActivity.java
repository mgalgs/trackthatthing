package com.mgalgs.trackthatthing;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;


public class MainActivity extends Activity {
    public static final int ACTIVITY_RESULT_GET_SECRET = 0;
    /*
     * Define a request code to send to Google Play services
     * This code is returned in Activity.onActivityResult
     */
    public final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

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

            return rootView;
        }

        public void updateSecretCode(String secretCode, View rootView) {
            // update the secret code text view
            TextView tv = (TextView) rootView.findViewById(R.id.tv_with_code);
            tv.setText(getString(R.string.with_code) + " " + secretCode);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_tracking);
        mTracking = false;
        UI_notTracking();
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
                ErrorDialogFragment errorFragment = new ErrorDialogFragment();
                errorFragment.setDialog(dialog);
                errorFragment.show(getFragmentManager(), "Location Updates");
            }
            return false;
        }
    }

    /**
     * Define a DialogFragment to display the error dialog generated in
     * showErrorDialog.
     */
    public static class ErrorDialogFragment extends DialogFragment {

        // Global field to contain the error dialog
        private Dialog mDialog;

        /**
         * Default constructor. Sets the dialog field to null
         */
        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }

        /**
         * Set the dialog to display
         *
         * @param dialog An error dialog
         */
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }

        /*
         * This method must return a Dialog to the DialogFragment.
         */
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }


    private void UI_notTracking() {
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        NotTrackingFragment ntf = new NotTrackingFragment();
        fragmentTransaction.replace(R.id.tracking_fragment_container, ntf);
        fragmentTransaction.commit();
    }

    private void UI_yesTracking() {
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        YesTrackingFragment ytf = new YesTrackingFragment();
        fragmentTransaction.replace(R.id.tracking_fragment_container, ytf);
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
            case CONNECTION_FAILURE_RESOLUTION_REQUEST :

                switch (resultCode) {
                    // If Google Play services resolved the problem
                    case Activity.RESULT_OK:
                        break;

                    // If any other result was returned by Google Play services
                    default:
                        // Log the result
                        Log.d(TrackThatThing.TAG, "CONNECTION_FAILURE_RESOLUTION_REQUEST didn't return RESULT_OK");
                        break;
                }
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

        mTracking = true;
        UI_yesTracking();
    }
}
