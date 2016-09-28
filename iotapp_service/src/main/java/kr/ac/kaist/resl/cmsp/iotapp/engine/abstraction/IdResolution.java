package kr.ac.kaist.resl.cmsp.iotapp.engine.abstraction;

import android.content.Context;
import kr.ac.kaist.resl.cmsp.iotapp.engine.abstraction.cache.IdResolutionCache;
import kr.ac.kaist.resl.cmsp.iotapp.platform.PlatformService;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by shheo on 15. 4. 13.
 */
public class IdResolution {
    public static final IdResolution INSTANCE = new IdResolution();

    private static PlatformService service;
    private static Context appContext;

    private IdResolution() {
    }

    public void init(PlatformService _service) {
        service = _service;
        appContext = service.getApplicationContext();
        IdResolutionCache.setTestValues();
        /* FIXME: Change to actual id resolution values
        try {
            IdResolutionCache.init(appContext);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        */
    }

    public String getBundleLocation(String id) {
        String cachedData = IdResolutionCache.cache.get(id);
        if (cachedData != null) {
            return cachedData;
        } else {
            // TODO: Add ONS query sequence
            return null;
        }
    }

    public String getDeviceServices(String id) {
        String cachedData = IdResolutionCache.cache.get(id);
        if (cachedData != null) {
            return cachedData;
        } else {
            // TODO: Add ONS query sequence
            return null;
        }
    }
}
