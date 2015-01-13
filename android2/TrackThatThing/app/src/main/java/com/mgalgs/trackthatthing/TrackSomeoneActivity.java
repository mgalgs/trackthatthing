package com.mgalgs.trackthatthing;

import android.app.Activity;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;


public class TrackSomeoneActivity extends Activity
        implements OnMapReadyCallback {

    private final Handler mHandler = new Handler();
    private boolean mUpdating;
    private String mSecret;
    private final long UPDATE_DELAY_MS = 5000;
    private GoogleMap mMap;
    private boolean mZoomedOnce;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_loading);
        Log.d(TrackThatThing.TAG, "TrackSomeoneActivity->onCreate()");
        // final ProgressDialog dlg = ProgressDialog.show(this, "Loading", "Please wait...");

        // this little handler just buys us enough time to render the skeleton
        // "loading" layout before spinning up the heavyweight layout_track_someone_else
        // there should be a better way of doing this (inflating the heavyweight layout
        // on a background thread (i.e. actually render it off the ui thread somehow?))
        (new Handler(Looper.getMainLooper())).postDelayed(new Runnable() {
            @Override
            public void run() {
                setContentView(R.layout.layout_track_someone_else);
                Log.d(TrackThatThing.TAG, "layout_track_someone_else loaded!");
                // dlg.dismiss();

                MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.the_map);
                if (mapFragment == null) {
                    Log.e(TrackThatThing.TAG, "mapFragment is null!!!!");
                }
                mapFragment.getMapAsync(TrackSomeoneActivity.this);
            }
        }, 1000);

        // NOTE:
        // don't do anything with things in layout_track_someone_else below here
        // since we load it in the handler above

        String url = getIntent().getDataString();

        if (url != null) {
            // we're handling a /live?secret= URL
            try {
                url = URLDecoder.decode(url, "UTF-8");
                mSecret = url.split("=")[1];
                Log.e(TrackThatThing.TAG, "Decoded secret: " + mSecret);
            } catch (UnsupportedEncodingException e) {
                Log.e(TrackThatThing.TAG, "Couldn't decode URL due to unsupported encoding: " + url);
                e.printStackTrace();
            }
        } else {
            // we were started with startActivity
            mSecret = getIntent().getStringExtra("secret");
        }

        if (mSecret == null || mSecret.isEmpty()) {
            Log.e(TrackThatThing.TAG, "Invalid secret!!!");
            Toast.makeText(this, getString(R.string.invalid_secret), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setTitle(getString(R.string.tracking) + mSecret);
        TrackThatThingDB trackThatThingDB = new TrackThatThingDB(this);
        if (trackThatThingDB.addTrackingCode(mSecret) == -1)
            trackThatThingDB.updateTrackingCodeLastUse(mSecret);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_track_someone, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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
            if (json == null) {
                Log.e(TrackThatThing.TAG, "Couldn't connect to " + url);
                return null;
            }
            double longitude, latitude;
            try {
                JSONObject data = json.getJSONObject("data");
                JSONArray locations = data.getJSONArray("locations");
                JSONObject first = locations.getJSONObject(0);
                longitude = first.getDouble("longitude");
                latitude = first.getDouble("latitude");
            } catch (JSONException e) {
                Log.e(TrackThatThing.TAG, "Error retrieving data from server.  Got: " + json.toString());
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
            Log.i(TrackThatThing.TAG, "mSecret is null.  not requesting location.");
            return;
        }
        QueryString qs = new QueryString(TrackThatThing.BASE_URL + "/get");
        qs.add("secret", mSecret);
        new GetUpdatesTask().execute(qs.toString());
    }

    private void putLocationOnMap(Location location) {
        if (mMap == null) {
            Log.i(TrackThatThing.TAG, "Map not ready yet.  Not drawing point: " + location.toString());
            return;
        }

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        mMap.setMyLocationEnabled(true);
        if (!mZoomedOnce) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
            mZoomedOnce = true;
        } else {
            // just recenter
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        }
        mMap.addMarker(new MarkerOptions()
            .title(getString(R.string.location_update))
            .position(latLng));
    }
}
