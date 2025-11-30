package com.babsnet.posapp.util;

import com.babsnet.posapp.model.ReportRow;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.awt.*;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public final class Utils {
    private Utils() {}

    public static Window ownerOf(Node node) {
        return (node != null && node.getScene() != null) ? node.getScene().getWindow() : null;
    }

    public static Dialog<Void> buildLoadingDialog(String title, String defaultMsg, Task<?> task, Window owner) {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle(title);
        if (owner != null) dlg.initOwner(owner);

        dlg.initModality(Modality.NONE);
        dlg.getDialogPane().getButtonTypes().clear();

        ProgressIndicator pi = new ProgressIndicator();
        pi.setPrefSize(64, 64);
        pi.progressProperty().bind(task.progressProperty());

        Label msg = new Label(defaultMsg);
        msg.textProperty().bind(
                Bindings.when(task.messageProperty().isEmpty())
                        .then(defaultMsg)
                        .otherwise(task.messageProperty())
        );

        VBox box = new VBox(12, pi, msg);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(16));
        dlg.getDialogPane().setContent(box);


        task.stateProperty().addListener((o, old, st) -> {
            if (st == Worker.State.SUCCEEDED || st == Worker.State.FAILED || st == Worker.State.CANCELLED) {
                Platform.runLater(() -> safeHide(dlg));
            }
        });


        dlg.setOnCloseRequest(e -> {
            if (task.isRunning()) task.cancel();
        });


        dlg.setOnHidden(e -> {
            pi.progressProperty().unbind();
            msg.textProperty().unbind();
        });

        return dlg;
    }


    private static void safeHide(Dialog<?> dlg) {
        if (dlg == null) return;
        try {
            if (dlg.isShowing()) dlg.hide();
        } catch (Exception ignored) {}

        try {
            Window w = (dlg.getDialogPane() != null && dlg.getDialogPane().getScene() != null)
                    ? dlg.getDialogPane().getScene().getWindow() : null;
            if (w != null) w.hide();
        } catch (Exception ignored) {}
    }


    public static void writePdfReport(List<ReportRow> list, File target, LocalDate start, LocalDate end) throws Exception {
        byte[] pdfBytes;
        DateTimeFormatter dtPdf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        BigDecimal totalBuy  = BigDecimal.ZERO;
        BigDecimal totalSell = BigDecimal.ZERO;
        for (ReportRow r : list) {
            if (r.getBuyPrice()  != null) totalBuy  = totalBuy.add(r.getBuyPrice());
            if (r.getUnitPrice() != null) totalSell = totalSell.add(r.getUnitPrice());
        }
        BigDecimal margin = totalSell.subtract(totalBuy);

        NumberFormat rupiah = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate(), 24, 24, 24, 24);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            Font fTitle = new Font(Font.HELVETICA, 14, Font.BOLD);
            Font fHdr   = new Font(Font.HELVETICA, 10, Font.BOLD);
            Font fCell  = new Font(Font.HELVETICA, 9,  Font.NORMAL);

            Paragraph title = new Paragraph("Laporan Transaksi", fTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);

            Paragraph per = new Paragraph("Periode: " + start + " s.d. " + end, new Font(Font.HELVETICA, 10));
            per.setAlignment(Element.ALIGN_CENTER);
            per.setSpacingAfter(10f);
            doc.add(per);

            float[] widths = {1.0f, 2.2f, 1.1f, 1.6f, 1.8f, 0.9f, 2.6f, 1.4f, 1.8f, 3.0f, 1.1f, 1.2f, 1.2f};
            PdfPTable t = new PdfPTable(widths);
            t.setWidthPercentage(100);
            addHdr(t, fHdr, "ID","Tanggal","Status","Customer","Ref ID","RC","Pesan",
                    "Kategori","Produk","Deskripsi","Denom","Harga Beli","Harga Jual");

            for (ReportRow r : list) {
                addCell(t, fCell, n(r.getId()));
                addCell(t, fCell, r.getTransDate()==null? "" : dtPdf.format(r.getTransDate()));
                addCell(t, fCell, s(r.getStatus()));
                addCell(t, fCell, s(r.getCustomerId()));
                addCell(t, fCell, s(r.getRefId()));
                addCell(t, fCell, s(r.getRc()));
                addCell(t, fCell, s(r.getProviderMessage()));
                addCell(t, fCell, s(r.getCategory()));
                addCell(t, fCell, s(r.getProductCode()));
                addCell(t, fCell, s(r.getDescription()));
                addCell(t, fCell, n(r.getDenom()));
                addCell(t, fCell, n(r.getBuyPrice()));
                addCell(t, fCell, n(r.getUnitPrice()));
            }

            doc.add(t);
  
            doc.add(Chunk.NEWLINE);
            PdfPTable sum = new PdfPTable(new float[]{2.5f, 3.0f});
            sum.setWidthPercentage(45);                       
            sum.setHorizontalAlignment(Element.ALIGN_RIGHT);
            addKeyVal(sum, fHdr, fCell, "Total Harga Beli",  fmtRp(totalBuy, rupiah));
            addKeyVal(sum, fHdr, fCell, "Total Harga Jual",  fmtRp(totalSell, rupiah));
      

            doc.add(sum);
            doc.close();
            pdfBytes = baos.toByteArray();
        }


        Files.write(target.toPath(), pdfBytes,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }

    private static void addKeyVal(PdfPTable t, Font keyFont, Font valFont, String key, String val) {
        PdfPCell ck = new PdfPCell(new Phrase(key, keyFont));
        ck.setBorder(Rectangle.NO_BORDER);
        ck.setVerticalAlignment(Element.ALIGN_MIDDLE);

        PdfPCell cv = new PdfPCell(new Phrase(val, valFont));
        cv.setBorder(Rectangle.NO_BORDER);
        cv.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cv.setVerticalAlignment(Element.ALIGN_MIDDLE);

        t.addCell(ck);
        t.addCell(cv);
    }

    private static void addHdr(PdfPTable t, Font f, String... cols){
        for (String c : cols) {
            PdfPCell cell = new PdfPCell(new Phrase(c, f));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBackgroundColor(new Color(230,230,230));
            t.addCell(cell);
        }
    }
    private static void addCell(PdfPTable t, Font f, String txt){
        PdfPCell cell = new PdfPCell(new Phrase(txt==null? "" : txt, f));
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        t.addCell(cell);
    }



    public static void writeCsvReport(List<ReportRow> list, File target) throws IOException {
       DateTimeFormatter dtCsv = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        try (var bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(target), StandardCharsets.UTF_8))) {
            bw.write(String.join(",", "ID","Tanggal","Status","Customer","Ref ID","RC","Pesan",
                    "Kategori","Produk","Deskripsi","Denom","Harga Beli","Harga Jual"));
            bw.newLine();
            for (ReportRow r : list) {
                String tanggal = r.getTransDate()==null ? "" : dtCsv.format(r.getTransDate());
                bw.write(csv(n(r.getId())));            bw.write(',');
                bw.write(csv(tanggal));               bw.write(',');
                bw.write(csv(s(r.getStatus())));         bw.write(',');
                bw.write(csv(s(r.getCustomerId())));     bw.write(',');
                bw.write(csv(s(r.getRefId())));          bw.write(',');
                bw.write(csv(s(r.getRc())));             bw.write(',');
                bw.write(csv(s(r.getProviderMessage())));bw.write(',');
                bw.write(csv(s(r.getCategory())));       bw.write(',');
                bw.write(csv(s(r.getProductCode())));    bw.write(',');
                bw.write(csv(s(r.getDescription())));    bw.write(',');
                bw.write(csv(n(r.getDenom())));          bw.write(',');
                bw.write(csv(n(r.getBuyPrice())));       bw.write(',');
                bw.write(csv(n(r.getUnitPrice())));
                bw.newLine();
            }
        }
    }
    private static String s(String v){ return v==null? "" : v; }
    private static String n(Object v){ return v==null? "" : String.valueOf(v); }
    private static String csv(String v){
        if (v==null) return "";
        boolean need = v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r");
        String esc = v.replace("\"","\"\"");
        return need ? "\"" + esc + "\"" : esc;
    }

    private static String fmtRp(BigDecimal v, NumberFormat nf) {
        return v == null ? "-" : nf.format(v);
    }


}
