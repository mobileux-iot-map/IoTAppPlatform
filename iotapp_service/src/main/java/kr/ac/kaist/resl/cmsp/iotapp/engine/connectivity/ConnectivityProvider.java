package kr.ac.kaist.resl.cmsp.iotapp.engine.connectivity;

import java.util.List;


public interface ConnectivityProvider {
    int init();

    List<String> scan();
}
