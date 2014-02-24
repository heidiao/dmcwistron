package wistron.upnp.heidi;

import java.io.IOException;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class TransparentActivity extends Activity {

    private static final String TAG = "TransparentActivity";
    
    @Override
    protected void onCreate(Bundle arg0) {
        // TODO Auto-generated method stub
        super.onCreate(arg0);

        if (!isUpnpScanServiceRunning()) {
          
            Thread thread1 = new Thread(){
                public void run(){
                    Intent startServiceIntent = new Intent(getApplicationContext(), UpnpScanService.class);
                    startService(startServiceIntent);
                    Log.d(TAG, "start upnp service.....");
                }
            };
            thread1.start();

            Log.d(TAG, "sleep 5 sec");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            Log.d(TAG, "sleep 5 sec DONE!");
        }

        String ac = getIntent().getAction();
        String url = null, subject = null;
        if(ac.equals(Intent.ACTION_SEND)){
            Bundle extras = getIntent().getExtras();
            if( null != extras){
                if( extras.containsKey(Intent.EXTRA_TEXT)){
                    url = extras.getString(Intent.EXTRA_TEXT);
                }
                if( extras.containsKey(Intent.EXTRA_SUBJECT)){
                    subject = extras.getString(Intent.EXTRA_SUBJECT);
                }

                Intent i = new Intent("com.wistron.heidi.URL");
                Bundle bundle = new Bundle();
                bundle.putString("url", url);
                i.putExtras(bundle);
                sendBroadcast(i);  
                Log.d(TAG, "url:"+url+" subject:"+subject);
                this.finish();
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
}
