package com.wistron.youtubedmc;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

public class TransparentActivity extends Activity {

    private static final String TAG = "TransparentActivity";
    protected static final String BCAST_COMPLETE = "com.wistron.youtubedmc.COMPLETE"; 
    protected static final String BCAST_URL = "com.wistron.youtubedmc.URL"; 
    private String URL = null;


    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String ac = intent.getAction();
            if (ac.equals(BCAST_COMPLETE)) {
                sendYoutubeUrl(URL);
            }
        }
    };
    
    private void initYoutubeExtras(){
        String ac = getIntent().getAction();
        if(ac.equals(Intent.ACTION_SEND)){
            Bundle extras = getIntent().getExtras();
            if( null != extras){
                if( extras.containsKey(Intent.EXTRA_TEXT)){
                    URL = extras.getString(Intent.EXTRA_TEXT);
                }
                if (!isUpnpScanServiceRunning()) {
                    Intent startServiceIntent = new Intent(this, UpnpScanService.class);
                    startService(startServiceIntent);
                    Log.d(TAG, "Start upnp service.....");
                }else{
                    sendYoutubeUrl(URL);
                }
            }
        }
    }

    private boolean isUpnpScanServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager
                .getRunningServices(Integer.MAX_VALUE)) {
            if (UpnpScanService.class.getName().equals(
                    service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    
    private void sendYoutubeUrl(String url){
        Intent i = new Intent(BCAST_URL);
        Bundle bundle = new Bundle();
        bundle.putString("url", url);
        i.putExtras(bundle);
        sendBroadcast(i);  
        Log.d(TAG, "sendYoutubeUrl: "+url);
        finish();
    }
    

    
    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        //Register upnp service complete broadcast action
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BCAST_COMPLETE);
        registerReceiver(mBroadcastReceiver, intentFilter);
        
        initYoutubeExtras();
    }
    
    @Override
    protected void onDestroy() {
        if(mBroadcastReceiver!=null){
            unregisterReceiver(mBroadcastReceiver);
            mBroadcastReceiver = null;
        }
        Log.i(TAG, "onDestroy() ");
        super.onDestroy();
    }              
}
