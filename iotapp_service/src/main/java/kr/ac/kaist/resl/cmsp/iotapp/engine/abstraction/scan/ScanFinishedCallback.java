package kr.ac.kaist.resl.cmsp.iotapp.engine.abstraction.scan;

/**
 * Created by shheo on 15. 4. 20.
 */

public interface ScanFinishedCallback {
    void scanFinished(DeviceScanStrategy finishedStrategy);
}