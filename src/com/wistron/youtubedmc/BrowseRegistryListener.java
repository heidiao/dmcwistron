package com.wistron.youtubedmc;

import java.util.ArrayList;

import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.registry.DefaultRegistryListener;
import org.fourthline.cling.registry.Registry;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

class BrowseRegistryListener extends DefaultRegistryListener {

    ArrayList<DeviceDisplay> mDevice = new ArrayList<DeviceDisplay>();
    protected static final String BCAST_UPDATE_DEVICE = "com.wistron.youtubedmc.UPDATE_DEVICE"; 
    private static final String TAG = "BrowseRegistryListener";
    private static final String MATCH_NAME = "WiRenderer";
    private Context mContext;
    BrowseRegistryListener(Context context){
        mContext = context;
    }
    /* Discovery performance optimization for very slow Android devices! */
    @Override
    public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice device) {
        deviceAdded(device);
    }

    @Override
    public void remoteDeviceDiscoveryFailed(Registry registry, final RemoteDevice device, final Exception ex) {
        Log.d(TAG, "Discovery failed of '" + device.getDisplayString() + "': "+ex.toString());
        deviceRemoved(device);
    }
    /* End of optimization, you can remove the whole block if your Android handset is fast (>= 600 Mhz) */

    @Override
    public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
        deviceAdded(device);
    }

    @Override
    public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
        deviceRemoved(device);
    }

    @Override
    public void localDeviceAdded(Registry registry, LocalDevice device) {
        deviceAdded(device);
    }

    @Override
    public void localDeviceRemoved(Registry registry, LocalDevice device) {
        deviceRemoved(device);
    }

    private int getPosition(DeviceDisplay d){
    	int position=-1;
        int i=0;
   	 	for(DeviceDisplay dd : mDevice){
            if(dd.toString().equals(d.toString())){
			     position = i;
			 }
			 i++;
		 }     
   	 	return position;
    }
    
    public void deviceAdded(final Device device) {
    	 DeviceDisplay d = new DeviceDisplay(device);
         if(d.toString().startsWith(MATCH_NAME) && !d.toString().endsWith("*")){
             int position = getPosition(d);
             if (position >= 0) {                
             	mDevice.set(position, d);
                Log.d(TAG, "update ... [ "+d.toString()+" ]");
             } else {
              	mDevice.add(d);
                Log.d(TAG, "add ... [ "+d.toString()+" ]");
             }
         }
         Intent i = new Intent(BCAST_UPDATE_DEVICE);
         mContext.sendBroadcast(i);    
    }


	public void deviceRemoved(final Device device) {
	    DeviceDisplay d = new DeviceDisplay(device);
		mDevice.remove(d);
        Log.d(TAG, "removed ...[ "+d.toString()+" ]");
        Intent i = new Intent(BCAST_UPDATE_DEVICE);
        mContext.sendBroadcast(i);    
    }
	

    public  ArrayList<DeviceDisplay> getUpnpDevice(){
		return mDevice;
	}
	
}