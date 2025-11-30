package com.babsnet.posapp.licensing;

import java.nio.file.Path;
import java.nio.file.Files;

public class LicensePaths {
    private LicensePaths(){}

    public static Path defaultLicensePath() {
        String os = System.getProperty("os.name","").toLowerCase();
        if (os.contains("win")) {
            String programData = System.getenv().getOrDefault("ProgramData", "C:\\\\ProgramData");
            return Path.of(programData, "BabsnetPOS", "license.lic");
        }
        var etc = Path.of("/etc/babsnetpos/license.lic");
        if (Files.isReadable(etc)) return etc;
        return Path.of(System.getProperty("user.home"), ".babsnetpos", "license.lic");
    }
}
