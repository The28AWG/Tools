package ru.tasp.tools;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;

import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import ru.tasp.tools.logger.L;

/**
 * Created by the28awg on 25.10.15.
 */
public class Tools {

    private static Context context = null;

    public static Context getContext() {
        if (context == null) {
            Lock lock = new ReentrantLock();
            lock.lock();
            try {
                if (context == null) {
                    Context tmp = null;
                    try {
                        tmp = getApplicationUsingReflection().getApplicationContext();
                    } catch (Exception ignore) {
                    }
                    if (tmp != null) {
                        context = tmp;
                    }
                }
            } finally {
                lock.unlock();
            }
        }
        return context;
    }

    public static Application getApplicationUsingReflection() throws Exception {
        return (Application) Class.forName("android.app.ActivityThread")
                .getMethod("currentApplication").invoke(null, (Object[]) null);
    }

    public static Bundle getManifestProperty() {
        try {
            ApplicationInfo ai = getContext().getPackageManager().getApplicationInfo(getContext().getPackageName(), PackageManager.GET_META_DATA);

            return ai.metaData;
        } catch (Exception e) {
            L.error(e, e.getMessage());
        }
        return null;
    }

    public static void dumpIntent(Intent i){
        Bundle bundle = i.getExtras();
        dumpBundle(bundle);
    }

    public static void dumpBundle(Bundle bundle){
        if (bundle != null) {
            Set<String> keys = bundle.keySet();
            Iterator<String> it = keys.iterator();
            L.debug("Dumping start");
            while (it.hasNext()) {
                String key = it.next();
                L.debug("[" + key + "=" + bundle.get(key) + "]");
            }
            L.debug("Dumping end");
        }
    }

    // 00000000-3893-2adf-ffff-ffff99d603a9
    public static String deviceId() {
        final TelephonyManager tm = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        final String tmDevice, tmSerial, androidId;
        tmDevice = "" + tm.getDeviceId();
        tmSerial = "" + tm.getSimSerialNumber();
        androidId = "" + android.provider.Settings.Secure.getString(getContext().getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        UUID deviceUuid = new UUID(androidId.hashCode(), ((long)tmDevice.hashCode() << 32) | tmSerial.hashCode());
        return deviceUuid.toString();
    }
}
