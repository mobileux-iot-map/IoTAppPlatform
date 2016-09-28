package kr.ac.kaist.resl.cmsp.iotapp.platform;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;
import kr.ac.kaist.resl.cmsp.iotapp.engine.PlatformException;
import kr.ac.kaist.resl.cmsp.iotapp.engine.abstraction.AbstractionModule;
import kr.ac.kaist.resl.cmsp.iotapp.library.IGetAvailableServicesCallback;
import kr.ac.kaist.resl.cmsp.iotapp.library.ThingServiceEndpoint;
import kr.ac.kaist.resl.cmsp.iotapp.engine.clustering.AllJoynClusteringModule;
import kr.ac.kaist.resl.cmsp.iotapp.engine.clustering.ClusteringModule;
import kr.ac.kaist.resl.cmsp.iotapp.engine.connectivity.ConnectivityModule;
import kr.ac.kaist.resl.cmsp.iotapp.library.ThingServiceInfo;
import kr.ac.kaist.resl.cmsp.iotapp.library.impl.ILocalServiceObjectHandler;
import kr.ac.kaist.resl.cmsp.iotapp.library.impl.IPlatformService;
import kr.ac.kaist.resl.cmsp.iotapp.library.invocation.MethodInvocationJsonObject;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by shheo on 15. 4. 7.
 */
public class PlatformService extends Service {
    final Logger logger = LoggerFactory.getLogger(PlatformService.class.getSimpleName());
    private static final int mId = 849283412;
    private static AbstractionModule absModule;
    private static ConnectivityModule connModule;
    private static ClusteringModule clusterModule;
    private final IPlatformService.Stub mBinder = new IPlatformServiceImpl();
    public static final String APP_NAME = PlatformService.class.getName();
    // Use 64-bit zeros and 64-bit ANDROID_ID as device ID. ANDROID_ID is changed when the device performs factory-reset.
    private static UUID APP_ID;
    private static UUID DEVICE_ID;

    public static UUID getDeviceId() {
        return DEVICE_ID;
    }

    public static UUID getAppId() {
        return APP_ID;
    }

    public static ClusteringModule getClusterModule() {
        return clusterModule;
    }

    public static AbstractionModule getAbstractionModule() {
        return absModule;
    }

    public void registerServiceObject(ThingServiceEndpoint info, ILocalServiceObjectHandler handler) {
        absModule.addLocalService(info, handler);
    }

    public void unregisterServiceObject(String thingId) {
        absModule.removeLocalService(thingId);
    }

    public List<ThingServiceInfo> getAvailableServices(Set<String> requiredInterfaces) {
        return absModule.getAvailableServices(requiredInterfaces);
    }

    public void getAvailableServices(Set<String> requiredInterfaces, int scanPeriod, IGetAvailableServicesCallback callback) {
        absModule.getAvailableServices(requiredInterfaces, scanPeriod, callback);
    }

    public String handleServiceInvocation(String methodInvocation) {
        Object toReturn = null;
        try {
            MethodInvocationJsonObject invocation = new MethodInvocationJsonObject(methodInvocation);
            String thingId = invocation.getThingId();
            if (absModule.isLocalService(thingId)) {
                toReturn = absModule.invokeLocalService(invocation);
            } else if (absModule.isAvailableService(thingId)) {
                if (invocation.getReturnType().equalsIgnoreCase("void")) {
                    clusterModule.sendServiceInvocation(thingId, methodInvocation);
                } else {
                    String toReturnStr = clusterModule.sendServiceInvocationWithReturn(thingId, methodInvocation);
                    toReturn = MethodInvocationJsonObject.cast(invocation.getReturnType(), toReturnStr);
                }
            } else {
                logger.error("Thing id " + thingId + " is not available");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        // this will be casted to appropriate type in invocation handler
        if (toReturn == null)
            return null;
        else
            return toReturn.toString();
    }

    public String handleAppServiceInvocation(String thingId, MethodInvocationJsonObject object) {
        //return mHandler.handleAppServiceInvocation(clientHash, object);
        return null;
    }

    @Override
    public void onCreate() {
        // Get device id as UUID
        String android_id = Settings.Secure.getString(getApplicationContext().getContentResolver(),
                Settings.Secure.ANDROID_ID);

        logger.debug("Android ID is " + android_id);
        String deviceUuid = "00000000-0000-0000-" + android_id.substring(0, 4) + "-" + android_id.substring(5);
        DEVICE_ID = UUID.fromString(deviceUuid);

        logger.debug("Device ID (UUID) is " + DEVICE_ID);

        // get app id as UUID
        SharedPreferences prefs = this.getSharedPreferences("AndroidIoTAppService", Context.MODE_PRIVATE);
        String uuidStr = prefs.getString("APP_ID", null);
        if (uuidStr == null) {
            while (true) {
                APP_ID = UUID.randomUUID();
                // AllJoyn bus exception is thrown when the APP_ID start with digit.
                // FIXME: modify this in more elegant way
                if (!Character.isDigit(APP_ID.toString().charAt(0)))
                    break;
            }
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("APP_ID", APP_ID.toString());
            editor.apply();
        } else {
            APP_ID = UUID.fromString(uuidStr);
        }
        logger.debug("App ID (UUID) is " + APP_ID);

        absModule = new AbstractionModule();
        connModule = new ConnectivityModule();
        clusterModule = new AllJoynClusteringModule();

        // Initialize and start whole platform
        logger.debug("Initializing connectivity module...");
        connModule.init();
        logger.debug("Connectivity module is initialized");

        logger.debug("Initializing abstraction module...");
        absModule.init(this);
        logger.debug("Starting abstraction module...");
        try {
            absModule.start();
        } catch (PlatformException e) {
            logger.error("Failed to start abstraction module", e);
            stopSelf();
        }
        logger.debug("Abstraction module is started");

        logger.debug("Starting clustering module...");
        clusterModule.init(getApplicationContext());
        logger.debug("Clustering module is started");
        try {
            clusterModule.start();
        } catch (PlatformException e) {
            logger.error("Failed to start clustering module", e);
            stopSelf();
        }

        // Set fixed notification
        PendingIntent intent = PendingIntent.getActivity(this, 0, new Intent(this, PlatformMain.class), 0);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_noti_icon)
                .setContentTitle("IoT-App Platform Service")
                .setContentText("Service is running")
                .setOngoing(true)
                .setContentIntent(intent);


        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(mId, mBuilder.build());

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, (flags | START_STICKY), startId);
    }

