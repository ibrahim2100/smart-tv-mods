package com.albrhi.tvwidget;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Keeps a plain-text copy of your layout (app order, hidden apps, HDMI names) next to the app's files,
 * where `adb pull` can reach it, and imports a copy that `adb push` left in the import folder.
 */
final class LayoutBackup {
    private LayoutBackup() { }

    static File file(Context c) {
        File dir = c.getExternalFilesDir(null);
        return dir == null ? null : new File(dir, "layout.json");
    }

    static void export(Context c) {
        try {
            File f = file(c);
            if (f == null) return;
            SharedPreferences l = c.getSharedPreferences("layout", Context.MODE_PRIVATE);
            SharedPreferences h = c.getSharedPreferences("hdmi", Context.MODE_PRIVATE);
            JSONObject o = new JSONObject();
            o.put("order", l.getString("order", ""));
            o.put("hidden", l.getString("hidden", ""));
            o.put("names", l.getString("names", "{}"));
            o.put("sportsKey", c.getSharedPreferences("fx", Context.MODE_PRIVATE).getString("key", "123"));
            JSONObject names = new JSONObject();
            for (String k : h.getAll().keySet()) names.put(k, String.valueOf(h.getAll().get(k)));
            o.put("hdmi", names);
            o.put("savedAt", System.currentTimeMillis());
            FileOutputStream out = new FileOutputStream(f);
            out.write(o.toString(2).getBytes("UTF-8"));
            out.close();
        } catch (Throwable ignored) { }
    }

    /** If a layout.json was pushed into files/import/, apply it once and remove it. Returns true if applied. */
    static boolean importPending(Context c) {
        try {
            File dir = c.getExternalFilesDir(null);
            if (dir == null) return false;
            File f = new File(new File(dir, "import"), "layout.json");
            if (!f.exists()) return false;
            InputStream in = new FileInputStream(f);
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] b = new byte[4096];
            int n;
            while ((n = in.read(b)) > 0) bo.write(b, 0, n);
            in.close();
            JSONObject o = new JSONObject(bo.toString("UTF-8"));
            c.getSharedPreferences("layout", Context.MODE_PRIVATE).edit()
                    .putString("order", o.optString("order")).putString("hidden", o.optString("hidden"))
                    .putString("names", o.optString("names", "{}")).apply();
            SharedPreferences.Editor e = c.getSharedPreferences("hdmi", Context.MODE_PRIVATE).edit().clear();
            JSONObject names = o.optJSONObject("hdmi");
            if (names != null) {
                java.util.Iterator<String> it = names.keys();
                while (it.hasNext()) { String k = it.next(); e.putString(k, names.optString(k)); }
            }
            e.apply();
            if (o.has("sportsKey")) c.getSharedPreferences("fx", Context.MODE_PRIVATE).edit().putString("key", o.optString("sportsKey", "123")).apply();
            f.delete();
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
