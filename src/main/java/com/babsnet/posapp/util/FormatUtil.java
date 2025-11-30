package com.babsnet.posapp.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class FormatUtil {

    // Format angka ke mata uang Rupiah (contoh: Rp12.345,00)
    public static String toRupiah(double value) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        return formatter.format(value);
    }

    // Format angka ke Dollar Amerika (contoh: $12,345.00)
    public static String toDollar(double value) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
        return formatter.format(value);
    }

    // Format Rupiah tanpa koma desimal (contoh: Rp12.345)
    public static String toRupiahNoDecimal(double value) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        formatter.setMaximumFractionDigits(0);
        return formatter.format(value);
    }

    // Format date dari yyyy-MM-dd ke dd/MM/yyyy (atau format lain sesuai kebutuhan)
    public static String formatDate(String dateStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            return date.format(formatter);
        } catch (Exception e) {
            return dateStr;
        }
    }

    // Format date ke dd MMM yyyy (contoh: 14 Jul 2025)
    public static String formatDateLong(String dateStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", new Locale("id", "ID"));
            return date.format(formatter);
        } catch (Exception e) {
            return dateStr;
        }
    }

    public static int parseIntSafe(String s) {
        if (s == null || s.trim().isEmpty()) return 0;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static double parseDoubleSafe(String s) {
        if (s == null || s.trim().isEmpty()) return 0.0;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            MessageDialogUtil.showWarning("Input harga tidak valid!");
            return 0.0;
        }
    }


    public static String toIntegerString(double value) {
        return BigDecimal.valueOf(value).setScale(0, RoundingMode.HALF_UP).toPlainString();
    }


    public static String toIntegerString(double value, RoundingMode mode) {
        if (mode == null) mode = RoundingMode.HALF_UP;
        return BigDecimal.valueOf(value).setScale(0, mode).toPlainString();
    }





}
