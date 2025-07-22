package com.babsnet.posapp.util;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.awt.*;
import java.awt.print.*;
import java.util.Arrays;

public class ThermalPrinterUtil {
    public static void printToThermalPrinter(String receiptContent) {
        PrinterJob job = PrinterJob.getPrinterJob();

        // Pilih printer thermal
        PrintService thermalPrinter = selectThermalPrinter("EPSON", "POS", "THERMAL");
        if (thermalPrinter == null) {
            MessageDialogUtil.showError("Thermal printer not found!");
            return;
        }

        try {
            job.setPrintService(thermalPrinter);
        } catch (PrinterException e) {
            e.printStackTrace();
            return;
        }

        // Set printable content
        job.setPrintable((graphics, pageFormat, pageIndex) -> {
            if (pageIndex > 0) return Printable.NO_SUCH_PAGE;

            Graphics2D g2d = (Graphics2D) graphics;

            // Set margin 0
            PageFormat customFormat = getThermalPageFormat();
            g2d.translate(customFormat.getImageableX(), customFormat.getImageableY());
            g2d.setFont(new Font("Monospaced", Font.PLAIN, 8));

            // Cetak baris per baris
            String[] lines = receiptContent.split("\\n");
            int y = 10;
            for (String line : lines) {
                g2d.drawString(line, 0, y);
                y += 12;
            }

            // Auto cut (Opsional: Sesuaikan dengan printer & driver)
            g2d.drawString("\u001D\u0056\u0001", 0, y + 20);  // ESC/POS Cut command (tidak semua printer support)

            return Printable.PAGE_EXISTS;
        }, getThermalPageFormat());

        try {
            job.print();
        } catch (PrinterException e) {
            e.printStackTrace();
        }
    }

    private static PageFormat getThermalPageFormat() {
        PageFormat format = new PageFormat();
        Paper paper = new Paper();

        // Paper 58mm: sekitar 200 point width (72 point = 1 inch)
        double width = 200;
        double height = 500; // bisa dynamic

        paper.setSize(width, height);
        paper.setImageableArea(0, 0, width, height); // tanpa margin
        format.setPaper(paper);

        return format;
    }

    private static PrintService selectThermalPrinter(String... keywords) {
        return Arrays.stream(PrintServiceLookup.lookupPrintServices(null, null))
                .filter(printer -> {
                    String name = printer.getName().toLowerCase();
                    return Arrays.stream(keywords).anyMatch(k -> name.contains(k.toLowerCase()));
                })
                .findFirst()
                .orElse(null);
    }
}

