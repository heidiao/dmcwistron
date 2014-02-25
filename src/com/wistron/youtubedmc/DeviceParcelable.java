package com.wistron.youtubedmc;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

public class DeviceParcelable implements Parcelable {

    private DeviceBinder mDeviceBinder;

    public DeviceParcelable() {
        mDeviceBinder = new DeviceBinder();
    }

    public DeviceParcelable(Parcel cParcel) {
        try {
            mDeviceBinder = (DeviceBinder) cParcel.readValue(DeviceBinder.class
                    .getClassLoader());
        } catch (Exception exException) {

        }
    }

    public void SetDeviceList(ArrayList<DeviceDisplay> d) {

        mDeviceBinder.SetDeviceList(d);
    }

    public ArrayList<DeviceDisplay> GetDeviceList() {
        ArrayList<DeviceDisplay> cResult = null;
        cResult = mDeviceBinder.GetDeviceList();
        return cResult;
    }

    public static final Parcelable.Creator<DeviceParcelable> CREATOR = new Creator<DeviceParcelable>() {
        public DeviceParcelable createFromParcel(Parcel cParcel) {
            return new DeviceParcelable(cParcel);
        }

        public DeviceParcelable[] newArray(int iSize) {
            return new DeviceParcelable[iSize];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(mDeviceBinder);
    }

}
//