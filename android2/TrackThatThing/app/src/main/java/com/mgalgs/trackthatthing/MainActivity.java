package com.mgalgs.trackthatthing;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;


public class MainActivity extends Activity {
    public static final int ACTIVITY_RESULT_GET_SECRET = 0;

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
            return inflater.inflate(R.layout.fragment_layout_yes_tracking, container, false);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_tracking);
        mTracking = false;
        UI_notTracking();
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
            case R.id.action_settings:
                openSettings();
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
        SharedPreferences settings = getSharedPreferences(TrackThatThing.PREFS_NAME, MODE_PRIVATE);
        mSecretCode = settings.getString(TrackThatThing.PREF_SECRET_CODE, null);

        if (mSecretCode == null) {
            launchSecretGetter();
            return;
        }

        mTracking = true;
        UI_yesTracking();
    }

    private void openSettings() {
        Toast.makeText(getApplicationContext(), "openSettings()", Toast.LENGTH_SHORT).show();
    }
}
