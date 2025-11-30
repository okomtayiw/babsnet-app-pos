package com.babsnet.posapp.licensing;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Properties;

public  class LicenseV2 {
    private static final String MAGIC = "LIC1";       // marker biner
    private static final int GCM_TAG_BITS = 128;
    private static final int SALT_LEN = 16;
    private static final int IV_LEN   = 12;

    private LicenseV2() {}

    public static record License(
            String hardwareId,
            LocalDate expiresAt,
            String licensee,
            String product
    ) {}

    public static class BadLicenseFormat extends IllegalArgumentException {
        public BadLicenseFormat(String m) { super(m); }
    }

    /**
     * Membaca & mendekripsi license V2 (format: "LIC1:" + base64(MAGIC|ITER|SALT|IV|CT+TAG)).
     * AAD = hardwareId saat ini (lowercase), jadi file tidak bisa dipakai di PC lain.
     */
    public static License read(Path licPath, char[] passphrase, String currentHw) throws Exception {
        String blob = Files.readString(licPath, StandardCharsets.UTF_8).trim();
        if (!blob.startsWith("LIC1:")) {
            throw new BadLicenseFormat("Not an encrypted V2 license");
        }

        byte[] all = Base64.getDecoder().decode(blob.substring(5).trim());
        ByteBuffer buf = ByteBuffer.wrap(all);

        byte[] magic = new byte[4]; buf.get(magic);
        if (!new String(magic, StandardCharsets.US_ASCII).equals(MAGIC)) {
            throw new BadLicenseFormat("Bad magic");
        }

        int iterations = buf.getInt();
        byte[] salt = new byte[SALT_LEN]; buf.get(salt);
        byte[] iv   = new byte[IV_LEN];   buf.get(iv);
        byte[] ct   = new byte[buf.remaining()]; buf.get(ct);

        byte[] aad = currentHw.toLowerCase().getBytes(StandardCharsets.UTF_8);

        byte[] pt;
        try {
            pt = decrypt(ct, passphrase, salt, iterations, iv, aad);
        } catch (AEADBadTagException badTag) {
            // hardware beda / passphrase salah / file korup
            throw new SecurityException("Integrity/hardware check failed", badTag);
        }

        Properties props = new Properties();
        props.load(new StringReader(new String(pt, StandardCharsets.UTF_8)));

        String hw   = req(props, "hardwareId").toLowerCase();
        String expS = req(props, "expiresAt");
        String lic  = req(props, "licensee");
        String prod = req(props, "product");

        // Safety: isi plaintext harus match hardware saat ini
        if (!hw.equals(currentHw.toLowerCase())) {
            throw new SecurityException("HardwareId inside license does not match this device");
        }

        return new License(hw, LocalDate.parse(expS), lic, prod);
    }

    private static String req(Properties p, String k) {
        String v = p.getProperty(k);
        if (v == null || v.isBlank()) throw new IllegalArgumentException("Missing property: " + k);
        return v.trim();
    }

    private static byte[] decrypt(byte[] ct, char[] pass, byte[] salt, int iter, byte[] iv, byte[] aad)
            throws GeneralSecurityException {
        var sk = deriveKey(pass, salt, iter);
        var c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, sk, new GCMParameterSpec(GCM_TAG_BITS, iv));
        if (aad != null && aad.length > 0) c.updateAAD(aad);
        return c.doFinal(ct);
    }

    private static SecretKeySpec deriveKey(char[] pass, byte[] salt, int iter) throws GeneralSecurityException {
        var spec = new PBEKeySpec(pass, salt, iter, 256);
        var f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return new SecretKeySpec(f.generateSecret(spec).getEncoded(), "AES");
    }
}
