package com.mgalgs.trackthatthing;

import android.app.Activity;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;


public class TrackSomeoneActivity extends Activity
        implements OnMapReadyCallback {

    private final Handler mHandler = new Handler();
    private boolean mUpdating;
    private String mSecret;
    private final long UPDATE_DELAY_MS = 5000;
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_someone);

        Bundle b = getIntent().getExtras();
        mSecret = b.getString("secret");
        if (mSecret == null || mSecret.isEmpty()) {
            Log.e(TrackThatThing.TAG, "Invalid secret!!!");
            Toast.makeText(this, "Invalid secret provided!", Toast.LENGTH_LONG);
            finish();
            return;
        }

        Button btn = (Button) findViewById(R.id.btn_apply_someones_secret);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                EditText editText = (EditText) findViewById(R.id.txt_someones_secret);
                String someones_secret = editText.getText().toString();
                Log.d(TrackThatThing.TAG, "Have text: " + someones_secret);
                mSecret = someones_secret;
            }
        });
        EditText text = (EditText) findViewById(R.id.txt_someones_secret);
        text.setText(mSecret);
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
        Log.d(TrackThatThing.TAG, "onMapReady has map: " + mMap.toString());
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
        String txt = "Putting location on map: " + location.toString();
        Log.d(TrackThatThing.TAG, txt);
        Toast.makeText(this, txt, Toast.LENGTH_SHORT);

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        mMap.setMyLocationEnabled(true);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
        mMap.addMarker(new MarkerOptions()
            .title("Location update")
            .position(latLng));
    }
}
