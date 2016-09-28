package kr.ac.kaist.resl.cmsp.iotapp.engine.abstraction.scan;



import kr.ac.kaist.resl.cmsp.iotapp.library.ThingServiceEndpoint;

import java.util.List;

/**
 * Created by shheo on 15. 4. 20.
 */
public class AppScanStrategy implements DeviceScanStrategy {

    public AppScanStrategy() {
    }

    @Override
    public void registerCallback(ScanFinishedCallback callback) {

    }

    @Override
    public void init() {

    }

    @Override
    public void startScan(int period) {

    }

    @Override
    public void stopScan() {

    }

    @Override
    public List<ThingServiceEndpoint> getScannedDevices() {
        return null;
    }

    @Override
    public boolean isScanning() {
        return false;
    }
}
