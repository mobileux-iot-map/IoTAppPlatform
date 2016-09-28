package kr.ac.kaist.resl.cmsp.iotapp.engine.abstraction;

import android.content.Context;
import android.content.res.AssetManager;
import kr.ac.kaist.resl.cmsp.iotapp.engine.PlatformException;
import kr.ac.kaist.resl.cmsp.iotapp.felixrunner.FelixRunner;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * Created by shheo on 15. 4. 7.
 */
public class DynamicModuleEngine {
    final Logger logger = LoggerFactory.getLogger(DynamicModuleEngine.class.getSimpleName());
    public static final DynamicModuleEngine INSTANCE = new DynamicModuleEngine();
    private static final FelixRunner fRunner = FelixRunner.INSTANCE;
    Context appContext;

    private DynamicModuleEngine() {
    }

    public int init(Context appContext) {
        this.appContext = appContext;
        return 0;
    }

    public void start() throws PlatformException {
        String bundlePath = appContext.getFilesDir().getAbsolutePath() + "/felix/bundles/";
        File bundleDir = new File(bundlePath);
        if (!bundleDir.exists()) bundleDir.mkdirs();
        copyAssets("felix/bundles", bundlePath);
        String cachePath = appContext.getFilesDir().getAbsolutePath() + "/felix/cache/";
        File cacheDir = new File(cachePath);
        if (!cacheDir.exists()) cacheDir.mkdirs();
        fRunner.init(cachePath, bundlePath);
        try {
            fRunner.startFramework();
        } catch (BundleException e) {
            throw new PlatformException(e);
        }
    }

    public void stop() throws PlatformException {
        try {
            fRunner.stopFramwework();
        } catch (BundleException e) {
            throw new PlatformException(e);
        }
    }

    private void copyAssets(String assetPath, String outPath) {
        AssetManager assetManager = appContext.getAssets();
        String[] files = null;
        try {
            files = assetManager.list(assetPath);
        } catch (IOException e) {
            logger.error("Failed to get asset file list.", e);
        }
        for (String filename : files) {
            InputStream in = null;
            OutputStream out = null;
            try {
                String absFileName = assetPath + "/" + filename;
                logger.debug("Copying asset file: " + absFileName);
                in = assetManager.open(absFileName);
                File outFile = new File(outPath, filename);
                out = new FileOutputStream(outFile);
                copyFile(in, out);
                in.close();
                in = null;
                out.flush();
                out.close();
                out = null;
            } catch (IOException e) {
                logger.error("Failed to copy asset file: " + filename, e);
            }
        }
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }
}
