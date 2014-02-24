package wistron.upnp.heidi;

import java.io.IOException;
import java.util.ArrayList;

import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.os.Bundle;
import android.os.IBinder;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;

import org.fourthline.cling.android.AndroidUpnpService;
import org.fourthline.cling.android.AndroidUpnpServiceImpl;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.ServiceType;
import org.fourthline.cling.model.types.UDAServiceType;

public class MainActivity extends ListActivity {

    protected static final String TAG = "MainActivity";
    public static final ServiceType SUPPORTED_AV_TRANSPORT_TYPE = new UDAServiceType(
            "AVTransport", 1);

    // DOC:CLASS
    // DOC:SERVICE_BINDING
    private ArrayAdapter<DeviceDisplay> listAdapter;

    public static String streamUrl = null;
    public static String streamUrlErrMsg = null;
    ArrayList<DeviceDisplay> mDevice;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        if (!isUpnpScanServiceRunning()) {
            Intent startServiceIntent = new Intent(this, UpnpScanService.class);
            startService(startServiceIntent);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            Log.d(TAG, "start upnp service.....");
        }

        listAdapter = new ArrayAdapter<DeviceDisplay>(this,
                android.R.layout.simple_list_item_1);
        setListAdapter(listAdapter);

        // You can be pretty confident that the intent will not be null here.
        Intent intent = getIntent();

        // Get the extras (if there are any)
        Bundle extras = intent.getExtras();
        Log.d(TAG, "extras=" + extras);
        if (extras != null) {
            mDevice = ((DeviceParcelable) this.getIntent().getExtras()
                    .getParcelable("devicelist")).GetDeviceList();
            Log.d(TAG, "mDevice=" + mDevice);
            for (DeviceDisplay dd : mDevice) {
                listAdapter.add(dd);
            }
        }

        Log.d(TAG, "finish add");

        /*  */

        org.seamless.util.logging.LoggingUtil.resetRootHandler();

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

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Intent i = new Intent("com.wistron.heidi.SEND_POSITION");
        Bundle bundle = new Bundle();
        bundle.putInt("position", position);
        i.putExtras(bundle);
        sendBroadcast(i);    
        this.finish();
    }

}