    @Override
    public void onLowMemory() {
        logger.warn("Service is suffering low memory");
        super.onLowMemory();
    }

    @Override
    public IBinder onBind(Intent intent) {
        String app_name = intent.getStringExtra("app_name");
        logger.debug(app_name + "is bound to service");
        // FIXME: just for avoid delay. don't scan here
        absModule.scanDevices(6000);
        return mBinder.asBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        String app_name = intent.getStringExtra("app_name");
        logger.debug(app_name + " is unbound from service");
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        String app_name = intent.getStringExtra("app_name");
        logger.debug(app_name + "is rebound to service");
        super.onRebind(intent);
    }

    @Override
    public void onDestroy() {
        logger.debug("Service is being destroyed");
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(mId);
        connModule.stop();
        logger.debug("Stopping abstraction module...");
        absModule.stop();
        logger.debug("Abstraction module is stopped");
        logger.debug("Stopping clustering module...");
        clusterModule.stop();
        logger.debug("Clustering module is stopped");


        absModule = null;
        connModule = null;
        clusterModule = null;

        logger.debug("Service is destroyed");
        super.onDestroy();
    }

    public class IPlatformServiceImpl extends IPlatformService.Stub {

        @Override
        public void registerClient() throws RemoteException {
            // NOT_USED
        }

        @Override
        public void sendMessage(String invocation) throws RemoteException {
            PlatformService.this.handleServiceInvocation(invocation);
        }

        @Override
        public String invokeMessage(String invocation) throws RemoteException {
            return PlatformService.this.handleServiceInvocation(invocation);
        }

        @Override
        public List<ThingServiceInfo> getAvailableServicesNoScan(List<String> requiredServices) throws RemoteException {
            return PlatformService.this.getAvailableServices(new HashSet<>(requiredServices));
        }

        @Override
        public void getAvailableServices(List<String> requiredServices, int scanPeriod, IGetAvailableServicesCallback callback) throws RemoteException {
            PlatformService.this.getAvailableServices(new HashSet<>(requiredServices), scanPeriod, callback);
        }

        @Override
        public void registerLocalServiceObject(ThingServiceInfo thingInfo, ILocalServiceObjectHandler handler) throws RemoteException {
            try {
                ThingServiceEndpoint endpoint = new ThingServiceEndpoint(thingInfo);
                endpoint.setEndpoint(thingInfo.getDeviceId());
                PlatformService.this.registerServiceObject(endpoint, handler);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void unregisterLocalServiceObject(String thingId) throws RemoteException {
            PlatformService.this.unregisterServiceObject(thingId);
        }

        @Override
        public boolean openSessionAndStartAdvertise() throws RemoteException {
            if (PlatformService.getClusterModule().openSessionAndAdvertise() != 0) {
                makeToast("Failed to open session and advertise it");
                return false;
            } else {
                makeToast("Opened session and starting advertisement");
                return true;
            }
        }

        @Override
        public boolean closeSessionAndStopAdvertise() throws RemoteException {
            if (PlatformService.getClusterModule().stopAdvertiseAndCloseSession() != 0) {
                makeToast("Failed to stop advertisement and close session");
                return false;
            } else {
                makeToast("Stopped advertisement and closed session");
                return true;
            }
        }

        @Override
        public String getDeviceId() throws RemoteException {
            return PlatformService.getDeviceId().toString();
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        void makeToast(final String str) {
            new Handler(getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
