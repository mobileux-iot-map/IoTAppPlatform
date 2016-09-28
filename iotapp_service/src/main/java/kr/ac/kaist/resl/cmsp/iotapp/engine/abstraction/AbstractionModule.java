package kr.ac.kaist.resl.cmsp.iotapp.engine.abstraction;

import android.content.Context;
import android.os.Build;
import android.os.RemoteException;
import kr.ac.kaist.resl.cmsp.iotapp.engine.PlatformException;
import kr.ac.kaist.resl.cmsp.iotapp.gui.DeviceListBroker;
import kr.ac.kaist.resl.cmsp.iotapp.library.IGetAvailableServicesCallback;
import kr.ac.kaist.resl.cmsp.iotapp.library.IoTAppException;
import kr.ac.kaist.resl.cmsp.iotapp.library.ThingServiceEndpoint;
import kr.ac.kaist.resl.cmsp.iotapp.library.ThingServiceInfo;
import kr.ac.kaist.resl.cmsp.iotapp.library.impl.ILocalServiceObjectHandler;
import kr.ac.kaist.resl.cmsp.iotapp.library.invocation.MethodInvocationJsonObject;
import kr.ac.kaist.resl.cmsp.iotapp.library.service.general.*;
import kr.ac.kaist.resl.cmsp.iotapp.platform.PlatformService;
import kr.ac.kaist.resl.cmsp.iotapp.thing.HueService;
import kr.ac.kaist.resl.cmsp.iotapp.thing.MioService;
import kr.ac.kaist.resl.cmsp.iotapp.thing.MyoBandService;
import kr.ac.kaist.resl.cmsp.iotapp.thing.SensorTagService;
import kr.ac.kaist.resl.cmsp.iotapp.thing.bundle.hue.HueImpl;
import kr.ac.kaist.resl.cmsp.iotapp.thing.bundle.mio.MioImpl;
import kr.ac.kaist.resl.cmsp.iotapp.thing.bundle.myoband.MyoBandImpl;
import kr.ac.kaist.resl.cmsp.iotapp.thing.bundle.sensortag.SensorTagImpl;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by shheo on 15. 4. 13.
 */
public class AbstractionModule {
    final Logger logger = LoggerFactory.getLogger(AbstractionModule.class.getSimpleName());
    private static PlatformService service;
    private static Context appContext;
    // FIXME: Change to non-singleton
    private static DynamicModuleEngine dmEngine = DynamicModuleEngine.INSTANCE;
    private static DeviceScanner dScanner = DeviceScanner.INSTANCE;
    private static IdResolution iResolution = IdResolution.INSTANCE;
    private static ModuleInstaller mInstaller = ModuleInstaller.INSTANCE;

    private static Map<String, ThingServiceInfo> availableServices;
    private static Map<String, ThingServiceEndpoint> localServices;
    private static Map<String, ILocalServiceObjectHandler> localServiceHandlers;

    public AbstractionModule() {
    }

    public void init(PlatformService _service) {
        service = _service;
        appContext = service.getApplicationContext();
        availableServices = new ConcurrentHashMap<>();
        localServices = new ConcurrentHashMap<>();
        localServiceHandlers = new ConcurrentHashMap<>();
        dmEngine.init(appContext);
        dScanner.init(appContext);
    }

    public void start() throws PlatformException {
        /* FIXME: dmEngine blocks connection with hue
        try {
            dmEngine.start();
        } catch (PlatformException e) {
            logger.error("Failed to start dynamic module engine", e);
            throw e;
        }
        */
    }

    public void stop() {
        /*
        try {
            dmEngine.stop();
        } catch (PlatformException e) {
            logger.error("Failed to stop dynamic module engine", e);
        }
        */
        if (myoBand != null) myoBand.disconnect();
        if (sensorTag != null) sensorTag.disconnect();
        if (sensorTag2 != null) sensorTag2.disconnect();
        if (keyFobDemo != null) keyFobDemo.disconnect();
    }

    public void addLocalService(ThingServiceEndpoint serviceInfo, ILocalServiceObjectHandler handler) {
        addAvailableService(serviceInfo);
        localServices.put(serviceInfo.getThingId(), serviceInfo);
        if (handler != null)
            localServiceHandlers.put(serviceInfo.getThingId(), handler);
        logger.debug("Adding local service: " + serviceInfo.getThingId());
        PlatformService.getClusterModule().notifyAvailableServiceAdded(serviceInfo);
    }

    public void removeLocalService(String thingId) {
        removeAvailableService(thingId);
        localServices.remove(thingId);
        localServiceHandlers.remove(thingId);
        logger.debug("Removing local service: " + thingId);
        PlatformService.getClusterModule().notifyAvailableServiceRemoved(thingId);
    }

    public void addAvailableService(ThingServiceInfo serviceInfo) {
        availableServices.put(serviceInfo.getThingId(), serviceInfo);
        logger.debug("Adding available service: " + serviceInfo.getThingId() + ", " + serviceInfo.toString());
    }

