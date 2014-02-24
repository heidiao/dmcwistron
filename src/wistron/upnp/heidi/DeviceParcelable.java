package wistron.upnp.heidi;

import java.util.ArrayList;

import android.os.Binder;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class DeviceParcelable implements Parcelable {

	private DeviceBinder mDeviceBinder;
	
	//
	public DeviceParcelable()
	{
		mDeviceBinder = new DeviceBinder();
	}
	
	public DeviceParcelable(Parcel cParcel)
	{		
		try
		{
			mDeviceBinder = (DeviceBinder)cParcel.readValue(DeviceBinder.class.getClassLoader());	
		}
		catch (Exception exException)
		{
			
		}
	}

	public void SetDeviceList(ArrayList<DeviceDisplay> d)
	{
	
		mDeviceBinder.SetDeviceList(d);
	}//SetWpInfo
	
	
	public ArrayList<DeviceDisplay> GetDeviceList()
	{
 	   Log.i("aaaa", "GetDeviceList");
		ArrayList<DeviceDisplay> cResult = null;
	 	   Log.i("aaaa", "GetDeviceList2:"+cResult);
		
		cResult = mDeviceBinder.GetDeviceList();

	 	   Log.i("aaaa", "GetDeviceList3:"+cResult);
		return cResult;
	}
	
	
	public static final Parcelable.Creator<DeviceParcelable> CREATOR = new Creator<DeviceParcelable>()
	{
		public DeviceParcelable createFromParcel(Parcel cParcel)
		{			
			return new DeviceParcelable(cParcel);
		}//createFromParcel
		
		
		public DeviceParcelable[] newArray(int iSize)
		{
			
			return new DeviceParcelable[iSize];
		}//newArray
	};
	
	
	
	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// TODO Auto-generated method stub

		dest.writeValue(mDeviceBinder);
	}
	
}
	//