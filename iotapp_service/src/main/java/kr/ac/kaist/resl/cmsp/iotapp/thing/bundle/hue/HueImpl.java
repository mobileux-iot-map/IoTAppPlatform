package kr.ac.kaist.resl.cmsp.iotapp.thing.bundle.hue;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import com.philips.lighting.hue.sdk.PHAccessPoint;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.hue.sdk.PHMessageType;
import com.philips.lighting.hue.sdk.PHSDKListener;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;
import kr.ac.kaist.resl.cmsp.iotapp.library.IoTAppException;
import kr.ac.kaist.resl.cmsp.iotapp.library.ThingServiceEndpoint;
import kr.ac.kaist.resl.cmsp.iotapp.thing.HueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by shheo on 15. 7. 20.
 */
public class HueImpl implements HueService {
    final Logger logger = LoggerFactory.getLogger(HueImpl.class.getSimpleName());

    ThingServiceEndpoint endpoint = null;
    private PHHueSDK phHueSDK;
    private static final int HUE_OFF = 0;
    private static final int HUE_ON = 1;
    private static final int HUE_UNKNOWN = 2;
    private int isOn = HUE_OFF;
    private boolean isConnected = false;
    public static final String TAG = "QuickStart";
    Context context;

    public HueImpl(Context context) {
        this.context = context;
        phHueSDK = PHHueSDK.create();
        phHueSDK.setAppName("HueBundle");
        phHueSDK.setDeviceName(Build.MODEL);
        phHueSDK.getNotificationManager().registerSDKListener(listener);
    }

    @Override
    public int getOnOff() throws IoTAppException {
        return 0;
    }

    @Override
    public void on() throws IoTAppException {
        PHLightState lightState = new PHLightState();
        lightState.setOn(true);
        for (PHLight light : phHueSDK.getSelectedBridge().getResourceCache().getAllLights()) {
            phHueSDK.getSelectedBridge().updateLightState(light, lightState);
        }
        isOn = HUE_ON;
    }

    @Override
    public void off() throws IoTAppException {
        PHLightState lightState = new PHLightState();
        lightState.setOn(false);
        for (PHLight light : phHueSDK.getSelectedBridge().getResourceCache().getAllLights()) {
            phHueSDK.getSelectedBridge().updateLightState(light, lightState);
        }
        isOn = HUE_OFF;
    }

    @Override
    public void toggle() throws IoTAppException {
        if (isOn == HUE_ON) {
            off();
        } else if (isOn == HUE_OFF) {
            on();
        } else {
            isOn = HUE_UNKNOWN;
        }
    }

    @Override
    public String getThingId() {
        return endpoint.getThingId();
    }

    @Override
    public String getThingName() {
        return endpoint.getThingName();
    }

    @Override
    public void setEndpoint(ThingServiceEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public void connect() {
        PHAccessPoint accessPoint = new PHAccessPoint();
        accessPoint.setIpAddress(endpoint.getEndpoint());
        System.out.println("endpoint: " + endpoint.getEndpoint());
        accessPoint.setUsername("HueBundleUser");
        phHueSDK.connect(accessPoint);
        /*
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                PHAccessPoint accessPoint = new PHAccessPoint();
                accessPoint.setIpAddress(endpoint.getEndpoint());
                System.out.println("endpoint: " + endpoint.getEndpoint());
                accessPoint.setUsername("HueBundleUser");
                phHueSDK.connect(accessPoint);
            }
        }).start();
        */
    }

    @Override
    public void disconnect() {
        phHueSDK.disconnect(phHueSDK.getSelectedBridge());
    }

    @Override
    public Boolean isConnected() {
        return isConnected;
    }


    // Local SDK Listener
    private PHSDKListener listener = new PHSDKListener() {

        @Override
        public void onAccessPointsFound(List accessPoint) {
            logger.debug("onAccessPointsFound");
            // Handle your bridge search results here.  Typically if multiple results are returned you will want to display them in a list
            // and let the user select their bridge.   If one is found you may opt to connect automatically to that bridge.
        }

        @Override
        public void onCacheUpdated(List cacheNotificationsList, PHBridge bridge) {
            // Here you receive notifications that the BridgeResource Cache was updated. Use the PHMessageType to
            // check which cache was updated, e.g.
            if (cacheNotificationsList.contains(PHMessageType.LIGHTS_CACHE_UPDATED)) {
                System.out.println("Lights Cache Updated ");
            }
        }

        @Override
        public void onBridgeConnected(PHBridge b) {
            logger.debug("onBridgeConnected");
            phHueSDK.setSelectedBridge(b);
            phHueSDK.enableHeartbeat(b, PHHueSDK.HB_INTERVAL);
            isConnected = true;
            // Here it is recommended to set your connected bridge in your sdk object (as above) and start the heartbeat.
            // At this point you are connected to a bridge so you should pass control to your main program/activity.
            // Also it is recommended you store the connected IP Address/ Username in your app here.  This will allow easy automatic connection on subsequent use.

        }

        @Override
        public void onAuthenticationRequired(PHAccessPoint accessPoint) {
            logger.debug("onAuthenticationRequired");
            phHueSDK.startPushlinkAuthentication(accessPoint);
            // Arriving here indicates that Pushlinking is required (to prove the User has physical access to the bridge).  Typically here
            // you will display a pushlink image (with a timer) indicating to to the user they need to push the button on their bridge within 30 seconds.
        }

        @Override
        public void onConnectionResumed(PHBridge bridge) {
            logger.debug("onConnectionResumed");
            isConnected = true;
        }

        @Override
        public void onConnectionLost(PHAccessPoint accessPoint) {
            // Here you would handle the loss of connection to your bridge.
            logger.debug("onConnectionLost");
            isConnected = false;
        }

        @Override
        public void onError(int code, final String message) {
            logger.debug("onError: " + message);
            // Here you can handle events such as Bridge Not Responding, Authentication Failed and Bridge Not Found
            isConnected = false;
        }

        @Override
        public void onParsingErrors(List parsingErrorsList) {
            logger.debug("onParsingErrors");
            // Any JSON parsing errors are returned here.  Typically your program should never return these.
        }
    };
}
