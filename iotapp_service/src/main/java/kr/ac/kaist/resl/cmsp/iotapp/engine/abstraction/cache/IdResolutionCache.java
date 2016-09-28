package kr.ac.kaist.resl.cmsp.iotapp.engine.abstraction.cache;

import android.content.Context;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by shheo on 15. 4. 19.
 */
public class IdResolutionCache {
    public static Map<String, String> cache = null;
    private static Context context;

    public static void init(Context appContext) throws Throwable {
        context = appContext;
        File file = new File(context.getDir("cache", Context.MODE_PRIVATE), "idResolutionCache");
        FileInputStream f = new FileInputStream(file);
        ObjectInputStream s = new ObjectInputStream(f);
        cache = (HashMap<String, String>) s.readObject();
        s.close();
    }

    public static void setTestValues() {
        if (cache == null)
            cache = new HashMap<>();
        cache.clear();
        cache.put("something", "anything");
    }

    @Override
    protected void finalize() throws Throwable {
        if (cache != null) {
            File file = new File(context.getDir("cache", Context.MODE_PRIVATE), "idResolutionCache");
            ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(file));
            outputStream.writeObject(cache);
            outputStream.flush();
            outputStream.close();
        }
        super.finalize();
    }
}
