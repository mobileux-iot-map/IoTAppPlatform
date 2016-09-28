package kr.ac.kaist.resl.cmsp.iotapp.engine.abstraction;

import android.content.Context;
import kr.ac.kaist.resl.cmsp.iotapp.engine.abstraction.scan.AntPlusScanStrategy;
import kr.ac.kaist.resl.cmsp.iotapp.engine.abstraction.scan.BleScanStrategy;
import kr.ac.kaist.resl.cmsp.iotapp.engine.abstraction.scan.DeviceScanStrategy;
import kr.ac.kaist.resl.cmsp.iotapp.library.IGetAvailableServicesCallback;
import kr.ac.kaist.resl.cmsp.iotapp.library.ThingServiceEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by shheo on 15. 4. 13.
 */
public class DeviceScanner {
    final Logger logger = LoggerFactory.getLogger(DeviceScanner.class.getSimpleName());
    public static final DeviceScanner INSTANCE = new DeviceScanner();
    private List<DeviceScanStrategy> ScanStrategies;
    boolean mScanning = false;

    private DeviceScanner() {
    }

    public void init(Context context) {
        ScanStrategies = new ArrayList<>();
        //ScanStrategies.add(new BleScanStrategy(context));
        //ScanStrategies.add(new UpnpScanStrategy(context));
        //ScanStrategies.add(new AntPlusScanStrategy(context));
    }

    // Blocked call
    public void startScan(final int scan_period, IGetAvailableServicesCallback callback) {
        logger.debug("Starting " + ScanStrategies.size() + " scanners for " + scan_period + " milliseconds");

        mScanning = true;
        for (final DeviceScanStrategy strategy : ScanStrategies) {
            logger.debug("Starting scanner " + strategy.getClass().getSimpleName());
            strategy.init();
            strategy.startScan(scan_period);
        }
/*
        try {
            Thread.sleep(scan_period + 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        stopScan();
        logger.debug("Scan is finished");
        */
    }

    public void stopScan() {
        for (DeviceScanStrategy strategy : ScanStrategies) {
            if (strategy.isScanning())
                strategy.stopScan();
        }
        mScanning = false;
    }

    public List<ThingServiceEndpoint> getScannedDevices() {
        List<ThingServiceEndpoint> scannedDevices = new ArrayList<>();
        for (DeviceScanStrategy strategy : ScanStrategies) {
            scannedDevices.addAll(strategy.getScannedDevices());
        }
        return scannedDevices;
    }
}
