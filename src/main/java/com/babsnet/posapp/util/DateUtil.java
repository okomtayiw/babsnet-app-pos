package com.babsnet.posapp.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtil {

    // Format untuk tampilan di tabel (ubah sesuai kebutuhan)
    private static final DateTimeFormatter NICE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    /**
     * Parse String ISO date dan return LocalDateTime.
     * Return null jika gagal parse.
     */
    public static LocalDateTime parseIso(String isoString) {
        if (isoString == null || isoString.trim().isEmpty()) return null;
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return LocalDateTime.parse(isoString, formatter);
        } catch (Exception e) {
            // Optional: fallback ke ISO jika mau (atau logging error)
            try {
                return LocalDateTime.parse(isoString, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception e2) {
                return null;
            }
        }
    }



    /**
     * Format LocalDateTime ke String dengan format friendly (dd-MM-yyyy HH:mm:ss)
     */
    public static String formatNice(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return NICE_FORMAT.format(dateTime);
    }

    /**
     * Utility: langsung format dari ISO string ke format nice, aman untuk table view.
     */
    public static String formatIsoToNice(String isoString) {
        LocalDateTime dt = parseIso(isoString);
        return formatNice(dt);
    }

    public static LocalDate parseDBDate(String dbValue) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.parse(dbValue, formatter).toLocalDate();
    }



}
