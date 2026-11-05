package com.albrhi.tvwidget;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Tuya / Smart Life protocol, plain Java (no Android classes, so it can be tested on a computer).
 * Local: protocol 3.3 on TCP 6668 (AES-128-ECB with the device's local key) and the devices' own UDP broadcasts.
 * Cloud: the official OpenAPI, signed with HMAC-SHA256. JSON is passed around as strings on purpose.
 */
final class Tuya {
    private Tuya() { }

    private static int seq = 1;

    // ---- local protocol 3.3 -------------------------------------------------------------------

    static byte[] aes(byte[] data, byte[] key, boolean encrypt) throws Exception {
        Cipher c = Cipher.getInstance("AES/ECB/PKCS5Padding");
        c.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"));
        return c.doFinal(data);
    }

    static byte[] packet(int cmd, byte[] payload) {
        ByteBuffer b = ByteBuffer.allocate(16 + payload.length + 8);
        b.putInt(0x000055AA).putInt(seq++).putInt(cmd).putInt(payload.length + 8).put(payload);
        CRC32 crc = new CRC32();
        crc.update(b.array(), 0, 16 + payload.length);
        b.putInt((int) crc.getValue()).putInt(0x0000AA55);
        return b.array();
    }

    /** One frame from the device: {@code cmd} and the decrypted JSON text ("" when it carries none). */
    static final class Frame {
        int cmd, ret;
        String json = "";
    }

    static Frame readFrame(DataInputStream in, byte[] key) throws Exception {
        byte[] h = new byte[16];
        in.readFully(h);
        ByteBuffer hb = ByteBuffer.wrap(h);
        if (hb.getInt(0) != 0x000055AA) throw new Exception("not a Tuya frame");
        Frame f = new Frame();
        f.cmd = hb.getInt(8);
        int len = hb.getInt(12);
        if (len < 8 || len > 8192) throw new Exception("bad frame length");
        byte[] body = new byte[len];
        in.readFully(body);
        if (len >= 12) f.ret = ByteBuffer.wrap(body).getInt(0);
        int from = len >= 12 ? 4 : 0;
        int to = len - 8;
        if (to > from) {
            byte[] p = new byte[to - from];
            System.arraycopy(body, from, p, 0, p.length);
            if (p.length > 15 && p[0] == '3' && p[1] == '.') {              // "3.3" + 12 bytes before the ciphertext
                byte[] q = new byte[p.length - 15];
                System.arraycopy(p, 15, q, 0, q.length);
                p = q;
            }
            try { f.json = new String(aes(p, key, false), "UTF-8"); } catch (Exception e) { f.json = ""; }
        }
        return f;
    }

    private static Socket open(String ip) throws Exception {
        Socket s = new Socket();
        s.connect(new InetSocketAddress(ip, 6668), 1800);
        s.setSoTimeout(2500);
        return s;
    }

    /** Asks the device for its current state: returns its JSON, e.g. {"dps":{"1":false,"2":true,...}}. */
    static String query(String ip, String devId, String localKey) throws Exception {
        byte[] key = localKey.getBytes("UTF-8");
        String json = "{\"gwId\":\"" + devId + "\",\"devId\":\"" + devId + "\"}";
        Socket s = open(ip);
        try {
            s.getOutputStream().write(packet(0x0A, aes(json.getBytes("UTF-8"), key, true)));
            DataInputStream in = new DataInputStream(s.getInputStream());
            for (int i = 0; i < 4; i++) {
                Frame f = readFrame(in, key);
                if (f.cmd == 0x0A && f.json.length() > 0) return f.json;
            }
            throw new Exception("no answer");
        } finally { try { s.close(); } catch (Exception ignored) { } }
    }

    /** Sets datapoints, e.g. dpsJson = {"1":true}. Returns normally when the device acknowledged. */
    static void control(String ip, String devId, String localKey, String dpsJson) throws Exception {
        byte[] key = localKey.getBytes("UTF-8");
        String json = "{\"devId\":\"" + devId + "\",\"uid\":\"" + devId + "\",\"t\":\"" + (System.currentTimeMillis() / 1000)
                + "\",\"dps\":" + dpsJson + "}";
        byte[] enc = aes(json.getBytes("UTF-8"), key, true);
        byte[] payload = new byte[15 + enc.length];
        payload[0] = '3'; payload[1] = '.'; payload[2] = '3';
        System.arraycopy(enc, 0, payload, 15, enc.length);
        Socket s = open(ip);
        try {
            s.getOutputStream().write(packet(0x07, payload));
            DataInputStream in = new DataInputStream(s.getInputStream());
            for (int i = 0; i < 4; i++) {
                Frame f = readFrame(in, key);
                if (f.cmd == 0x07) { if (f.ret != 0) throw new Exception("device refused (" + f.ret + ")"); return; }
            }
            throw new Exception("no acknowledgement");
        } finally { try { s.close(); } catch (Exception ignored) { } }
    }

    /** Listens to the devices' own broadcasts for {@code ms}: returns gwId -> ip. Sends nothing. */
    static Map<String, String> discover(int ms) {
        Map<String, String> found = new HashMap<String, String>();
        DatagramSocket[] socks = new DatagramSocket[2];
        int[] ports = {6666, 6667};
        try {
            byte[] bkey = MessageDigest.getInstance("MD5").digest("yGAdlopoPVldABfn".getBytes("UTF-8"));
            for (int i = 0; i < 2; i++) {
                try {
                    DatagramSocket d = new DatagramSocket(null);
                    d.setReuseAddress(true);
                    d.bind(new InetSocketAddress(ports[i]));
                    d.setSoTimeout(250);
                    socks[i] = d;
                } catch (Exception ignored) { }
            }
            long end = System.currentTimeMillis() + ms;
            byte[] buf = new byte[2048];
            Pattern gw = Pattern.compile("\"gwId\"\\s*:\\s*\"([^\"]+)\"");
            while (System.currentTimeMillis() < end) {
                for (int i = 0; i < 2; i++) {
                    if (socks[i] == null) continue;
                    try {
                        DatagramPacket p = new DatagramPacket(buf, buf.length);
                        socks[i].receive(p);
                        byte[] d = new byte[p.getLength()];
                        System.arraycopy(buf, 0, d, 0, d.length);
                        if (d.length < 28 || ByteBuffer.wrap(d).getInt(0) != 0x000055AA) continue;
                        int len = ByteBuffer.wrap(d).getInt(12);
                        int end2 = Math.min(d.length, 16 + len) - 8;
                        if (end2 <= 20) continue;
                        byte[] pl = new byte[end2 - 20];
                        System.arraycopy(d, 20, pl, 0, pl.length);
                        String text;
                        try { text = new String(pl, "UTF-8"); if (!text.startsWith("{")) throw new Exception(); }
                        catch (Exception e) { text = new String(aes(pl, bkey, false), "UTF-8"); }
                        Matcher m = gw.matcher(text);
                        if (m.find()) found.put(m.group(1), p.getAddress().getHostAddress());
                    } catch (java.net.SocketTimeoutException ignored) {
                    } catch (Exception ignored) { }
                }
            }
        } catch (Exception ignored) {
        } finally {
            for (DatagramSocket d : socks) if (d != null) d.close();
        }
        return found;
    }

    // ---- cloud (OpenAPI) ------------------------------------------------------------------------

    static String sha256Hex(byte[] data) throws Exception {
        return hex(MessageDigest.getInstance("SHA-256").digest(data));
    }

    static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }

    /** One signed request. {@code token} is "" for the token request itself. Returns the response body. */
    static String cloud(String method, String base, String id, String secret, String token, String path, String body) throws Exception {
        byte[] bb = body == null ? new byte[0] : body.getBytes("UTF-8");
        String t = String.valueOf(System.currentTimeMillis());
        String toSign = method + "\n" + sha256Hex(bb) + "\n\n" + path;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256"));
        String sign = hex(mac.doFinal((id + token + t + toSign).getBytes("UTF-8"))).toUpperCase();
        HttpURLConnection h = (HttpURLConnection) new URL(base + path).openConnection();
        try {
            h.setConnectTimeout(8000);
            h.setReadTimeout(12000);
            h.setRequestMethod(method);
            h.setRequestProperty("client_id", id);
            h.setRequestProperty("sign", sign);
            h.setRequestProperty("t", t);
            h.setRequestProperty("sign_method", "HMAC-SHA256");
            if (token.length() > 0) h.setRequestProperty("access_token", token);
            if (body != null) {
                h.setRequestProperty("Content-Type", "application/json");
                h.setDoOutput(true);
                h.getOutputStream().write(bb);
            }
            int code = h.getResponseCode();
            InputStream in = code >= 400 ? h.getErrorStream() : h.getInputStream();
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            if (in != null) {
                byte[] b = new byte[2048];
                int n;
                while ((n = in.read(b)) > 0) bo.write(b, 0, n);
            }
            return bo.toString("UTF-8");
        } finally { h.disconnect(); }
    }
}
