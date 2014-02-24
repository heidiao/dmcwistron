package wistron.upnp.heidi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.fourthline.cling.android.AndroidUpnpService;
import org.fourthline.cling.android.AndroidUpnpServiceImpl;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.types.ServiceType;
import org.fourthline.cling.model.types.UDAServiceType;
import org.fourthline.cling.model.types.UnsignedIntegerFourBytes;
import org.fourthline.cling.support.avtransport.callback.Play;
import org.fourthline.cling.support.avtransport.callback.SetAVTransportURI;

import android.app.Activity;
import android.app.IntentService;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.util.Log;
import android.widget.ArrayAdapter;

public class UpnpScanService extends Service {

    private AndroidUpnpService upnpService;
    private static final String TAG = "UpnpScanService";
    private BrowseRegistryListener registryListener = new BrowseRegistryListener();
    public static final ServiceType SUPPORTED_AV_TRANSPORT_TYPE = new UDAServiceType(
            "AVTransport", 1);
    public static String streamUrl = null;
    public static String streamUrlErrMsg = null;
    private DeviceParcelable mDeviceParcelable;
    private ArrayList<DeviceDisplay> mDeviceList;

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate()");
        // TODO Auto-generated method stub
        super.onCreate();
        mDeviceList = new ArrayList<DeviceDisplay>();
        mDeviceParcelable = new DeviceParcelable();
        boolean isBound = bindService(new Intent(this,
                AndroidUpnpServiceImpl.class), serviceConnection,
                Context.BIND_AUTO_CREATE);
        Log.d(TAG, "isBound=" + isBound);

        /*
         * getApplicationContext().bindService( new Intent(this,
         * AndroidUpnpServiceImpl.class), serviceConnection,
         * Context.BIND_AUTO_CREATE );
         */
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        // TODO Auto-generated method stub
        super.onDestroy();

        if (upnpService != null) {
            upnpService.getRegistry().removeListener(registryListener);
        }
        // This will stop the UPnP service if nobody else is bound to it
        getBaseContext().unbindService(serviceConnection);
        unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        /*
         * <action android:name="android.intent.action.ACTION_BOOT_COMPLETED" />
         * <action android:name="com.wistron.heidi.UPNPSTART" /> <action
         * android:name="com.wistron.heidi.RESPONSE_DEVICE" />
         */
        IntentFilter intentFilter = new IntentFilter();
//        intentFilter.addDataAuthority("www.youtube.com",null);
//        try {
//            intentFilter.addDataType("text/plain");
//        } catch (MalformedMimeTypeException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//        intentFilter.addCategory("android.intent.category.DEFAULT");
//        intentFilter.addAction("android.intent.action.SEND");
        intentFilter.addAction("com.wistron.heidi.URL");
        intentFilter.addAction("com.wistron.heidi.SEND_POSITION");
        intentFilter.addAction("com.wistron.heidi.YOUTUBE");
        intentFilter.addAction("com.wistron.heidi.UPNPSTART");
        intentFilter.addAction("android.intent.action.ACTION_BOOT_COMPLETEDE");
        registerReceiver(mBroadcastReceiver, intentFilter);

