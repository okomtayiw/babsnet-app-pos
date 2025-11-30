package com.babsnet.posapp.licensing;

import java.io.InputStream;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.Properties;

public  class LicenseV1 {
    private LicenseV1(){}

    public record License(String hardwareId, LocalDate expiresAt, String licensee, String product) {}

    public static License read(Path path) throws Exception {
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(path)) { p.load(in); }
        String hw  = p.getProperty("hardwareId", "").trim();
        String exp = p.getProperty("expiresAt", "").trim();
        String lic = p.getProperty("licensee", "").trim();
        String prod= p.getProperty("product", "").trim();
        LocalDate expires = exp.isBlank() ? null : LocalDate.parse(exp);
        return new License(hw, expires, lic, prod);
    }
}
