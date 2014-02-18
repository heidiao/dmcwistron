package wistron.upnp.heidi;

import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Bundle;
import android.os.IBinder;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection; 
import android.graphics.PixelFormat;

import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.model.DefaultServiceManager;
import org.fourthline.cling.model.ModelUtil;
import org.fourthline.cling.model.ServiceManager;
import org.fourthline.cling.model.ValidationException;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.message.header.STAllHeader;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.DeviceDetails;
import org.fourthline.cling.model.meta.DeviceIdentity;
import org.fourthline.cling.model.meta.Icon;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.LocalService;
import org.fourthline.cling.model.meta.ManufacturerDetails;
import org.fourthline.cling.model.meta.ModelDetails;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.ServiceType;
import org.fourthline.cling.model.types.UDADeviceType;
import org.fourthline.cling.model.types.UDAServiceType;
import org.fourthline.cling.model.types.UDN;
import org.fourthline.cling.model.types.UnsignedIntegerFourBytes;
import org.fourthline.cling.registry.DefaultRegistryListener;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.registry.RegistryListener;
import org.fourthline.cling.support.lastchange.LastChangeAwareServiceManager;
import org.fourthline.cling.support.model.TransportState;
import org.fourthline.cling.support.avtransport.callback.SetAVTransportURI;
import org.fourthline.cling.support.avtransport.callback.Play;
import org.fourthline.cling.support.avtransport.lastchange.AVTransportLastChangeParser;
import org.fourthline.cling.support.lastchange.LastChange;
import org.fourthline.cling.support.renderingcontrol.lastchange.RenderingControlLastChangeParser;
import org.fourthline.cling.support.shared.log.LogView.Presenter;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.android.AndroidUpnpService;
import org.fourthline.cling.android.AndroidUpnpServiceImpl;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;



public class MainActivity  extends ListActivity {



    protected static final String TAG = "MainActivity";
    public static final ServiceType SUPPORTED_AV_TRANSPORT_TYPE = new UDAServiceType("AVTransport", 1);

    // DOC:CLASS
    // DOC:SERVICE_BINDING
    private ArrayAdapter<DeviceDisplay> listAdapter;

    private BrowseRegistryListener registryListener = new BrowseRegistryListener();

    private AndroidUpnpService upnpService;

    private ServiceConnection serviceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            upnpService = (AndroidUpnpService) service;

            // Clear the list
            listAdapter.clear();

            // Get ready for future device advertisements
            upnpService.getRegistry().addListener(registryListener);

            // Now add all devices to the list we already know about
            for (Device device : upnpService.getRegistry().getDevices()) {
                registryListener.deviceAdded(device);
            }

