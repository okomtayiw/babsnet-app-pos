package com.babsnet.posapp.licensing;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

public class LicenseGate {
    private LicenseGate() {}

    public static LicenseStatus check() {
        try {
            Path licPath = LicensePaths.defaultLicensePath();
            if (!Files.exists(licPath)) return LicenseStatus.MISSING;

            String myHw = HardwareFingerprint.getHardwareId();

            // Decrypt & validate V2
            var lic = LicenseV2.read(licPath, SecretLoader.loadAppSecret(), myHw);

            if (lic.expiresAt() != null && LocalDate.now().isAfter(lic.expiresAt()))
                return LicenseStatus.EXPIRED;

            return LicenseStatus.VALID;

        } catch (SecurityException hwOrKey) {
            return LicenseStatus.MISMATCH;
        } catch (Exception e) {
            return LicenseStatus.READ_ERROR;
        }
    }

    public static boolean isValid() { return check() == LicenseStatus.VALID; }
}
