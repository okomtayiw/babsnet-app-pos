package com.babsnet.posapp.util;

public class ReceiptPrintUtil {
    private static final int COLS = 32;
    private static final int NAME_COL = 17; // kolom "Item"
    private static final int QTY_COL  = 3;  // kolom "Qty"
    private static final int AMT_COL  = 10; // kolom "Total"

    public static void appendSummaryQtyAndAmount(StringBuilder sb, String label, int qty, double amount){
        int leftWidth = COLS - (1 + QTY_COL + 1 + AMT_COL);
        String left   = fit(label, leftWidth);
        String q      = padLeft(String.valueOf(qty), QTY_COL);
        String amt    = padLeft(money(amount), AMT_COL);
        sb.append(left).append(" ").append(q).append(" ").append(amt).append("\n");
    }

    public static void appendSummaryAmount(StringBuilder sb, String label, double amount){
        String left  = fit(label, COLS - AMT_COL);
        String right = padLeft(money(amount), AMT_COL);
        sb.append(left).append(right).append("\n");
    }


    // ====== HELPER STRING ======
    private static String padLeft(String s, int w){ return String.format("%"  + w + "s", s == null ? "" : s); }
    private static String padRight(String s,int w){ return String.format("%-" + w + "s", s == null ? "" : s); }
    private static String fit(String s, int w){
        if (s == null) s = "";
        s = s.replace("\r","").replace("\n",""); // pastikan 1 baris
        return (s.length() > w) ? s.substring(0, w) : padRight(s, w);
    }
    private static String money(double v){
        return String.format("%,.0f", v);
    }

    public static void appendProductLine58(StringBuilder sb, String name, int qty, double subtotal){

        String nm   = fit(name, NAME_COL);
        String q    = padLeft(String.valueOf(qty), QTY_COL);
        String amt  = padLeft(money(subtotal), AMT_COL);
        sb.append(nm).append(" ").append(q).append(" ").append(amt).append("\n");

        if (name != null && name.length() > NAME_COL){
            String rest = name.substring(NAME_COL);
            while (!rest.isEmpty()){
                String part = rest.length() > COLS ? rest.substring(0, COLS) : rest;
                sb.append(fit(part, COLS)).append("\n");
                rest = rest.length() > COLS ? rest.substring(COLS) : "";
            }
        }
    }

    public static void appendCenteredWrap(StringBuilder sb, String text, int cols) {
        if (text == null) return;
        text = text.trim().replaceAll("\\s+", " ");
        int i = 0;
        while (i < text.length()) {
            int end = Math.min(i + cols, text.length());
            int breakPos = end;
            if (end < text.length()) {
                int space = text.lastIndexOf(' ', end);
                if (space >= i + 1) breakPos = space;
            }
            String line = text.substring(i, breakPos);
            sb.append(centerText(line, cols)).append("\n");
            i = (breakPos < text.length() && text.charAt(breakPos) == ' ') ? breakPos + 1 : breakPos;
        }
    }



    public static String centerText(String text, int width) {
        int padding = (width - text.length()) / 2;
        return " ".repeat(Math.max(0, padding)) + text;
    }

    public static String repeat(String s, int count) {
        return s.repeat(Math.max(0, count));
    }
}
