package com.albrhi.tvwidget;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

/** Hands an APK (or an uninstall request) to Android's own installer. The user still confirms on the TV. */
final class Installer {
    private Installer() { }

    static final String ACTION = "com.albrhi.tvwidget.INSTALL_RESULT";

    static void install(Context c, File apk, String label) throws Exception {
        PackageInstaller pi = c.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams p = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        p.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_REQUIRED);
        int id = pi.createSession(p);
        PackageInstaller.Session s = pi.openSession(id);
        try {
            OutputStream o = s.openWrite("app.apk", 0, apk.length());
            InputStream in = new FileInputStream(apk);
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) > 0) o.write(buf, 0, n);
            in.close();
            s.fsync(o);
            o.close();
            s.commit(receiver(c, id, label));
        } catch (Exception e) {
            s.abandon();
            throw e;
        }
    }

    static void uninstall(Context c, String pkg) {
        PackageInstaller pi = c.getPackageManager().getPackageInstaller();
        pi.uninstall(pkg, receiver(c, pkg.hashCode(), pkg));
    }

    private static android.content.IntentSender receiver(Context c, int code, String label) {
        Intent i = new Intent(c, InstallReceiver.class).setAction(ACTION).putExtra("label", label);
        return PendingIntent.getBroadcast(c, code, i, PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT).getIntentSender();
    }
}
