package kr.ac.kaist.resl.cmsp.iotapp.engine.clustering;

import android.content.Context;
import kr.ac.kaist.resl.cmsp.iotapp.engine.PlatformException;
import kr.ac.kaist.resl.cmsp.iotapp.library.ThingServiceEndpoint;

/**
 * Created by shheo on 15. 4. 19.
 */
public interface ClusteringModule {
    void init(Context _appContext);

    void start() throws PlatformException;

    void stop();

    /**
     * Called when new peer comes into the cluster
     *
     * @param busName unique name of the peer
     */
    void notifyAllAvailableServices(String busName);

    /**
     * Called by Abstraction module. Notify other devices in cluster that new thing service is added
     *
     * @param serviceInfo
     */
    void notifyAvailableServiceAdded(ThingServiceEndpoint serviceInfo);

    /**
     * Called by Abstraction module. Notify other devices in cluster that new thing service is removed
     *
     * @param thingId
     */
    void notifyAvailableServiceRemoved(String thingId);


    void sendServiceInvocation(String thingId, String invocation);

    String sendServiceInvocationWithReturn(String thingId, String invocation);

    int openSessionAndAdvertise();

    int stopAdvertiseAndCloseSession();
}
