package kr.ac.kaist.resl.cmsp.iotapp.engine.abstraction.scan;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import kr.ac.kaist.resl.cmsp.iotapp.engine.abstraction.IdResolution;
import kr.ac.kaist.resl.cmsp.iotapp.library.ThingServiceEndpoint;
import kr.ac.kaist.resl.cmsp.iotapp.library.ThingServiceInfo;
import kr.ac.kaist.resl.cmsp.iotapp.library.service.general.ColorControlService;
import kr.ac.kaist.resl.cmsp.iotapp.library.service.general.OnOffService;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Scanner;

/**
 * Adopted from Cheapcast
 *
 */
public class UpnpScanStrategy implements DeviceScanStrategy {
    private static final int UPNP_SCAN_PERIOD = 5000;
    private static final int UPNP_SCAN_PERIOD_SEC = UPNP_SCAN_PERIOD / 1000;
    public List<ThingServiceEndpoint> scannedDevices = null;
    private ScanFinishedCallback callback;

    private final static String TAG = "DeviceUpnpScan";
    private Context appContext;
    Handler mHandler_upnp;
    Boolean mScanning = false;

    public UpnpScanStrategy (Context context) {
        this.appContext = context;
        mNetIf = getActiveNetworkInterface();
        this.mHandler_upnp = new Handler();
    }

    @Override
    public void registerCallback(ScanFinishedCallback callback) {

    }

    @Override
    public void init() {

    }