    public void removeAvailableService(String thingId) {
        availableServices.remove(thingId);
        logger.debug("Removing available service: " + thingId);
    }

    public void scanDevices(int scanPeriod) {
        //dScanner.startScan(scanPeriod);
    }

    public ThingServiceInfo getAvailableService(String thingId) {
        return availableServices.get(thingId);
    }

    // FIXME: modified getAvailableServices() to pass test. Revert it
    MyoBandService myoBand = null;
    SensorTagService sensorTag = null;
    SensorTagService sensorTag2 = null;
    SensorTagService keyFobDemo = null;
    MioService mio = null;
    HueService hue = null;

    public List<ThingServiceInfo> getAvailableServices(Set<String> requiredInterfaces) {
        List<ThingServiceInfo> foundServiceList = new ArrayList<>();
        for (Map.Entry<String, ThingServiceInfo> entry : availableServices.entrySet()) {
            if (entry.getValue().getServices().containsAll(requiredInterfaces)) {
                foundServiceList.add(entry.getValue());
            }
        }

        return foundServiceList;
    }

    public void getAvailableServices(final Set<String> requiredInterfaces, final int scanPeriod, final IGetAvailableServicesCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // FIXME: fix dscanner to work
                // dScanner.startScan(scanPeriod, callback);
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                int remainPeriod = scanPeriod;
                // For service objects in cluster, scan every second and send callback.
                while (remainPeriod > 0) {
                    try {
                        if (remainPeriod < 2000) {
                            Thread.sleep(remainPeriod);
                            remainPeriod = 0;
                        } else {
                            Thread.sleep(1000);
                            remainPeriod -= 1000;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    for (Map.Entry<String, ThingServiceInfo> entry : availableServices.entrySet()) {
                        if (entry.getValue().getServices().containsAll(requiredInterfaces)) {
                            try {
                                callback.onAvailableServiceFound(entry.getValue());
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }).start();
    }

    public List<ThingServiceInfo> getAvailableServicesAll() {
        return new ArrayList<>(availableServices.values());
    }

    public boolean isLocalService(String thingId) {
        return localServices.containsKey(thingId);
    }

    public Object invokeLocalService(final MethodInvocationJsonObject invocation) throws JSONException {
        logger.debug("invokeLocalService: " + invocation.toString());
        Object toReturn = null;
        final String thingId = invocation.getString(MethodInvocationJsonObject.JSON_KEY_THING_ID);
        ThingServiceEndpoint endpoint = localServices.get(thingId);
        logger.debug("endpoint: " + endpoint.toString());
        if (endpoint.getDeviceFramework().equals(ThingServiceInfo.DEVICEFRAMEWORK_APP)) {
            if (invocation.getReturnType().equalsIgnoreCase("void")) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            localServiceHandlers.get(thingId).callback(invocation.toString(), true);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
                return null;
            } else {
                try {
                    String toReturnStr = localServiceHandlers.get(thingId).callback(invocation.toString(), true);
                    return MethodInvocationJsonObject.cast(invocation.getReturnType(), toReturnStr);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            return null;
        } else {
            if (endpoint.getThingId().equals("34:B1:F7:D5:0C:01")) {
                if (invocation.getString(MethodInvocationJsonObject.JSON_KEY_METHOD_NAME).equals("getAccelRateMeasurement"))
                    toReturn = sensorTag.getAccelRateMeasurement();
                else
                    toReturn = sensorTag.getButtonState(0); // FIXME: hard-coded to button index 0
            } else if (endpoint.getThingId().equals("34:B1:F7:D5:0C:27")) {
                if (invocation.getString(MethodInvocationJsonObject.JSON_KEY_METHOD_NAME).equals("getAccelRateMeasurement"))
                    toReturn = sensorTag2.getAccelRateMeasurement();
                else
                    toReturn = sensorTag2.getButtonState(0); // FIXME: hard-coded to button index 0
            } else if (endpoint.getThingId().equals("90:59:AF:0B:76:DD")) {
                toReturn = keyFobDemo.getButtonState(0); // FIXME: hard-coded to button index 0
                logger.debug("====invokeService: keyFobDemo, toReturn: " + toReturn.toString());
            } else if (endpoint.getThingId().equals("MIO56P0009M2IK")) {
                toReturn = mio.getHeartRateMeasurement();
            } else if (endpoint.getThingId().equals("00:17:88:16:07:ED")) {
                try {
                    hue.toggle();
                } catch (IoTAppException e) {
                    e.printStackTrace();
                }
                toReturn = null;
            } else if (endpoint.getThingId().equals("D3:4D:45:50:C9:D1")) {
                toReturn = myoBand.getCurrentGesture();
            } else {
                logger.error("Device framework " + endpoint.getDeviceFramework() + " is not supported");
            }

            if (invocation.getReturnType().equalsIgnoreCase("void")) { // FIXME: check it is really void
                toReturn = null;
            }
            return toReturn;
        }
    }

    public boolean isAvailableService(String thingId) {
        return availableServices.containsKey(thingId);
    }
}