        // TODO Auto-generated method stub
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind()");
        if (upnpService != null) {
            upnpService.getRegistry().removeListener(registryListener);
        }
        // This will stop the UPnP service if nobody else is bound to it
        unbindService(serviceConnection);
        // TODO Auto-generated method stub
        return super.onUnbind(intent);
    }

    @Override
    public IBinder onBind(Intent arg0) {
        Log.d(TAG, "onBind()");
        // TODO Auto-generated method stub
        return null;
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            upnpService = (AndroidUpnpService) service;

            // Get ready for future device advertisements
            upnpService.getRegistry().addListener(registryListener);

            // Now add all devices to the list we already know about
            for (Device device : upnpService.getRegistry().getDevices()) {
                registryListener.deviceAdded(device);
            }

            // Search asynchronously for all devices, they will respond soon
            upnpService.getControlPoint().search();

            Log.e(TAG, "onServiceConnected()");
        }

        public void onServiceDisconnected(ComponentName className) {
            upnpService = null;
        }
    };

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            String ac = intent.getAction();
            Log.d(TAG, "onReceive:" + ac);

            if (ac.equals("com.wistron.heidi.URL")) {
                String org_url = intent.getExtras().getString("url");
                Log.d(TAG, "org_url:" + org_url);
                try {
                    streamUrl = getStreamingUrisFromYouTubePage(org_url);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                if (streamUrl != null) {
                    mDeviceList = registryListener.getUpnpDevice();
                    mDeviceParcelable.SetDeviceList(mDeviceList);
                    int size = mDeviceList.size();
                    if (size == 1) {
                        sendToMediaRenderer(0, (DeviceDisplay) mDeviceList.get(0), streamUrl);
                    } else {
                        Intent i = new Intent(getBaseContext(), MainActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        Bundle bundle = new Bundle();
                        bundle.putParcelable("devicelist", mDeviceParcelable);
                        i.putExtras(bundle);
                        getApplication().startActivity(i);
                        Log.d(TAG, "startActivity:" + i);
                    }
                }
                  
            }else if (ac.equals("com.wistron.heidi.SEND_POSITION")) {
                int position = intent.getExtras().getInt("position");
                Log.i(TAG, "position="+position);
                sendToMediaRenderer(0, (DeviceDisplay) mDeviceList.get(position), streamUrl);
                
            } else if (ac.equals("com.wistron.heidi.UPNPSTART")
                    || ac.equals(Intent.ACTION_BOOT_COMPLETED)) {
                Intent startServiceIntent = new Intent(context,
                        UpnpScanService.class);
                context.startService(startServiceIntent);

            // ############ FOR TEST....
            // ############ FOR TEST....
            // ############ FOR TEST....
            } else if (ac.equals("com.wistron.heidi.YOUTUBE")) {
                streamUrl = "http://10.34.197.63:32400/library/parts/357/file.mp4";
                mDeviceList = registryListener.getUpnpDevice();
                mDeviceParcelable.SetDeviceList(mDeviceList);
                int size = mDeviceList.size();
                if (size == 1) {
                    sendToMediaRenderer(0, (DeviceDisplay) mDeviceList.get(0), streamUrl);
                } else {
                    Intent i = new Intent(getBaseContext(), MainActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    Bundle bundle = new Bundle();
                    bundle.putParcelable("devicelist", mDeviceParcelable);
                    i.putExtras(bundle);
                    getApplication().startActivity(i);
                    Log.d(TAG, "startActivity:" + i);
                }
                
            }
            // ############ FOR TEST....
            // ############ FOR TEST....
            // ############ FOR TEST....
        }
    };
    

    protected void sendPlay(final int instanceId,
            final org.fourthline.cling.model.meta.Service avTransportService) {
        upnpService.getControlPoint().execute(
                new Play(new UnsignedIntegerFourBytes(instanceId),
                        avTransportService) {
                    @Override
                    public void success(ActionInvocation invocation) {
                        Log.d(TAG, "sendPlay() 'Play' action successfully");
                    }

                    @Override
                    public void failure(ActionInvocation invocation,
                            UpnpResponse operation, String defaultMsg) {
                        Log.d(TAG, "sendPlay() Fail:" + defaultMsg);
                    }
                });
    }

    protected void sendToMediaRenderer(final int instanceId, DeviceDisplay device, String uri) {
        Log.i(TAG, "sendToMediaRenderer(), device="+device+", url="+uri);
        final org.fourthline.cling.model.meta.Service avTransportService = (device.getDevice()).findService(SUPPORTED_AV_TRANSPORT_TYPE);        
        upnpService.getControlPoint().execute(
                new SetAVTransportURI(new UnsignedIntegerFourBytes(instanceId),
                        avTransportService, uri) {
                    @Override
                    public void success(ActionInvocation invocation) {
                        Log.d(TAG,
                                "sendToMediaRenderer() Successfuly sent URI to: (Instance: "
                                        + instanceId
                                        + ") "
                                        + avTransportService.getDevice()
                                                .getDetails().getFriendlyName());
                        sendPlay(instanceId, avTransportService);
                    }

                    @Override
                    public void failure(ActionInvocation arg0,
                            UpnpResponse arg1, String arg2) {
                        Log.d(TAG, "sendToMediaRenderer() Failed to send URI: "
                                + arg0 + "..." + arg2);
                    }
                });
    }
    

    public String getStreamingUrisFromYouTubePage(String ytUrl)
            throws IOException {
        if (ytUrl == null) {
            return null;
        }

        // Remove any query params in query string after the watch?v=<vid> in
        // e.g.
        // http://www.youtube.com/watch?v=0RUPACpf8Vs&feature=youtube_gdata_player
        int andIdx = ytUrl.indexOf('&');
        if (andIdx >= 0) {
            ytUrl = ytUrl.substring(0, andIdx);
        }

        // Get the HTML response
        String userAgent = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:8.0.1)";
        HttpClient client = new DefaultHttpClient();
        client.getParams().setParameter(CoreProtocolPNames.USER_AGENT,
                userAgent);
        HttpGet request = new HttpGet(ytUrl);
        HttpResponse response = client.execute(request);
        String html = "";
        InputStream in = response.getEntity().getContent();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder str = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            str.append(line.replace("\\u0026", "&"));
        }
        in.close();
        html = str.toString();

        // Parse the HTML response and extract the streaming URIs
        if (html.contains("verify-age-thumb")) {
            streamUrlErrMsg = "YouTube is asking for age verification. We can't handle that sorry.";
            Log.w("MainActivity", streamUrlErrMsg);
            return null;
        }

        if (html.contains("das_captcha")) {
            streamUrlErrMsg = "Captcha found, please try with different IP address.";
            Log.w("MainActivity", streamUrlErrMsg);
            return null;
        }

        Pattern p = Pattern.compile("stream_map\": \"(.*?)?\"");
        // Pattern p = Pattern.compile("/stream_map=(.[^&]*?)\"/");
        Matcher m = p.matcher(html);
        List<String> matches = new ArrayList<String>();
        while (m.find()) {
            matches.add(m.group());
        }

        if (matches.size() != 1) {
            streamUrlErrMsg = "Found zero or too many stream maps.";
            Log.w("MainActivity", streamUrlErrMsg);
            return null;
        }

        String urls[] = matches.get(0).split(",");
        HashMap<String, String> foundArray = new HashMap<String, String>();
        for (String ppUrl : urls) {
            String url = URLDecoder.decode(ppUrl, "UTF-8");

            Pattern p1 = Pattern.compile("itag=([0-9]+?)[&]");
            Matcher m1 = p1.matcher(url);
            String itag = null;
            if (m1.find()) {
                itag = m1.group(1);
            }

            Pattern p2 = Pattern.compile("sig=(.*?)[&]");
            Matcher m2 = p2.matcher(url);
            String sig = null;
            if (m2.find()) {
                sig = m2.group(1);
            }

            Pattern p3 = Pattern.compile("url=(.*?)[&]");
            Matcher m3 = p3.matcher(ppUrl);
            String um = null;
            if (m3.find()) {
                um = m3.group(1);
            }

            if (itag != null && sig != null && um != null) {
                foundArray.put(itag, URLDecoder.decode(um, "UTF-8") + "&"
                        + "signature=" + sig);
            }
        }

        if (foundArray.size() == 0) {
            streamUrlErrMsg = "Couldn't find any URLs and corresponding signatures";
            Log.w("MainActivity", streamUrlErrMsg);
            return null;
        }

        HashMap<String, Meta> typeMap = new HashMap<String, Meta>();
        typeMap.put("13", new Meta("13", "3GP", "Low Quality - 176x144"));
        typeMap.put("17", new Meta("17", "3GP", "Medium Quality - 176x144"));
        typeMap.put("36", new Meta("36", "3GP", "High Quality - 320x240"));
        typeMap.put("5", new Meta("5", "FLV", "Low Quality - 400x226"));
        typeMap.put("6", new Meta("6", "FLV", "Medium Quality - 640x360"));
        typeMap.put("34", new Meta("34", "FLV", "Medium Quality - 640x360"));
        typeMap.put("35", new Meta("35", "FLV", "High Quality - 854x480"));
        typeMap.put("43", new Meta("43", "WEBM", "Low Quality - 640x360"));
        typeMap.put("44", new Meta("44", "WEBM", "Medium Quality - 854x480"));
        typeMap.put("45", new Meta("45", "WEBM", "High Quality - 1280x720"));
        typeMap.put("18", new Meta("18", "MP4", "Medium Quality - 480x360"));
        typeMap.put("22", new Meta("22", "MP4", "High Quality - 1280x720"));
        typeMap.put("37", new Meta("37", "MP4", "High Quality - 1920x1080"));
        typeMap.put("33", new Meta("38", "MP4", "High Quality - 4096x230"));

        // ArrayList<Video> videos = new ArrayList<ARViewer.Video>();
        ArrayList<Video> videos = new ArrayList();
        for (String format : typeMap.keySet()) {
            Meta meta = typeMap.get(format);

            if (foundArray.containsKey(format)) {
                Video newVideo = new Video(meta.ext, meta.type,
                        foundArray.get(format));
                videos.add(newVideo);
                Log.d("MainActivity", "YouTube Video streaming details: ext:"
                        + newVideo.ext + ", type:" + newVideo.type + ", url:"
                        + newVideo.url);
                return newVideo.url;
            }
        }
        streamUrlErrMsg = "";
        return null;
    }

    public String getStreamMsg() {
        return streamUrlErrMsg;
    }

}
