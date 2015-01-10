package com.mgalgs.trackthatthing;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class GetTrackingCodeDialogFragment extends DialogFragment {
    private Cursor mCursor;
    private TrackThatThingDB mTrackThatThingDB;
    private SimpleCursorAdapter mSimpleCursorAdapter;

    public GetTrackingCodeDialogFragment() {}

    public interface TrackingCodeSelectListener {
        void onFinishTrackingCodeSelect(String trackingCode);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mTrackThatThingDB = new TrackThatThingDB(getActivity());
        final Activity activity = getActivity();
        LayoutInflater inflater = activity.getLayoutInflater();
        final View getTrackingCodeView = inflater.inflate(R.layout.get_tracking_code, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage("Choose a tracking code")
                .setView(getTrackingCodeView);
        mCursor = mTrackThatThingDB.getTrackingCodesCursor();
        mSimpleCursorAdapter = new SimpleCursorAdapter(
                activity, R.layout.lv_item_tracking_code, mCursor,
                new String[]{ TrackThatThingDB.TrackingCodeEntry.COLUMN_NAME_TRACKING_CODE,
                              TrackThatThingDB.TrackingCodeEntry.COLUMN_NAME_LAST_USE },
                new int[]{R.id.tv_lv_item_tracking_code_code, R.id.tv_lv_item_tracking_code_last_used},
                0);

        ListView listView = (ListView) getTrackingCodeView.findViewById(R.id.lv_tracking_codes);
        listView.setAdapter(mSimpleCursorAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String trackingCode = getTrackingCodeAtCursorPos(position);
                mTrackThatThingDB.updateTrackingCodeLastUse(trackingCode);
                TrackingCodeSelectListener activity = (TrackingCodeSelectListener) getActivity();
                activity.onFinishTrackingCodeSelect(trackingCode);
                GetTrackingCodeDialogFragment.this.dismiss();
            }
        });

        registerForContextMenu(listView);

        Button button = (Button) getTrackingCodeView.findViewById(R.id.btn_jus_ok_secret);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editText = (EditText) getTrackingCodeView.findViewById(R.id.et_jus_secret);
                String trackingCode = editText.getText().toString();
                mTrackThatThingDB.addTrackingCode(trackingCode);
                    GetTrackingCodeDialogFragment.this.dismiss();
                    TrackingCodeSelectListener activity = (TrackingCodeSelectListener) getActivity();
                    activity.onFinishTrackingCodeSelect(trackingCode);
            }
        });

        return builder.create();
    }

    private String getTrackingCodeAtCursorPos(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getString(mCursor.getColumnIndexOrThrow(
                TrackThatThingDB.TrackingCodeEntry.COLUMN_NAME_TRACKING_CODE));
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId() == R.id.lv_tracking_codes) {
            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.menu_tracking_codes_actions, menu);

            // glorious hack from http://stackoverflow.com/a/18853634/209050
            // (without this, onContextItemSelected is never called for some reason...)
            MenuItem.OnMenuItemClickListener listener = new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    onContextItemSelected(item);
                    return true;
                }
            };

            for (int i = 0, n = menu.size(); i < n; i++)
                menu.getItem(i).setOnMenuItemClickListener(listener);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.menu_item_tracking_code_delete:
                mTrackThatThingDB.deleteTrackingCode(getTrackingCodeAtCursorPos(info.position));
                mCursor = mTrackThatThingDB.getTrackingCodesCursor();
                mSimpleCursorAdapter.changeCursor(mCursor);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }
}
