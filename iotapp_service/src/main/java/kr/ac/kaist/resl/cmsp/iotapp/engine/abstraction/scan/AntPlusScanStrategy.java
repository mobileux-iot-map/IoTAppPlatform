package kr.ac.kaist.resl.cmsp.iotapp.engine.abstraction.scan;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;
import com.dsi.ant.plugins.antplus.pcc.MultiDeviceSearch;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceType;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import kr.ac.kaist.resl.cmsp.iotapp.engine.abstraction.IdResolution;
import kr.ac.kaist.resl.cmsp.iotapp.library.ThingServiceEndpoint;
import kr.ac.kaist.resl.cmsp.iotapp.library.ThingServiceInfo;
import kr.ac.kaist.resl.cmsp.iotapp.library.service.general.HeartRateService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

/**
 * Created by shheo on 15. 7. 8.
 */
public class AntPlusScanStrategy implements DeviceScanStrategy {
    private final static String TAG = AntPlusScanStrategy.class.getSimpleName();
    public List<ThingServiceEndpoint> scannedDevices = null;
    MultiDeviceSearch mSearch;
    private Handler mHandler_Antplus;
    private Context context;
    boolean mScanning = false;

    public AntPlusScanStrategy(Context context) {
        this.context = context;
        mHandler_Antplus = new Handler();
    }

    @Override
    public void registerCallback(ScanFinishedCallback callback) {

    }

    @Override
    public void init() {
        Log.d(TAG, "Initializing scanner");
    }

    @Override
    public void startScan(int period) {
        Log.d(TAG, "Starting scan");
        scannedDevices = new ArrayList<ThingServiceEndpoint>();

        mHandler_Antplus.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mScanning) {
                    stopScan();
                }
            }
        }, period);

        mScanning = true;
        @SuppressWarnings("unchecked")
        EnumSet<DeviceType> devices = EnumSet.noneOf(DeviceType.class);
        // TODO: add other devices to support
        devices.add(DeviceType.HEARTRATE);
        mSearch = new MultiDeviceSearch(context, devices, mCallback);

    }

    @Override
    public void stopScan() {
        Log.d(TAG, "Stopping scan");
        mSearch.close();
        mScanning = false;
    }

    @Override
    public List<ThingServiceEndpoint> getScannedDevices() {
        return scannedDevices;
    }

    @Override
    public boolean isScanning() {
        return mScanning;
    }

    /**
     * Callbacks from the multi-device search interface
     */
    private MultiDeviceSearch.SearchCallbacks mCallback = new MultiDeviceSearch.SearchCallbacks() {
        /**
         * Called when a device is found. Display found devices in connected and
         * found lists
         */
        public void onDeviceFound(final com.dsi.ant.plugins.antplus.pccbase.MultiDeviceSearch.MultiDeviceSearchResult deviceFound) {
            Log.d(TAG, "onDeviceFound: " + deviceFound.toString());
            String bundleLoc = IdResolution.INSTANCE.getBundleLocation(deviceFound.getAntDeviceNumber() + "");
            if (bundleLoc == null) {
                Log.d(TAG, "Failed to resolve Id " + deviceFound.getAntDeviceNumber() + " of device " + deviceFound.toString());
            } else {
                // FIXME: get service list from IdResolution module
                String[] testBleString = {HeartRateService.class.getSimpleName()};
                ThingServiceEndpoint endpoint = new ThingServiceEndpoint(
                        ThingServiceInfo.DEVICEFRAMEWORK_ANTPLUS, deviceFound.getAntDeviceNumber() + "", deviceFound.getAntDeviceNumber() + "",
                        deviceFound.getDeviceDisplayName(), Arrays.asList(testBleString));
                endpoint.setEndpoint(deviceFound.getAntDeviceNumber() + "");
                scannedDevices.add(endpoint);
            }
        }

        /**
         * The search has been stopped unexpectedly
         */
        public void onSearchStopped(RequestAccessResult reason) {
            Log.d(TAG, "onSearchStopped: " + reason.toString());
        }

        @Override
        public void onSearchStarted(MultiDeviceSearch.RssiSupport supportsRssi) {
            Log.d(TAG, "onSearchStarted");
            if (supportsRssi == MultiDeviceSearch.RssiSupport.UNAVAILABLE) {
                Toast.makeText(context, "Rssi information not available.", Toast.LENGTH_SHORT).show();
            } else if (supportsRssi == MultiDeviceSearch.RssiSupport.UNKNOWN_OLDSERVICE) {
                Toast.makeText(context, "Rssi might be supported. Please upgrade the plugin service.", Toast.LENGTH_SHORT).show();
            }
        }
    };
}
