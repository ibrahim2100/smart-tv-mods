package com.albrhi.tvwidget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;

/** Result of an install/uninstall: shows the confirmation screen, then the outcome. */
public class InstallReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        int st = i.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        String label = i.getStringExtra("label");
        RemoteServer srv = RemoteServer.get(c);
        switch (st) {
            case PackageInstaller.STATUS_PENDING_USER_ACTION:
                Intent confirm = i.getParcelableExtra(Intent.EXTRA_INTENT);
                srv.status = "بانتظار تأكيدك على التلفزيون: " + label;
                Banner.show(c, "تأكيد مطلوب", label == null ? "" : label, "اضغط OK على الريموت", null, null, 30);
                if (confirm != null) {
                    try { c.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); } catch (Throwable ignored) { }
                }
                break;
            case PackageInstaller.STATUS_SUCCESS:
                srv.status = "تم: " + label;
                Banner.show(c, "تمت العملية بنجاح", label == null ? "" : label, "", null, null, 8);
                break;
            default:
                String msg = i.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
                srv.status = "ما تمت العملية" + (msg == null ? "" : " (" + msg + ")");
                Banner.show(c, "ما تمت العملية", label == null ? "" : label, msg == null ? "" : msg, null, null, 12);
        }
    }
}
