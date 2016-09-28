package kr.ac.kaist.resl.cmsp.iotapp.engine.clustering;

import org.alljoyn.bus.annotation.BusInterface;
import org.alljoyn.bus.annotation.BusMethod;

/**
 * Created by shheo on 15. 4. 19.
 */
@BusInterface(name = "kr.ac.kaist.resl.cmsp.iotapp.engine.clustering.clustermanager")
public interface AllJoynClusterBusInterface {
    @BusMethod
    void handleServiceInvocation(String thingId, String serviceInvocation);

    @BusMethod
    void handleLongServiceInvocation(String thingId, String serviceInvocationChunk, int id, int index, int count);

    @BusMethod
    String handleServiceInvocationWithReturn(String thingId, String serviceInvocation);

    @BusMethod
    String handleLongServiceInvocationWithReturn(String thingId, String serviceInvocationChunk, int id, int index, int count);

    @BusMethod
    String getAvailableServiceList(String interfaces, boolean scanNow);

    @BusMethod
    int addAvailableService(String serviceInfo);

    @BusMethod
    int removeAvailableService(String thingId);


    @BusMethod
    String getDeviceId();
}

