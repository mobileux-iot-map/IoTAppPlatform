package kr.ac.kaist.resl.cmsp.iotapp.engine.clustering;

import android.content.Context;
import kr.ac.kaist.resl.cmsp.iotapp.engine.PlatformException;
import kr.ac.kaist.resl.cmsp.iotapp.library.ThingServiceEndpoint;
import kr.ac.kaist.resl.cmsp.iotapp.library.ThingServiceInfo;
import kr.ac.kaist.resl.cmsp.iotapp.library.invocation.MethodInvocationJsonObject;
import kr.ac.kaist.resl.cmsp.iotapp.platform.PlatformService;
import org.alljoyn.bus.*;
import org.alljoyn.bus.alljoyn.DaemonInit;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by shheo on 15. 4. 19.
 */
public class AllJoynClusteringModule implements BusObject, ClusteringModule, AllJoynClusterBusInterface {
    static {
        System.loadLibrary("alljoyn_java");
    }

    final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());
    private static final String SERVICE_NAME = AllJoynClusteringModule.class.getName();
    private static String WELLKNOWN_NAME;
    private static final String SERVICE_PATH = "/" + AllJoynClusteringModule.class.getSimpleName();
    private static final String APP_NAME = PlatformService.APP_NAME;
    private static final short CONTACT_PORT = 63;
    public static final int MAX_INVOCATION_SIZE = (int) Math.pow(2, 17) - (int) Math.pow(2, 16);
    private BusListener busListener;
    private AboutListener aboutListener;
    private Context context;
    BusAttachment busAttachment;
    boolean isConnected = false;
    boolean isProvider = false;
    int providerSessionId = 0;
    // busObjectMap is initialized as ConcurrentHashMap for thread-safety
    private Map<String, AllJoynClusterBusInterface> busObjectMap;
    private Map<String, String> deviceIdMap;
    private Map<Integer, String[]> longInvocationMap;

    public AllJoynClusteringModule() {
        longInvocationMap = new HashMap<>();
    }

    private void addBusObject(final String busName, final int sessionId) {
        ProxyBusObject proxyObj = busAttachment.getProxyBusObject(
                busName,
                SERVICE_PATH,
                sessionId,
                new Class[]{AllJoynClusterBusInterface.class});
        final AllJoynClusterBusInterface clusterObj = proxyObj.getInterface(AllJoynClusterBusInterface.class);
        synchronized (busObjectMap) { // for debug
            busObjectMap.put(busName, clusterObj);
            printBusObjectMap();
        }
        (new Thread(new Runnable() {
            @Override
            public void run() {
                String busNameLocal = busName;
                AllJoynClusterBusInterface clusterObjLocal = busObjectMap.get(busNameLocal);
                try { // FIXME: change to handle callback
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                logger.debug("deviceid is registered: " + clusterObjLocal.getDeviceId());
                deviceIdMap.put(clusterObjLocal.getDeviceId(), busNameLocal);
                notifyAllAvailableServices(busNameLocal);
            }
        })).start();
    }

    private void removeBusObject(String busName) {
        synchronized (busObjectMap) { // for debug
            busObjectMap.remove(busName);
            String deviceId = null;
            for (Map.Entry<String, String> entry : deviceIdMap.entrySet()) {
                if (entry.getValue() == busName) {
                    deviceId = entry.getKey();
                }
            }
            if (deviceId != null)
                deviceIdMap.remove(deviceId);
            printBusObjectMap();
        }
    }

    // For debug
    private void printBusObjectMap() {
        StringBuilder builder = new StringBuilder();
        builder.append("busObjectMap is updated, ");
        builder.append(busObjectMap.size() + " bus objects: ");
        for (String key : busObjectMap.keySet()) {
            builder.append(key + ", ");
        }
        logger.debug(builder.toString());
    }

    @Override
    public void notifyAvailableServiceAdded(ThingServiceEndpoint serviceInfo) {
        ThingServiceInfo simpleInfo = serviceInfo;
        List<String> busToRemove = new ArrayList<>();
        for (Map.Entry<String, AllJoynClusterBusInterface> entry : busObjectMap.entrySet()) {
            AllJoynClusterBusInterface clusterBus = entry.getValue();
            try {
                clusterBus.addAvailableService(serviceInfo.toString());
            } catch (Exception e) {
                logger.error("Failed to add available service to device " + PlatformService.getDeviceId().toString() +
                        ". Removing it from local registry.", e);
                busToRemove.add(entry.getKey());
            }
        }
        for (String busName : busToRemove) {
            // FIXME: remove deviceIdMap together, or find other way
            busObjectMap.remove(busName);
        }
    }

    @Override
    public void notifyAvailableServiceRemoved(String thingId) {
        List<String> busToRemove = new ArrayList<>();
        for (Map.Entry<String, AllJoynClusterBusInterface> entry : busObjectMap.entrySet()) {
            AllJoynClusterBusInterface clusterBus = entry.getValue();
            try {
                clusterBus.removeAvailableService(thingId);
            } catch (Exception e) {
                logger.error("Failed to remove available service to device " + PlatformService.getDeviceId().toString() +
                        ". Removing it from local registry.", e);
                busToRemove.add(entry.getKey());
            }
            for (String busName : busToRemove) {
                // FIXME: remove deviceIdMap together, or find other way
                busObjectMap.remove(busName);
            }
        }
    }

    @Override
    public void sendServiceInvocation(String thingId, String invocation) {
        ThingServiceInfo info = PlatformService.getAbstractionModule().getAvailableService(thingId);
        String deviceId = info.getDeviceId();
        String busName = deviceIdMap.get(deviceId);
        AllJoynClusterBusInterface obj = busObjectMap.get(busName);
        if (obj == null) {
            logger.error("Failed to find device " + deviceId + " from busObjectMap");
        } else {
            logger.debug("sendServiceInvocation: deviceId: " + deviceId + ", thingId: " + thingId + ", busName: " + busName);
            try {
                if (invocation.length() > MAX_INVOCATION_SIZE) {
                    int curr = 0;
                    int index = 0;
                    int count = invocation.length() / MAX_INVOCATION_SIZE + 1;
                    while (true) {
                        if (invocation.length() - curr > MAX_INVOCATION_SIZE) {
                            obj.handleLongServiceInvocation(thingId, invocation.substring(curr, curr + MAX_INVOCATION_SIZE), hashCode(), index++, count);
                            curr += MAX_INVOCATION_SIZE;
                        } else {
                            obj.handleLongServiceInvocation(thingId, invocation.substring(curr), hashCode(), index++, count);
                            break;
                        }
                    }
                    if (index != count) {
                        logger.debug("Index and count are different. There should be some problem");
                    }
                } else {
                    obj.handleServiceInvocation(thingId, invocation);
                }
            } catch (Exception e) {
                logger.error("Failed to invoke remote object in device " + deviceId, e);
            }
        }
    }

    @Override
    public String sendServiceInvocationWithReturn(String thingId, String invocation) {
        ThingServiceInfo info = PlatformService.getAbstractionModule().getAvailableService(thingId);
        String deviceId = info.getDeviceId();
        String busName = deviceIdMap.get(deviceId);
        AllJoynClusterBusInterface obj = busObjectMap.get(busName);
        logger.debug("sendServiceInvocationWithReturn: deviceId: " + deviceId + "thingId: " + thingId + ", busName: " + busName);
        String toReturn = "";
        try {
            if (invocation.length() > MAX_INVOCATION_SIZE) {
                int curr = 0;
                int index = 0;
                int count = invocation.length() / MAX_INVOCATION_SIZE + 1;
                while (true) {
                    if (invocation.length() - curr > MAX_INVOCATION_SIZE) {
                        obj.handleLongServiceInvocationWithReturn(thingId, invocation.substring(curr, curr + MAX_INVOCATION_SIZE), hashCode(), index++, count);
                        curr += MAX_INVOCATION_SIZE;
                    } else {
                        toReturn = obj.handleLongServiceInvocationWithReturn(thingId, invocation.substring(curr), hashCode(), index++, count);
                        break;
                    }
                }
                if (index != count) {
                    logger.debug("Index and count are different. There should be some problem");
                }
            } else {
                toReturn = obj.handleServiceInvocationWithReturn(thingId, invocation);
            }
        } catch (Exception e) {
            logger.error("Failed to invoke remote object in device " + deviceId, e);
        }
        return toReturn;
    }

    @Override
    public void notifyAllAvailableServices(String busName) {
        AllJoynClusterBusInterface clusterBus = busObjectMap.get(busName);
        if (clusterBus != null) {
            logger.debug("notifyAllAvailableServices: busName: " + busName + ", deviceId: " + clusterBus.getDeviceId());
            List<ThingServiceInfo> serviceList = PlatformService.getAbstractionModule().getAvailableServicesAll();
            for (ThingServiceInfo info : serviceList) {
                clusterBus.addAvailableService(info.toString());
            }
        } else {
            logger.error("notifyAllAvailableServices: failed to find busObject from busObjectMap, busName: " + busName);
        }
    }

    @Override
    public int openSessionAndAdvertise() {
        try {
            Status status;
            Mutable.ShortValue contactPort = new Mutable.ShortValue(CONTACT_PORT);
            SessionOpts sessionOpts = new SessionOpts();
            sessionOpts.traffic = SessionOpts.TRAFFIC_MESSAGES;
            sessionOpts.isMultipoint = true;
            sessionOpts.proximity = SessionOpts.PROXIMITY_ANY;
            sessionOpts.transports = SessionOpts.TRANSPORT_TCP;

            int flags = BusAttachment.ALLJOYN_NAME_FLAG_ALLOW_REPLACEMENT; //no request name flags
            logger.debug("Requesting Well-known name from BusAttachment: " + WELLKNOWN_NAME);
            status = busAttachment.requestName(WELLKNOWN_NAME, flags);
            if (status != Status.OK) {
                //logger.error("BusAttachment.requestName failed: " + status);
                throw new PlatformException("BusAttachment.requestName failed: " + status);
            }

            logger.debug("Binding to session port: " + contactPort.value);
            status = busAttachment.bindSessionPort(contactPort, sessionOpts,
                    new CmSessionPortListener());
            if (status != Status.OK) {
                //logger.error("BusAttachment.bindSessionPort() failed: " + status);
                busAttachment.releaseName(WELLKNOWN_NAME);
                throw new PlatformException("BusAttachment.bindSessionPort() failed: " + status);
            }

            logger.debug("Advertising name: " + WELLKNOWN_NAME);
            //status = busAttachment.advertiseName(WELLKNOWN_NAME, SessionOpts.TRANSPORT_ANY);
            status = busAttachment.advertiseName(WELLKNOWN_NAME, SessionOpts.TRANSPORT_TCP);
            if (status != Status.OK) {
                //logger.error("BusAttachment.findAdvertisedName() failed: " + status);
                busAttachment.unbindSessionPort(CONTACT_PORT);
                busAttachment.releaseName(WELLKNOWN_NAME);
                throw new PlatformException("BusAttachment.findAdvertisedName() failed: " + status);
            }
        } catch (PlatformException e) {
            logger.error("Failed to open session and advertise", e);
            return -1;
        }
        isProvider = true;
        return 0;
    }

    @Override
    public int stopAdvertiseAndCloseSession() {
        busAttachment.cancelAdvertiseName(WELLKNOWN_NAME, SessionOpts.TRANSPORT_ANY);
        busAttachment.unbindSessionPort(CONTACT_PORT);
        busAttachment.releaseName(WELLKNOWN_NAME);
        isProvider = false;
        return 0;
    }

    public void init(Context _appContext) {
        this.context = _appContext;
        busObjectMap = new ConcurrentHashMap<>();
        deviceIdMap = new ConcurrentHashMap<>();
        WELLKNOWN_NAME = SERVICE_NAME + "._" + PlatformService.getAppId().toString().replace('-', '_');
    }

    @Override
    public void start() throws PlatformException {
        if (DaemonInit.PrepareDaemon(context) == false) {
            //logger.error("Failed to prepare AllJoyn daemon");
            throw new PlatformException("Failed to prepare AllJoyn daemon");
        }
        busAttachment = new BusAttachment(APP_NAME, BusAttachment.RemoteMessage.Receive);
        busListener = new CmBusListener();
        aboutListener = new CmAboutListener();
        busAttachment.registerBusListener(busListener);
        busAttachment.registerAboutListener(aboutListener);


        logger.debug("Registering bus object to BusAttachment...");
        Status status = busAttachment.registerBusObject(this, SERVICE_PATH);
        if (Status.OK != status) {
            //logger.error("BusAttachment.registerBusObject() failed: " + status);
            throw new PlatformException("BusAttachment.registerBusObject() failed: " + status);
        }
        logger.debug("Connecting BusAttachment to router...");
        status = busAttachment.connect();
        if (status != Status.OK) {
            //logger.error("BusAttachment.connect() failed: " + status);
            throw new PlatformException("BusAttachment.connect() failed: " + status);
        }

        logger.debug("Registering service name to interest list: " + SERVICE_NAME);
        status = busAttachment.findAdvertisedName(SERVICE_NAME);
        if (status != Status.OK) {
            //logger.error("BusAttachment.findAdvertisedName() failed: " + status);
            throw new PlatformException("BusAttachment.findAdvertisedName() failed: " + status);
        }
    }

    public void stop() {
        if (isProvider == true) {
            stopAdvertiseAndCloseSession();
            isProvider = false;
            //busAttachment.leaveSession(0);
        }
        if (providerSessionId != 0) {
            Status status = busAttachment.leaveSession(providerSessionId);
            if (status != Status.OK) {
                logger.error("Failed to leave session: " + status);
            }
            providerSessionId = 0;
        }
        busAttachment.unregisterBusListener(busListener);
        busAttachment.unregisterAboutListener(aboutListener);
        busAttachment.unregisterBusObject(this);
        isConnected = false;
        busObjectMap.clear();
        deviceIdMap.clear();
    }

    // Methods for AllJoynClusterBusInterface
    @Override
    public void handleServiceInvocation(String thingId, String serviceInvocation) {
        logger.debug("handleServiceInvocation: thingId:" + thingId + ", invocation: " + serviceInvocation);
        try {
            PlatformService.getAbstractionModule().invokeLocalService(new MethodInvocationJsonObject(serviceInvocation));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handleLongServiceInvocation(String thingId, String serviceInvocationChunk, int id, int index, int count) {
        if (index == 0) {
            longInvocationMap.put(id, new String[count]);
        }
        longInvocationMap.get(id)[index] = serviceInvocationChunk;
        if (index == count - 1) {
            String assembled = "";
            for (String chunk : longInvocationMap.get(id)) {
                assembled += chunk;
            }
            logger.debug("Assembled all chunks: " + assembled);
            handleServiceInvocation(thingId, assembled);
            longInvocationMap.remove(id);
        }
    }

    @Override
    public String handleServiceInvocationWithReturn(String thingId, String serviceInvocation) {
        logger.debug("handleServiceInvocation: thingId:" + thingId + ", invocation: " + serviceInvocation);
        Object toReturn = null;
        try {
            toReturn = PlatformService.getAbstractionModule().invokeLocalService(new MethodInvocationJsonObject(serviceInvocation));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (toReturn != null) {
            return toReturn.toString();
        } else {
            return null;
        }
    }

    @Override
    public String handleLongServiceInvocationWithReturn(String thingId, String serviceInvocationChunk, int id, int index, int count) {
        if (index == 0) {
            longInvocationMap.put(id, new String[count]);
        }
        longInvocationMap.get(id)[index] = serviceInvocationChunk;
        String toReturn = "";
        if (index == count - 1) {
            String assembled = "";
            for (String chunk : longInvocationMap.get(id)) {
                assembled += chunk;
            }
            logger.debug("Assembled all chunks: " + assembled);
            toReturn = handleServiceInvocationWithReturn(thingId, assembled);
            longInvocationMap.remove(id);
        }
        return toReturn;
    }

    @Override
    public String getAvailableServiceList(String interfaces, boolean scanNow) {
        // FIXME: Not used currently, but need to impl later
        /*
        try {
            JSONObject interfaceList = new JSONObject(interfaces);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        PlatformService.getAbstractionModule().getAvailableServices(new HashSet<String>(interfaces));
        */
        return null;
    }

    @Override
    public int addAvailableService(String serviceInfo) {
        try {
            ThingServiceInfo info = new ThingServiceInfo(new JSONObject(serviceInfo));
            PlatformService.getAbstractionModule().addAvailableService(info);
        } catch (JSONException e) {
            e.printStackTrace();
            return -1;
        }
        return 0;
    }

    @Override
    public int removeAvailableService(String thingId) {
        PlatformService.getAbstractionModule().removeAvailableService(thingId);
        return 0;
    }

    @Override
    public String getDeviceId() {
        return PlatformService.getDeviceId().toString();
    }

    /**
     * This is for PROVIDER role in AllJoyn network
     * This listener is used to monitor newly joined CONSUMER to its bound session
     */
    private class CmSessionPortListener extends SessionPortListener {
        @Override
        public boolean acceptSessionJoiner(short sessionPort, String joiner, SessionOpts opts) {
            logger.debug("CmSessionPortListener:acceptSessionJoiner: joiner: " + joiner);
            if (sessionPort == CONTACT_PORT)
                return true;
            return super.acceptSessionJoiner(sessionPort, joiner, opts);
        }

        @Override
        public void sessionJoined(short sessionPort, int sessionId, String joiner) {
            logger.debug("CmSessionPortListener:sessionJoined: sessionId:" + sessionId + ", joiner: " + joiner);
            providerSessionId = sessionId;
            if (sessionPort == CONTACT_PORT) {
                AllJoynClusteringModule.this.addBusObject(joiner, sessionId);
            }
            super.sessionJoined(sessionPort, sessionId, joiner);
        }
    }

    /**
     * This is for CONSUMER role in AllJoyn Network
     * When a new CONSUMER joins to PROVIDER's session, it receives list of current CONSUMER.
     * Then the new CONSUMER notifies other CONSUMER that it is added and this listener is called.
     */
    private class CmSessionListener extends SessionListener {
        @Override
        public void sessionLost(int sessionId, int reason) {
            logger.debug("CmSessionListener:sessionLost: sessionId:" + sessionId + ", reason: " + reason);
            isConnected = false;
            super.sessionLost(sessionId, reason);
        }

        @Override
        public void sessionMemberAdded(int sessionId, String uniqueName) {
            logger.debug("CmSessionListener:sessionMemberAdded: sessionId:" + sessionId + ", name: " + uniqueName);
            AllJoynClusteringModule.this.addBusObject(uniqueName, sessionId);
            super.sessionMemberAdded(sessionId, uniqueName);
        }

        @Override
        public void sessionMemberRemoved(int sessionId, String uniqueName) {
            // TODO: If the removed member is PROVIDER, decide new PROVIDER among CONSUMERs and restart session
            logger.debug("CmSessionListener:sessionMemberRemoved: sessionId:" + sessionId + ", name: " + uniqueName);
            AllJoynClusteringModule.this.removeBusObject(uniqueName);
            isConnected = false;
            super.sessionMemberRemoved(sessionId, uniqueName);
        }
    }

    /**
     * This is for CONSUMER role in AllJoyn Network
     * This listener is called right after this CONSUMER joins session.
     * In this impl, parameter 'context' of onJoinSession() is String type unique name of the PROVIDER.
     */
    private class CmOnJoinSessionListener extends OnJoinSessionListener {
        @Override
        public void onJoinSession(Status status, int sessionId, SessionOpts opts, Object context) {
            if (status == Status.OK) {
                logger.debug("CmOnJoinSessionListener:onJoinSession:OK sessionId:" + sessionId + ", joiner: " + context);
                providerSessionId = sessionId;
                String name = (String) context;
                AllJoynClusteringModule.this.addBusObject(name, sessionId);
            } else {
                logger.debug("CmOnJoinSessionListener:onJoinSession:" + status + " sessionId:" + sessionId + ", joiner: " + context);
                //AllJoynClusteringModule.this.removeBusObject(sessionId);
                isConnected = false;
            }
            super.onJoinSession(status, sessionId, opts, context);
        }
    }

    // Bus Listener
    private class CmBusListener extends BusListener {
        private final Object lock = new Object();

        @Override
        public void foundAdvertisedName(String name, short transport, String namePrefix) {
            logger.debug("foundAdvertisedName: foundName: " + name + ", transport: " + transport + ", namePrefix: " + namePrefix);
            synchronized (lock) {
                if (isConnected == false) {
                    if (!name.startsWith(SERVICE_NAME)) {
                        // Found service is not a ClusteringModule service
                        logger.error("foundAdvertisedName: service name does not match, foundName: " + name + ", serviceName: " + SERVICE_NAME);
                        return;
                    }
                    if (name.equalsIgnoreCase(WELLKNOWN_NAME)) {
                        // Found service is advertised by this device
                        logger.debug("foundAdvertisedName: service is advertised by this device, foundName: " + name + ", wellKnownName: " + WELLKNOWN_NAME);
                        return;
                    }
                    SessionOpts sessionOpts = new SessionOpts();
                    sessionOpts.transports = transport;

                    busAttachment.joinSession(name, CONTACT_PORT, sessionOpts,
                            new CmSessionListener(), new CmOnJoinSessionListener(),
                            name);
                    isConnected = true;
                }
            }
            super.foundAdvertisedName(name, transport, namePrefix);
        }
    }

    // About Listener
    private class CmAboutListener implements AboutListener {
        @Override
        public void announced(String busName, int version, short port, AboutObjectDescription[] objectDescriptions, Map<String, Variant> aboutData) {

        }
    }
}
