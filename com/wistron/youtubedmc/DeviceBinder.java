package com.wistron.youtubedmc;

import java.util.ArrayList;

import android.os.Binder;
import android.util.Log;

public class DeviceBinder extends Binder {
	//
	private ArrayList<DeviceDisplay> DeviceList;
	
	
	
	protected void SetDeviceList(ArrayList<DeviceDisplay> d)
	{	
		DeviceList = d;
	}
	
	
	protected ArrayList<DeviceDisplay> GetDeviceList()
	{
	 	   Log.i("aaaa", "GetDeviceList-2");
		return DeviceList;
	}
}
