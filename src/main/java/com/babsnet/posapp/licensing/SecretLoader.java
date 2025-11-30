package com.babsnet.posapp.licensing;

public class SecretLoader {
    private SecretLoader(){}

    private static final char[] SECRET = "b4b5405".toCharArray();

    public static char[] loadAppSecret() {
        return SECRET.clone();
    }
}
