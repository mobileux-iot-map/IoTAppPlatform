package kr.ac.kaist.resl.cmsp.iotapp.engine.connectivity.alljoyn;

import kr.ac.kaist.resl.cmsp.iotapp.engine.connectivity.ConnectivityProvider;

import java.util.List;

/**
 * Created by shheo on 15. 4. 10.
 */
public class AllJoynConnectivityProvider implements ConnectivityProvider {
    static {
        System.loadLibrary("alljoyn_java");
    }

    @Override
    public int init() {
        return 0;
    }

    @Override
    public List<String> scan() {
        return null;
    }


}
