package com.babsnet.posapp.util;

import com.babsnet.posapp.model.OnlineTransaction;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ThermalReceiptFormatter {
    private ThermalReceiptFormatter() {}

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
    private static final NumberFormat RUPIAH = NumberFormat.getInstance(new Locale("id", "ID"));

    public static String format(OnlineTransaction tx) {
        return format(tx, 32); // default 58mm ~ 32 kolom
    }

    public static String format(OnlineTransaction tx, int cols) {
        StringBuilder sb = new StringBuilder(256);
        line(sb, center(ConfigUtil.get("company.name"), cols));
        line(sb, center(ConfigUtil.get("company.address"), cols));
        line(sb, center(ConfigUtil.get("company.city"), cols));
        line(sb, repeat('-', cols));
        line(sb, twoCols("No", nv(tx.transactionNumber), cols));
        line(sb, twoCols("Tgl", tx.transDate == null ? "-" : DTF.format(tx.transDate), cols));
        line(sb, twoCols("Nomor", nv(tx.customerId), cols));
        line(sb, twoCols("Kategori", nv(tx.category), cols));
        if (tx.denom != null && tx.denom != 0) {
            line(sb, twoCols("Nominal", RUPIAH.format(tx.denom), cols));
            line(sb, twoCols("Admin", RUPIAH.format(tx.sellPrice.subtract(BigDecimal.valueOf(tx.denom))), cols));
        } else {
            line(sb, twoCols("Nominal", RUPIAH.format(tx.buyPrice), cols));
            line(sb, twoCols("Admin", RUPIAH.format(tx.sellPrice.subtract(tx.buyPrice)), cols));
        }

        if (tx.sellPrice != null)
            line(sb, twoCols("Total", RUPIAH.format(tx.sellPrice), cols));
        line(sb, repeat('-', cols));
        line(sb, twoCols("RC/Msg", (nv(tx.rc) + (tx.providerMessage != null ? " / " + tx.providerMessage : "")), cols));
        if (tx.sn != null && !tx.sn.isBlank() && tx.category.equalsIgnoreCase("pln")) {
            line(sb, "SN/Token:");
            for (String chunk : wrap(tx.sn, cols)) line(sb, chunk);
        }
//        line(sb, twoCols("Ref", nv(tx.refId), cols));
        line(sb, repeat('-', cols));
        line(sb, center("Terima kasih", cols));
        line(sb, "");
        line(sb, "");
        return sb.toString();
    }

    private static void line(StringBuilder sb, String s) { sb.append(s).append('\n'); }
    private static String nv(String s) { return (s == null || s.isBlank()) ? "-" : s; }

    private static String center(String s, int cols) {
        if (s == null) s = "";
        if (s.length() >= cols) return s;
        int pad = (cols - s.length()) / 2;
        return " ".repeat(Math.max(0, pad)) + s;
    }
    private static String repeat(char c, int n) { return String.valueOf(c).repeat(Math.max(0, n)); }

    private static String twoCols(String left, String right, int cols) {
        left = left == null ? "" : left;
        right = right == null ? "" : right;
        int gap = 2; // spasi minimal antara kolom
        int leftMax = Math.max(0, cols - right.length() - gap);
        if (left.length() > leftMax) left = left.substring(0, leftMax);
        return left + " ".repeat(Math.max(1, cols - left.length() - right.length())) + right;
    }

    /** wrap sederhana per kolom (tanpa breaking words agresif) */
    private static java.util.List<String> wrap(String text, int cols) {
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        if (text == null) return out;
        String[] parts = text.replace("\r\n", "\n").split("\n");
        for (String p : parts) {
            int i = 0;
            while (i < p.length()) {
                int end = Math.min(i + cols, p.length());
                out.add(p.substring(i, end));
                i = end;
            }
            if (p.isEmpty()) out.add("");
        }
        return out;
    }
}