    @Override
    public synchronized void startScan(int period) {
        mScanning = true;
        mHandler_upnp.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mScanning == true) {
                    try {
                        mMulticastSocket = new MulticastSocket(1900);
                        mMulticastSocket.setLoopbackMode(true);
                        mMulticastSocket.joinGroup(mMulticastGroupAddress, mNetIf);

                        mUnicastSocket = new DatagramSocket(null);
                        mUnicastSocket.setReuseAddress(true);
                        mUnicastSocket.bind(new InetSocketAddress(getLocalV4Address(mNetIf),1900));

                    } catch (IOException e) {
                        Log.e(LOG_TAG, "Setup SSDP failed.", e);
                    }
                    while(mScanning) {
                        DatagramPacket dp = null;
                        try {
                            dp = receive();

                            String startLine = parseStartLine(dp);
                            if(startLine.equals(SL_MSEARCH)) {
                                String st = parseHeaderValue(dp, ST);

                                if(st.contains("dial-multiscreen-org:service:dial:1")) {

                                    String responsePayload = "HTTP/1.1 200 OK\n" +
                                            "ST: urn:dial-multiscreen-org:service:dial:1\n"+
                                            "HOST: 239.255.255.250:1900\n"+
                                            "EXT:\n"+
                                            "CACHE-CONTROL: max-age=1800\n"+
                                            "LOCATION: http://"+getLocalV4Address(mNetIf).getHostAddress()+":8008/ssdp/device-desc.xml\n" +
                                            "CONFIGID.UPNP.ORG: 7339\n" +
                                            "BOOTID.UPNP.ORG: 7339\n" +
                                            "USN: uuid:"+ "(DUMMY)"+"\n\n";


                                    DatagramPacket response = new DatagramPacket(responsePayload.getBytes(), responsePayload.length(), new InetSocketAddress(dp.getAddress(),dp.getPort()));
                                    mUnicastSocket.send(response);

                                    //Log.d(LOG_TAG, "Responding to "+ dp.getAddress().getHostAddress());
                                }
                            }
                        } catch (IOException e) {
                            Log.e(LOG_TAG, "SSDP fail.", e);
                        }
                    }
                    Log.e(LOG_TAG, "SSDP shutdown.");
                }
            }
        }, 2000);
        mHandler_upnp.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mScanning == true) {
                    stopScan();
                }
            }
        }, UPNP_SCAN_PERIOD);

    }

    @Override
    public void stopScan() {
        mScanning = false;
        callback.scanFinished(this);
    }

    @Override
    public List<ThingServiceEndpoint> getScannedDevices() {
        /*
        // FIXME: do id resolution
        scannedDevices = new ArrayList<ThingServiceEndpoint>();
        for (Device device : UpnpConnectivityProvider.getInstance(appContext).getScannedDevices()) {
            // FIXME: get service list from IdResolution module
            String[] testBleString = {ColorControlService.class.getSimpleName(), OnOffService.class.getSimpleName()};
            String bundleLoc = IdResolution.INSTANCE.getBundleLocation(device.getIdentity().getUdn().toString() + "");
            if (bundleLoc == null) {
                Log.d(TAG, "Failed to resolve Id " + device.getIdentity().getUdn().toString());
            } else {
                ThingServiceEndpoint endpoint = new ThingServiceEndpoint(
                        ThingServiceInfo.DEVICEFRAMEWORK_UPNP, device.getIdentity().getUdn().toString(), device.getIdentity().getUdn().toString(),
                        device.getDisplayString(), testBleString);
                endpoint.setEndpoint(device.getDetails().getBaseURL().toString());
                scannedDevices.add(endpoint);
            }
        }
        */
        return scannedDevices;


    }

    @Override
    public boolean isScanning() {
        return mScanning;
    }


    /**
     * Default IPv4 multicast address for SSDP messages
     */
    public static final String ADDRESS = "239.255.255.250";

    public static final String IPV6_LINK_LOCAL_ADDRESS = "FF02::C";
    public static final String IPV6_SUBNET_ADDRESS = "FF03::C";
    public static final String IPV6_ADMINISTRATIVE_ADDRESS = "FF04::C";
    public static final String IPV6_SITE_LOCAL_ADDRESS = "FF05::C";
    public static final String IPV6_GLOBAL_ADDRESS = "FF0E::C";

    public static final String ST = "ST";
    public static final String LOCATION = "LOCATION";
    public static final String NT = "NT";
    public static final String NTS = "NTS";

    /* Definitions of start line */
    public static final String SL_NOTIFY = "NOTIFY * HTTP/1.1";
    public static final String SL_MSEARCH = "M-SEARCH * HTTP/1.1";
    public static final String SL_OK = "HTTP/1.1 200 OK";

    /* Definitions of notification sub type */
    public static final String NTS_ALIVE = "ssdp:alive";
    public static final String NTS_BYEBYE = "ssdp:byebye";
    public static final String NTS_UPDATE = "ssdp:update";

    public static final String LOG_TAG = "SSDP";

    private SocketAddress mMulticastGroupAddress = new InetSocketAddress("239.255.255.250", 1900);
    private MulticastSocket mMulticastSocket;
    private DatagramSocket mUnicastSocket;

    private NetworkInterface mNetIf;

    private Context mContext;


    public static NetworkInterface getActiveNetworkInterface() {

        Enumeration<NetworkInterface> interfaces = null;
        try {
            interfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            return null;
        }

        while (interfaces.hasMoreElements()) {
            NetworkInterface iface = interfaces.nextElement();
            Enumeration<InetAddress> inetAddresses = iface.getInetAddresses();

            /* Check if we have a non-local address. If so, this is the active
             * interface.
             *
             * This isn't a perfect heuristic: I have devices which this will
             * still detect the wrong interface on, but it will handle the
             * common cases of wifi-only and Ethernet-only.
             */
            while (inetAddresses.hasMoreElements()) {
                InetAddress addr = inetAddresses.nextElement();

                if (!(addr.isLoopbackAddress() || addr.isLinkLocalAddress())) {
                    return iface;
                }
            }
        }

        return null;
    }

    public synchronized void shutdown() {
        mScanning = false;
    }

    private DatagramPacket receive() throws IOException {
        byte[] buf = new byte[1024];
        DatagramPacket dp = new DatagramPacket(buf, buf.length);
        mMulticastSocket.receive(dp);

        return dp;
    }

    private String parseHeaderValue(String content, String headerName) {
        Scanner s = new Scanner(content);
        s.nextLine(); // Skip the start line

        while (s.hasNextLine()) {
            String line = s.nextLine();
            int index = line.indexOf(':');
            String header = line.substring(0, index);
            if (headerName.equalsIgnoreCase(header.trim())) {
                return line.substring(index + 1).trim();
            }
        }

        return null;
    }

    private String parseHeaderValue(DatagramPacket dp, String headerName) {
        return parseHeaderValue(new String(dp.getData()), headerName);
    }

    private String parseStartLine(String content) {
        Scanner s = new Scanner(content);
        return s.nextLine();
    }

    private String parseStartLine(DatagramPacket dp) {
        return parseStartLine(new String(dp.getData()));
    }

    public static InetAddress getLocalV4Address(NetworkInterface netif)
    {
        Enumeration addrs = netif.getInetAddresses();
        while (addrs.hasMoreElements())
        {
            InetAddress addr = (InetAddress) addrs.nextElement();
            if (addr instanceof Inet4Address && !addr.isLoopbackAddress())
                return addr;
        }
        return null;
    }

}
