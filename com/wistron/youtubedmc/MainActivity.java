package com.wistron.youtubedmc;

import java.util.ArrayList;

import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.os.Bundle;
import android.app.ListActivity;
import android.content.Intent;

public class MainActivity extends ListActivity {    
    protected static final String TAG = "MainActivity"; 
    protected static final String BCAST_SEND_POSITION = "com.wistron.youtubedmc.SEND_POSITION"; 
    private ArrayAdapter<DeviceDisplay> listAdapter;
    private ArrayList<DeviceDisplay> mDevice;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        
        listAdapter = new ArrayAdapter<DeviceDisplay>(this,
                android.R.layout.simple_list_item_1);
        setListAdapter(listAdapter);
        
        if(listAdapter!=null){
            listAdapter.clear();
        }

        Intent intent = getIntent();
        if (intent.getExtras() != null) {
            mDevice = ((DeviceParcelable) this.getIntent().getExtras()
                    .getParcelable("devicelist")).GetDeviceList();
            for (DeviceDisplay dd : mDevice) {
                listAdapter.add(dd);
                Log.d(TAG, "add device =>" + dd.toString());
            }
        }
        org.seamless.util.logging.LoggingUtil.resetRootHandler();

    }        

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Intent i = new Intent(BCAST_SEND_POSITION);
        Bundle bundle = new Bundle();
        bundle.putInt("position", position);
        i.putExtras(bundle);
        sendBroadcast(i);    
        finish();
    }

}
