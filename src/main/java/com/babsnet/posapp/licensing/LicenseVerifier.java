package com.babsnet.posapp.licensing;

import java.io.IOException;
import java.nio.file.*;

public class LicenseVerifier {
    private LicenseVerifier(){}

    public static void installLicense(Path source) throws IOException {
        Path target = LicensePaths.defaultLicensePath();
        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }
}
