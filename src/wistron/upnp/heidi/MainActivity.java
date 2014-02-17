package wistron.upnp.heidi;


import android.util.Log;
import android.os.Bundle;
import android.os.IBinder;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection; 

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



public class MainActivity extends Activity {

    private AndroidUpnpService upnpService;
    private BrowseRegistryListener registryListener = new BrowseRegistryListener();
    public static final String TAG = "MainActivity";

    protected Presenter presenter;
    public static final ServiceType SUPPORTED_AV_TRANSPORT_TYPE = new UDAServiceType("AVTransport", 1);
    
    private ServiceConnection serviceConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected:"+name);
            upnpService = (AndroidUpnpService) service;
            upnpService.getRegistry().addListener(registryListener);
            // Now add all devices to the list we already know about
            for (Device device : upnpService.getRegistry().getDevices()) {
                Log.d(TAG, "device:"+device);
                registryListener.deviceAdded(device);
            }

            upnpService.getControlPoint().search();
            Log.d(TAG, "upnp server search...?");
            
                        // Search asynchronously for all devices, they will respond soon
            upnpService.getControlPoint().search();

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisonnected:"+name);
            // TODO Auto-generated method stub
            upnpService = null;
            
        }
        
    };
 
    protected void sendToMediaRenderer(final int instanceId, final Service avTransportService, String uri) {
        SetAVTransportURI setAVTransportURIActionCallback =
                new SetAVTransportURI(new UnsignedIntegerFourBytes(instanceId), avTransportService, uri) {

                    @Override
                    public void success(ActionInvocation invocation) {
                        Log.d(TAG, "Successfuly sent URI to: (Instance: " + instanceId + ") " +
                                avTransportService.getDevice().getDetails().getFriendlyName()); 
                    }
 
                    @Override
                    public void failure(ActionInvocation arg0,
                            UpnpResponse arg1, String arg2) {
                        Log.d(TAG, "Failed to send URI: UpnpResponse>> " + arg1); 
                        Log.d(TAG, "Failed to send URI:                " + arg0+"..."+ arg2); 
                        // TODO Auto-generated method stub
                        
                    }
                };

        upnpService.getControlPoint().execute(setAVTransportURIActionCallback);
        try{
            Thread.sleep(3000);
        }catch(InterruptedException ie){

        }

        upnpService.getControlPoint().execute(new Play(new UnsignedIntegerFourBytes(instanceId), avTransportService) {
            @Override
            public void success(ActionInvocation invocation) {
                Log.d(TAG,
                    "Called 'Play' action successfully"
                );
            }

            @Override
            public void failure(ActionInvocation invocation,
                                UpnpResponse operation,
                                String defaultMsg) {
                 Log.d(TAG,"Fail:"+defaultMsg);
            }
        });
    }
           
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); 

      // Fix the logging integration between java.util.logging and Android internal logging
        org.seamless.util.logging.LoggingUtil.resetRootHandler(
        //    new org.seamless.android.FixedAndroidLogHandler()
        );

        
                //new Intent(this, BrowserUpnpService.class),
        getApplicationContext().bindService(
                new Intent(this, AndroidUpnpServiceImpl.class),
                serviceConnection,
                Context.BIND_AUTO_CREATE
            );
        
           // UPnP discovery is asynchronous, we need a callback
       

        // This will create necessary network resources for UPnP right away
        Log.d(TAG, "Starting Cling...");

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
   

    boolean flag = false;
    protected class BrowseRegistryListener extends DefaultRegistryListener {

        public void deviceAdded(final Device device) {
            runOnUiThread(new Runnable() {
                public void run() {
                    DeviceDisplay d = new DeviceDisplay(device);
                    Log.d(TAG, "["+d.toString()+"]");
                    //Log.d(TAG, "              "+d.getDetailsMessage());
                    //Log.d(TAG, "-----------------------------------------------------------------");
                    

                    //   Service getMatchingAVTransportService(Device device, List<ProtocolInfo> infos, Res resource);

                   // final Service avTransportService =
                     //       presenter.getMatchingAVTransportService(device, entry.getValue(), resource);
                    
                    if(d.toString().equals("MediaRenderer on sonyvaio_i7") && flag==false){
                    //if(d.toString().equals("10106003 (TPEA90057679 : Windows Media Player) *") && flag==false){
                        flag =true;
                        Log.d(TAG, " start push url to dmr...");
                        Service avTransportService =
                                device.findService(SUPPORTED_AV_TRANSPORT_TYPE);
                        Log.d(TAG, " avTransportService:"+avTransportService);
                    //    String url = "http://r10---sn-u2x76n76.googlevideo.com/videoplayback?source=youtube&key=yt5&ip=211.20.60.37&ratebypass=yes&ipbits=0&itag=22&sver=3&expire=1392368708&fexp=936907%2C914049%2C916611%2C937417%2C913434%2C936910%2C936913%2C3300054%2C3300111%2C3300132%2C3300137%2C3300161%2C3310366%2C3310623%2C3310649&sparams=id%2Cip%2Cipbits%2Citag%2Cratebypass%2Csource%2Cupn%2Cexpire&upn=Ddz7j0nep6g&id=o-ADVwC3Us5Zjo0Bj0u_Nnl4LTOy0HGqGuSXK3JrXGczYy&signature=777934C7A8394B1C9E19E4D9C949F2550B3478DB.13A3AE2E0031DC89E539012AA55EE90478D8B47E&redirect_counter=1&cms_redirect=yes&ms=nxu&mt=1392348804&mv=m";
                        String url = "http://10.34.197.70:32400/library/parts/357/file.mp4";
                        if (avTransportService != null) {
                            sendToMediaRenderer(0, avTransportService, url);
                            Log.d(TAG, " pushed!");
                        }
                    }
                   // sendToMediaRenderer(instanceId, avTransportService, uri);
                }
            });
        }

        public void deviceRemoved(final Device device) {
            runOnUiThread(new Runnable() {
                public void run() {
                    DeviceDisplay d = new DeviceDisplay(device);
                    Log.d(TAG, "remove :"+d.toString());
                }
            });
        }

        /* Discovery performance optimization for very slow Android devices! */
        @Override
        public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice device) {
            Log.d(TAG, "remote Device Discovery Started: ");
            deviceAdded(device);
        }

        @Override
        public void remoteDeviceDiscoveryFailed(Registry registry, final RemoteDevice device, final Exception ex) {
            Log.d(TAG, "remote Device Discovery Failed: ");
            deviceRemoved(device);
        }
        /* End of optimization, you can remove the whole block if your Android handset is fast (>= 600 Mhz) */

        @Override
        public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
            deviceAdded(device);
        }

        @Override
        public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
            Log.d(TAG, "remote Device Removed: ");
            deviceRemoved(device);
        }

        @Override
        public void localDeviceAdded(Registry registry, LocalDevice device) {
            Log.d(TAG, "local Device Added: ");
            deviceAdded(device);
        }

        @Override
        public void localDeviceRemoved(Registry registry, LocalDevice device) {
            Log.d(TAG, "local Device Removed: ");
            deviceRemoved(device);
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
                //sb.append(getString(R.string.deviceDetailsNotYetAvailable));
                sb.append("deviceDetailsNotYetAvailable");
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
}

