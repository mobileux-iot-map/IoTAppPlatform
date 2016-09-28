package kr.ac.kaist.resl.cmsp.iotapp.felixrunner;

import org.apache.felix.framework.FrameworkFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class FelixRunner {
    final Logger logger = LoggerFactory.getLogger(FelixRunner.class.getSimpleName());
    public static final FelixRunner INSTANCE = new FelixRunner();
    Framework framework;
    String cachePath = null;
    String bundlePath = null;

    private FelixRunner() {
    }

    public void init(String cachePath, String bundlePath) {
        this.cachePath = cachePath;
        this.bundlePath = bundlePath;
    }

    public void startFramework() throws BundleException, IllegalStateException {
        if (cachePath == null || bundlePath == null) {
            logger.error("Cache or bundle directory is not set.");
            throw new IllegalStateException("Cache or bundle directory is not set.");
        }
        FrameworkFactory factory = new FrameworkFactory();
        Map<String, String> configMap = new HashMap<String, String>();
        configMap.put(Constants.FRAMEWORK_STORAGE, cachePath);
        configMap.put(Constants.FRAMEWORK_BEGINNING_STARTLEVEL, "5");
        configMap.put(Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
        configMap.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, ANDROID_FRAMEWORK_PACKAGES + ", " + IOTAPP_LIB_PACKAGES);

        configMap.put("osgi.shell.telnet.ip", "127.0.0.1");
        configMap.put("osgi.shell.telnet.port", "6666");
        configMap.put("osgi.shell.telnet.maxconn", "2");
        configMap.put("osgi.shell.telnet.socketTimeout", "0");

        configMap.put("felix.embedded.execution", "true");
        configMap.put("felix.bootdelegation.implicit", "false");
        configMap.put("felix.log.level", "1");
        // Auto deployment is not working in Android
        configMap.put("felix.auto.deploy.action", "install,start");
        configMap.put("felix.auto.deploy.dir", bundlePath);
        // This option prevents Gogo shell shutting down the framework
        configMap.put("gosh.args", "--nointeractive");

        framework = factory.newFramework(configMap);
        try {
            // Start framework
            framework.init();
            framework.start();
            BundleContext bundleCtx = framework.getBundleContext();
            // Get list of bundles from bundleDir and install/start them
            File bundleDir = new File(bundlePath);
            File[] bundleFiles = bundleDir.listFiles();
            for (File file : bundleFiles) {
                if (file.getName().endsWith("jar")) {
                    logger.debug("Installing bundle...: " + file.getName());
                    bundleCtx.installBundle("file:" + file.getPath());
                }
            }
            for (Bundle bundle : bundleCtx.getBundles()) {
                logger.debug("Starting bundle...: " + bundle.getSymbolicName());
                bundle.start();
            }
        } catch (BundleException e) {
            logger.error("Failed to start framework. Stopping framework...", e);
            try {
                framework.stop();
            } catch (BundleException e1) {
                logger.error("Failed to stop framework", e1);
                throw e1;
            }
            throw e;
        }
    }

    public void stopFramwework() throws BundleException {
        try {
            framework.stop();
        } catch (BundleException e) {
            logger.error("Failed to stop framework", e);
            throw e;
        }
    }

    public String getBundlePath() {
        return bundlePath;
    }


    private static final String IOTAPP_LIB_PACKAGES = (
            "kr.ac.kaist.resl.cmsp.api, " +
                    "kr.ac.kaist.resl.cmsp.api.exception, " +
                    "kr.ac.kaist.resl.cmsp.abstraction.service, " +
                    "kr.ac.kaist.resl.cmsp.abstraction.service.exception, " +
                    "kr.ac.kaist.resl.cmsp.abstraction.service.general, " +
                    "kr.ac.kaist.resl.cmsp.abstraction.service.extended, " +
                    "kr.ac.kaist.resl.cmsp.abstraction.service.invocation, " +
                    "kr.ac.kaist.resl.cmsp.abstraction.service.annotation, " +
                    "kr.ac.kaist.resl.cmsp.connectivtyprovider, " +
                    "kr.ac.kaist.resl.cmsp.view, " +
                    "org.fourthline.cling.model.meta, " +
                    "org.fourthline.cling.model.types"
    ).intern();

    private static final String ANDROID_FRAMEWORK_PACKAGES = (
            "android, " +
                    "android.accessibilityservice, " +
                    "android.accounts, " +
                    "android.animation, " +
                    "android.app, " +
                    "android.app.admin, " +
                    "android.app.backup, " +
                    "android.appwidget, " +
                    "android.bluetooth, " +
                    "android.content, " +
                    "android.content.pm, " +
                    "android.content.res, " +
                    "android.database, " +
                    "android.database.sqlite, " +
                    "android.drm, " +
                    "android.gesture, " +
                    "android.graphics, " +
                    "android.graphics.drawable, " +
                    "android.graphics.drawable.shapes, " +
                    "android.graphics.pdf, " +
                    "android.hardware, " +
                    "android.hardware.display, " +
                    "android.hardware.input, " +
                    "android.hardware.location, " +
                    "android.hardware.usb, " +
                    "android.inputmethodservice, " +
                    "android.location, " +
                    "android.media, " +
                    "android.media.audiofx, " +
                    "android.media.effect, " +
                    "android.mtp, " +
                    "android.net, " +
                    "android.net.http, " +
                    "android.net.nsd, " +
                    "android.net.rtp, " +
                    "android.net.sip, " +
                    "android.net.wifi, " +
                    "android.net.wifi.p2p, " +
                    "android.net.wifi.p2p.nsd, " +
                    "android.nfc, " +
                    "android.nfc.cardemulation, " +
                    "android.nfc.tech, " +
                    "android.opengl, " +
                    "android.os, " +
                    "android.os.storage, " +
                    "android.preference, " +
                    "android.print, " +
                    "android.print.pdf, " +
                    "android.printservice, " +
                    "android.provider, " +
                    "android.renderscript, " +
                    "android.sax, " +
                    "android.security, " +
                    "android.service.dreams, " +
                    "android.service.notification, " +
                    "android.service.textservice, " +
                    "android.service.wallpaper, " +
                    "android.speech, " +
                    "android.speech.tts, " +
                    "android.support.v13.app, " +
                    "android.support.v4.accessibilityservice, " +
                    "android.support.v4.app, " +
                    "android.support.v4.content, " +
                    "android.support.v4.content.pm, " +
                    "android.support.v4.database, " +
                    "android.support.v4.graphics.drawable, " +
                    "android.support.v4.hardware.display, " +
                    "android.support.v4.media, " +
                    "android.support.v4.net, " +
                    "android.support.v4.os, " +
                    "android.support.v4.print, " +
                    "android.support.v4.text, " +
                    "android.support.v4.util, " +
                    "android.support.v4.view, " +
                    "android.support.v4.view.accessibility, " +
                    "android.support.v4.widget, " +
                    "android.support.v7.app, " +
                    "android.support.v7.appcompat, " +
                    "android.support.v7.gridlayout, " +
                    "android.support.v7.media, " +
                    "android.support.v7.mediarouter, " +
                    "android.support.v7.view, " +
                    "android.support.v7.widget, " +
                    "android.support.v8.renderscript, " +
                    "android.telephony, " +
                    "android.telephony.cdma, " +
                    "android.telephony.gsm, " +
                    "android.test, " +
                    "android.test.mock, " +
                    "android.test.suitebuilder, " +
                    "android.text, " +
                    "android.text.format, " +
                    "android.text.method, " +
                    "android.text.style, " +
                    "android.text.util, " +
                    "android.transition, " +
                    "android.util, " +
                    "android.view, " +
                    "android.view.accessibility, " +
                    "android.view.animation, " +
                    "android.view.inputmethod, " +
                    "android.view.textservice, " +
                    "android.webkit, " +
                    "android.widget, " +
                    "dalvik.bytecode, " +
                    "dalvik.system, " +
                    "javax.crypto, " +
                    "javax.crypto.interfaces, " +
                    "javax.crypto.spec, " +
                    "javax.microedition.khronos.egl, " +
                    "javax.microedition.khronos.opengles, " +
                    "javax.net, " +
                    "javax.net.ssl, " +
                    "javax.security.auth, " +
                    "javax.security.auth.callback, " +
                    "javax.security.auth.login, " +
                    "javax.security.auth.x500, " +
                    "javax.security.cert, " +
                    "javax.sql, " +
                    "javax.xml, " +
                    "javax.xml.datatype, " +
                    "javax.xml.namespace, " +
                    "javax.xml.parsers, " +
                    "javax.xml.transform, " +
                    "javax.xml.transform.dom, " +
                    "javax.xml.transform.sax, " +
                    "javax.xml.transform.stream, " +
                    "javax.xml.validation, " +
                    "javax.xml.xpath, " +
                    "junit.framework, " +
                    "junit.runner, " +
                    "org.apache.http, " +
                    "org.apache.http.auth, " +
                    "org.apache.http.auth.params, " +
                    "org.apache.http.client, " +
                    "org.apache.http.client.entity, " +
                    "org.apache.http.client.methods, " +
                    "org.apache.http.client.params, " +
                    "org.apache.http.client.protocol, " +
                    "org.apache.http.client.utils, " +
                    "org.apache.http.conn, " +
                    "org.apache.http.conn.params, " +
                    "org.apache.http.conn.routing, " +
                    "org.apache.http.conn.scheme, " +
                    "org.apache.http.conn.ssl, " +
                    "org.apache.http.conn.util, " +
                    "org.apache.http.cookie, " +
                    "org.apache.http.cookie.params, " +
                    "org.apache.http.entity, " +
                    "org.apache.http.impl, " +
                    "org.apache.http.impl.auth, " +
                    "org.apache.http.impl.client, " +
                    "org.apache.http.impl.conn, " +
                    "org.apache.http.impl.conn.tsccm, " +
                    "org.apache.http.impl.cookie, " +
                    "org.apache.http.impl.entity, " +
                    "org.apache.http.impl.io, " +
                    "org.apache.http.io, " +
                    "org.apache.http.message, " +
                    "org.apache.http.params, " +
                    "org.apache.http.protocol, " +
                    "org.apache.http.util, " +
                    "org.json, " +
                    "org.w3c.dom, " +
                    "org.w3c.dom.ls, " +
                    "org.xml.sax, " +
                    "org.xml.sax.ext, " +
                    "org.xml.sax.helpers, " +
                    "org.xmlpull.v1, " +
                    "org.xmlpull.v1.sax2"
    ).intern();
}
