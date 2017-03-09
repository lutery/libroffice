package org.libreoffice;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.AssetManager;
import android.util.Log;

import org.libreoffice.kit.LibreOfficeKit;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.libreoffice.kit.LibreOfficeKit.putenv;
import static org.libreoffice.kit.LibreOfficeKit.redirectStdio;

/**
 * Created by 辉 on 2017/1/10.
 */

public class MainLibreOfficeKit {
    private static String LOGTAG = LibreOfficeKit.class.getSimpleName();
    private static AssetManager mgr;

    // private constructor because instantiating would be meaningless
    private MainLibreOfficeKit() {
    }

    // Trigger library initialization - as this is done automatically by executing the "static" block, this method remains empty. However we need this to manually (at the right time) can force library initialization.
    public static void initializeLibrary() {
    }

    static boolean initializeDone = false;

    // This init() method should be called from the upper Java level of
    // LO-based apps.
    public static synchronized void init(Context context)
    {
        if (initializeDone) {
            return;
        }

        mgr = context.getResources().getAssets();

        ApplicationInfo applicationInfo = context.getApplicationInfo();
        String dataDir = applicationInfo.dataDir;
        Log.i(LOGTAG, String.format("Initializing LibreOfficeKit, dataDir=%s\n", dataDir));

        LibreOfficeKit.redirectStdio(true);

        String cacheDir = context.getCacheDir().getAbsolutePath();
        String apkFile = context.getPackageResourcePath();

        // If there is a fonts.conf file in the apk that can be extracted, automatically
        // set the FONTCONFIG_FILE env var.
        InputStream inputStream;
        try {
            inputStream = context.getAssets().open("unpack/etc/fonts/fonts.conf");
        } catch (java.io.IOException exception) {
            inputStream = null;
        }

        LibreOfficeKit.putenv("OOO_DISABLE_RECOVERY=1");

        if (inputStream != null) {
            LibreOfficeKit.putenv("FONTCONFIG_FILE=" + dataDir + "/etc/fonts/fonts.conf");
        }

        // TMPDIR is used by osl_getTempDirURL()
        LibreOfficeKit.putenv("TMPDIR=" + cacheDir);

        if (!LibreOfficeKit.initializeNative(dataDir, cacheDir, apkFile, mgr)) {
            Log.e(LOGTAG, "Initialize native failed!");
            return;
        }
        initializeDone = true;
    }
}
