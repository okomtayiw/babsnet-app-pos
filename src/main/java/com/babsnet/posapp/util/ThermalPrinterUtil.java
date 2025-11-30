package com.babsnet.posapp.util;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class ThermalPrinterUtil {

    public static void printToThermalPrinter(String receiptContent) {
        PrintService thermalPrinter = selectThermalPrinter("chasier", "EPSON", "POS", "Generic");
        if (thermalPrinter == null) {
            MessageDialogUtil.showError("Thermal printer not found!");
            return;
        }

        try {

            byte[] payload = buildEscPosPayload(receiptContent);
            DocPrintJob job = thermalPrinter.createPrintJob();
            Doc doc = new SimpleDoc(payload, DocFlavor.BYTE_ARRAY.AUTOSENSE, null);
            PrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet();
            job.print(doc, attrs);
        } catch (Exception e) {
            e.printStackTrace();
            MessageDialogUtil.showError("Failed to print: " + e.getMessage());
        }
    }



    private static byte[] buildEscPosPayload(String text) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        out.write(new byte[]{0x1B, '@'});                 // ESC @


        out.write(new byte[]{0x1B, 'a', 0x00});           // ESC a 0

        String normalized = text.replace("\r\n", "\n");
        out.write(normalized.getBytes(getPrinterCharset()));
        if (!normalized.endsWith("\n")) out.write('\n');

        out.write(new byte[]{0x1B, 'd', 0x03});
        out.write(cutPartial());

        return out.toByteArray();
    }

    private static byte[] cutPartial() { return new byte[]{0x1D, 'V', 0x41, 0x00}; }
    @SuppressWarnings("unused")
    private static byte[] cutFull()    { return new byte[]{0x1D, 'V', 0x00}; }

    // ===================== Printer discovery =====================
    private static PrintService selectThermalPrinter(String... keywords) {
        return Arrays.stream(PrintServiceLookup.lookupPrintServices(null, null))
                .filter(printer -> {
                    String name = printer.getName().toLowerCase();
                    return Arrays.stream(keywords).anyMatch(k -> name.contains(k.toLowerCase()));
                })
                .findFirst()
                .orElse(null);
    }

    // ===================== Encoding =====================
    private static Charset getPrinterCharset() {
        if (Charset.isSupported("CP858")) return Charset.forName("CP858");
        if (Charset.isSupported("CP437")) return Charset.forName("CP437");
        return StandardCharsets.ISO_8859_1;
    }
}
