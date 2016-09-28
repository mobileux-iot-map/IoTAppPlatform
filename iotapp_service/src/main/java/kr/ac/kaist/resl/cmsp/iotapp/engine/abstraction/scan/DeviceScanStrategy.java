package kr.ac.kaist.resl.cmsp.iotapp.engine.abstraction.scan;

import kr.ac.kaist.resl.cmsp.iotapp.library.ThingServiceEndpoint;

import java.util.List;

/**
 * Created by shheo on 15. 4. 20.
 */
public interface DeviceScanStrategy {
    void registerCallback(ScanFinishedCallback callback);

    void init();

    void startScan(int period);

    void stopScan();

    List<ThingServiceEndpoint> getScannedDevices();

    boolean isScanning();
}
