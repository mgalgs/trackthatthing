package com.mgalgs.trackthatthingtv;

import android.app.Activity;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/*
 * MainActivity class that loads MainFragment
 */
public class TrackSomeoneActivity extends Activity
        implements OnMapReadyCallback {

    private final Handler mHandler = new Handler();
    private GoogleMap mMap;
    private boolean mUpdating;
    private String mSecret;
    private final long UPDATE_DELAY_MS = 5000;

    /**
     * Called when the activity is first created.
     */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_someone);

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.the_map);
        if (mapFragment == null) {
            Log.e(TrackThatThingTV.TAG, "mapFragment is null!!!!");
        }
        mapFragment.getMapAsync(TrackSomeoneActivity.this);

        mSecret = getIntent().getStringExtra("secret");

        if (mSecret == null || mSecret.isEmpty()) {
            Log.e(TrackThatThingTV.TAG, "Invalid secret!!!");
            Toast.makeText(this, getString(R.string.invalid_secret), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setTitle(getString(R.string.tracking) + mSecret);
    }

    @Override
    protected void onStart() {
        super.onStart();

        startLocationUpdates();
    }

    @Override
    protected void onStop() {
        super.onStop();

        stopLocationUpdates();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    private void startLocationUpdates() {
        mUpdating = true;
        doUpdate();
    }

    private void stopLocationUpdates() {
        mUpdating = false;
    }

    private class GetUpdatesTask extends AsyncTask<String, Void, Location> {
        @Override
        protected Location doInBackground(String... urls) {
            String url = urls[0];
            JSONObject json = RestClient.connect(url);
            double longitude, latitude;
            try {
                JSONObject data = json.getJSONObject("data");
                JSONArray locations = data.getJSONArray("locations");
                JSONObject first = locations.getJSONObject(0);
                longitude = first.getDouble("longitude");
                latitude = first.getDouble("latitude");
            } catch (JSONException e) {
                Log.e(TrackThatThingTV.TAG, "Error retrieving data from server.  Got: " + json.toString());
                e.printStackTrace();
                return null;
            }
            Location location = new Location(""); // provider name is unnecessary
            location.setLongitude(longitude);
            location.setLatitude(latitude);
            return location;
        }

        @Override
        protected void onPostExecute(Location location) {
            super.onPostExecute(location);
            if (!mUpdating)
                return;

            if (location == null)
                return;

            putLocationOnMap(location);

            // reschedule ourselves
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    doUpdate();
                }
            }, UPDATE_DELAY_MS);
        }
    }

    private void doUpdate() {
        if (mSecret == null) {
            Log.i(TrackThatThingTV.TAG, "mSecret is null.  not requesting location.");
            return;
        }
        QueryString qs = new QueryString(TrackThatThingTV.BASE_URL + "/get");
        qs.add("secret", mSecret);
        new GetUpdatesTask().execute(qs.toString());
    }

    private void putLocationOnMap(Location location) {
        if (mMap == null) {
            Log.i(TrackThatThingTV.TAG, "Map not ready yet.  Not drawing point: " + location.toString());
            return;
        }

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        mMap.setMyLocationEnabled(true);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
        mMap.addMarker(new MarkerOptions()
                .title(getString(R.string.location_update))
                .position(latLng));
    }
}
