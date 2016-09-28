package kr.ac.kaist.resl.cmsp.iotapp.engine.abstraction.scan;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;
import kr.ac.kaist.resl.cmsp.iotapp.engine.abstraction.IdResolution;
import kr.ac.kaist.resl.cmsp.iotapp.library.ThingServiceEndpoint;
import kr.ac.kaist.resl.cmsp.iotapp.library.ThingServiceInfo;
import kr.ac.kaist.resl.cmsp.iotapp.library.service.general.AccelService;
import kr.ac.kaist.resl.cmsp.iotapp.library.service.general.ButtonService;
import kr.ac.kaist.resl.cmsp.iotapp.library.service.general.GestureService;
import kr.ac.kaist.resl.cmsp.iotapp.library.service.general.HeartRateService;
import kr.ac.kaist.resl.cmsp.iotapp.platform.PlatformService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by shheo on 15. 7. 8.
 */
public class BleScanStrategy implements DeviceScanStrategy {
    private final static String TAG = BleScanStrategy.class.getSimpleName();
    public List<ThingServiceEndpoint> scannedDevices = null;
    private ScanFinishedCallback callback;

    /**
     * members for bluetooth scan *
     */
    private static final long BLE_SCAN_PERIOD = 10000;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning; // scanning flag for ble
    private Handler mHandler_Ble;
    private ArrayList<String> mArrayAdapter = new ArrayList<String>();
    private Context context;

    public BleScanStrategy(Context context) {
        this.context = context;
        this.mHandler_Ble = new Handler();
    }


    @Override
    public void registerCallback(ScanFinishedCallback callback) {

    }

    @Override
    public void init() {
        Log.d(TAG, "Initializing scanner");
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(context, "BLE is not supported", Toast.LENGTH_SHORT).show();
        }
        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);

        mBluetoothAdapter = bluetoothManager.getAdapter();

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Log.d("BluetoothAdapter", "Bluetooth is not supported.");
        }
    }

    @Override
    public void startScan(int period) {
        Log.d(TAG, "Starting scan");
        if (mScanning) {
            Log.d(TAG, "Already scanning. Stopping current one...");
            stopScan();
        }

        scannedDevices = new ArrayList<ThingServiceEndpoint>();

        mHandler_Ble.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mScanning == true) {
                    stopScan();
                }
            }
        }, period);

        mScanning = true;

        //mBluetoothAdapter.startDiscovery();
        mBluetoothAdapter.startLeScan(mLeScanCallback);
    }

    @Override
    public void stopScan() {
        Log.d(TAG, "Stopping scan");
        mScanning = false;
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            Log.d(TAG, "onLeScan: " + device.toString());
            String bundleLoc = "TestLoc"; //IdResolution.INSTANCE.getBundleLocation(device.getAddress());
            if (bundleLoc == null) {
                Log.d(TAG, "Failed to resolve Id " + device.getAddress() + " of device " + device.toString());
            } else {
                // FIXME: get service list from IdResolution module
                String[] testBleString = {ButtonService.class.getSimpleName(), GestureService.class.getSimpleName(), AccelService.class.getSimpleName(), HeartRateService.class.getSimpleName()};
                ThingServiceEndpoint endpoint = new ThingServiceEndpoint(
                        ThingServiceInfo.DEVICEFRAMEWORK_BLE, PlatformService.getDeviceId().toString(), device.getAddress(),
                        device.getName(), Arrays.asList(testBleString));
                endpoint.setEndpoint(device.getAddress());
                scannedDevices.add(endpoint);
            }
        }


    };

    @Override
    public List<ThingServiceEndpoint> getScannedDevices() {
        return scannedDevices;
    }

    @Override
    public boolean isScanning() {
        return mScanning;
    }
}
