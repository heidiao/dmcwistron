package com.wistron.youtubedmc;

import java.util.ArrayList;

import org.fourthline.cling.android.AndroidUpnpServiceImpl;

import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.os.Bundle;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class MainActivity extends ListActivity {    
    protected static final String TAG = "MainActivity"; 
    protected static final String BCAST_SEND_POSITION = "com.wistron.youtubedmc.SEND_POSITION"; 
    protected static final String BCAST_UPDATE_DEVICE = "com.wistron.youtubedmc.UPDATE_DEVICE"; 
    private ArrayAdapter<DeviceDisplay> listAdapter;
    private ArrayList<DeviceDisplay> mDevice;

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String ac = intent.getAction();
            Log.i(TAG, "onReceive:" + ac);
            
            if (ac.equals(BCAST_UPDATE_DEVICE)) {
                doUpdate();
            } 
        }
    };
    
    private void doUpdate(){        
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
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        
        listAdapter = new ArrayAdapter<DeviceDisplay>(this,
                android.R.layout.simple_list_item_1);
        setListAdapter(listAdapter);
        doUpdate();
        org.seamless.util.logging.LoggingUtil.resetRootHandler();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BCAST_UPDATE_DEVICE);
        registerReceiver(mBroadcastReceiver, intentFilter);
    }        

    
    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy()");
        super.onDestroy();
        if(mBroadcastReceiver!=null){
            unregisterReceiver(mBroadcastReceiver);
            mBroadcastReceiver = null;
        }
    }

}
