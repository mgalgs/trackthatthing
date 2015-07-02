package com.mgalgs.trackthatthing;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class TrackingBroadcastReceiver extends BroadcastReceiver {
    public TrackingBroadcastReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        switch (action) {
            case "com.mgalgs.trackthatthing.START_TRACKING":
                MyLocationService.locationServiceIntent = new Intent(context, MyLocationService.class);
                context.startService(MyLocationService.locationServiceIntent);
                break;
            case "com.mgalgs.trackthatthing.STOP_TRACKING":
                if (MyLocationService.locationServiceIntent != null) {
                    context.stopService(MyLocationService.locationServiceIntent);
                    MyLocationService.locationServiceIntent = null;
                }
                break;
        }
    }
}