            // Search asynchronously for all devices, they will respond soon
            upnpService.getControlPoint().search();
        }

        public void onServiceDisconnected(ComponentName className) {
            upnpService = null;
        }
    };

    public static String streamUrl = null;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
                Log.d(TAG, "url:"+url+" subject:"+subject);
                try {
                    streamUrl = getStreamingUrisFromYouTubePage(url);
                } catch (Exception e) {
                    Log.e("MainActivity","Couldn't get YouTube streaming URL", e);
                }
                if(streamUrl == null){
                    streamUrlErrMsg = "Couldn't find any URLs and corresponding signatures";
                    Toast.makeText(this, streamUrlErrMsg, Toast.LENGTH_SHORT).show();
                    this.finish();
                }
                Log.d(TAG, "streamUrl:"+streamUrl);
            }
        }

        org.seamless.util.logging.LoggingUtil.resetRootHandler();

        listAdapter = new ArrayAdapter<DeviceDisplay>(this, android.R.layout.simple_list_item_1);
        setListAdapter(listAdapter);

        // This will start the UPnP service if it wasn't already started
        getApplicationContext().bindService(
            new Intent(this, AndroidUpnpServiceImpl.class),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (upnpService != null) {
            upnpService.getRegistry().removeListener(registryListener);
        }
        // This will stop the UPnP service if nobody else is bound to it
        getApplicationContext().unbindService(serviceConnection);
    }
    // DOC:SERVICE_BINDING

    

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        /*AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setTitle(R.string.deviceDetails);
        DeviceDisplay deviceDisplay = (DeviceDisplay)l.getItemAtPosition(position);
        dialog.setMessage(deviceDisplay.getDetailsMessage());
        dialog.setButton(
            getString(R.string.OK),
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                }
            }
        );
        dialog.show();
        TextView textView = (TextView) dialog.findViewById(android.R.id.message);
        textView.setTextSize(12);
        super.onListItemClick(l, v, position, id);
        */
        DeviceDisplay d = (DeviceDisplay)l.getItemAtPosition(position);
        Device device = d.getDevice();
        Service avTransportService =  device.findService(SUPPORTED_AV_TRANSPORT_TYPE); 
    //    String url = "http://r10---sn-u2x76n76.googlevideo.com/videoplayback?source=youtube&key=yt5&ip=211.20.60.37&ratebypass=yes&ipbits=0&itag=22&sver=3&expire=1392368708&fexp=936907%2C914049%2C916611%2C937417%2C913434%2C936910%2C936913%2C3300054%2C3300111%2C3300132%2C3300137%2C3300161%2C3310366%2C3310623%2C3310649&sparams=id%2Cip%2Cipbits%2Citag%2Cratebypass%2Csource%2Cupn%2Cexpire&upn=Ddz7j0nep6g&id=o-ADVwC3Us5Zjo0Bj0u_Nnl4LTOy0HGqGuSXK3JrXGczYy&signature=777934C7A8394B1C9E19E4D9C949F2550B3478DB.13A3AE2E0031DC89E539012AA55EE90478D8B47E&redirect_counter=1&cms_redirect=yes&ms=nxu&mt=1392348804&mv=m";
        //String url = "http://10.34.197.55:32400/library/parts/357/file.mp4";
        if (avTransportService != null && streamUrl!=null) {
            sendToMediaRenderer(0, avTransportService, streamUrl);
            Log.d(TAG, "Push to ["+d.toString()+"]");
        }
    }

    protected class BrowseRegistryListener extends DefaultRegistryListener {

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

        public void deviceAdded(final Device device) {
            runOnUiThread(new Runnable() {
                public void run() {
                    DeviceDisplay d = new DeviceDisplay(device);
                    Log.d(TAG, "[ "+d.toString()+" ]");
                    //if(d.toString().startsWith("MediaRenderer")){
                    if(d.toString().startsWith("WiRenderer")){
                        int position = listAdapter.getPosition(d);
                        if (position >= 0) {
                            // Device already in the list, re-set new value at same position
                            listAdapter.remove(d);
                            listAdapter.insert(d, position);
                        } else {
                            listAdapter.add(d);
                        }
                    }
                }
            });
        }

        public void deviceRemoved(final Device device) {
            runOnUiThread(new Runnable() {
                public void run() {
                    listAdapter.remove(new DeviceDisplay(device));
                }
            });
        }
    }

    protected class DeviceDisplay {

        Device device;

        public DeviceDisplay(Device device) {
            this.device = device;
        }

        public Device getDevice() {
            return device;
        }

        // DOC:DETAILS
        public String getDetailsMessage() {
            StringBuilder sb = new StringBuilder();
            if (getDevice().isFullyHydrated()) {
                sb.append(getDevice().getDisplayString());
                sb.append("\n\n");
                for (Service service : getDevice().getServices()) {
                    sb.append(service.getServiceType()).append("\n");
                }
            } else {
                sb.append(getString(R.string.deviceDetailsNotYetAvailable));
            }
            return sb.toString();
        }
        // DOC:DETAILS

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DeviceDisplay that = (DeviceDisplay) o;
            return device.equals(that.device);
        }

        @Override
        public int hashCode() {
            return device.hashCode();
        }

        @Override
        public String toString() {
            String name =
                getDevice().getDetails() != null && getDevice().getDetails().getFriendlyName() != null
                    ? getDevice().getDetails().getFriendlyName()
                    : getDevice().getDisplayString();
            // Display a little star while the device is being loaded (see performance optimization earlier)
            return device.isFullyHydrated() ? name : name + " *";
        }
    }
    // DOC:CLASS_END
    // ...

    
    protected void sendPlay(final int instanceId, final Service avTransportService){   
        upnpService.getControlPoint().execute(new Play(new UnsignedIntegerFourBytes(instanceId), avTransportService) {
            @Override
            public void success(ActionInvocation invocation) {
                   Log.d(TAG, "sendPlay() 'Play' action successfully");
                   MainActivity.this.finish();
            }

            @Override
            public void failure(ActionInvocation invocation, UpnpResponse operation,  String defaultMsg) {
                 Log.d(TAG,"sendPlay() Fail:"+defaultMsg);
            }
        });
    }
    
    protected void sendToMediaRenderer(final int instanceId, final Service avTransportService, String uri) {             
        upnpService.getControlPoint().execute( new SetAVTransportURI(new UnsignedIntegerFourBytes(instanceId), avTransportService, uri) {
            @Override
            public void success(ActionInvocation invocation) {
                Log.d(TAG, "sendToMediaRenderer() Successfuly sent URI to: (Instance: " + instanceId + ") " +
                        avTransportService.getDevice().getDetails().getFriendlyName()); 
                sendPlay(instanceId, avTransportService);
            }

            @Override
            public void failure(ActionInvocation arg0,
                    UpnpResponse arg1, String arg2) {
                Log.d(TAG, "sendToMediaRenderer() Failed to send URI: " + arg0+"..."+ arg2); 
            }
        });      
    }


    public static String streamUrlErrMsg = null;

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
            streamUrlErrMsg  = "YouTube is asking for age verification. We can't handle that sorry.";
            Log.w("MainActivity",streamUrlErrMsg);
            return null;
        }
    
        if (html.contains("das_captcha")) {
            streamUrlErrMsg = "Captcha found, please try with different IP address.";
            Log.w("MainActivity",streamUrlErrMsg);
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
            Log.w("MainActivity",streamUrlErrMsg);
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
            Log.w("MainActivity",streamUrlErrMsg);
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
                Log.d("MainActivity","YouTube Video streaming details: ext:" + newVideo.ext
                        + ", type:" + newVideo.type + ", url:" + newVideo.url);
                return newVideo.url;
            }
        }
        streamUrlErrMsg = "";
        return null;
    }
}

