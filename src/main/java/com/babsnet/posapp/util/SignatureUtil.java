package com.babsnet.posapp.util;


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class SignatureUtil {
    private SignatureUtil(){}

    public static String md5Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("MD5 error: " + e.getMessage(), e);
        }
    }
}

