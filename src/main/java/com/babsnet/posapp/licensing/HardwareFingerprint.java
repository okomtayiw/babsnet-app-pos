package com.babsnet.posapp.licensing;

import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

public class HardwareFingerprint {
    private HardwareFingerprint(){}

    public static String getHardwareId() {
        try {
            List<String> macs = new ArrayList<>();
            var ifs = NetworkInterface.getNetworkInterfaces();
            while (ifs.hasMoreElements()) {
                var ni = ifs.nextElement();
                if (ni == null || ni.isLoopback() || !ni.isUp()) continue;
                byte[] mac = ni.getHardwareAddress();
                if (mac != null && mac.length > 0) macs.add(bytesToHex(mac));
            }
            Collections.sort(macs);
            String host = Optional.ofNullable(System.getenv("COMPUTERNAME"))
                    .orElseGet(() -> Optional.ofNullable(System.getenv("HOSTNAME")).orElse("unknown"));
            String os = System.getProperty("os.name","") + "|" + System.getProperty("os.arch","") + "|" + System.getProperty("os.version","");
            return sha256Hex(String.join("|", String.join(",", macs), host, os));
        } catch (Exception e) {
            return sha256Hex(System.getProperty("os.name","") + "|" + System.getProperty("user.name","unknown"));
        }
    }

    private static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            var dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(dig.length*2);
            for (byte b : dig) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private static String bytesToHex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length*2);
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }
}
