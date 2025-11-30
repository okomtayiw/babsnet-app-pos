package com.babsnet.posapp.controller;

import com.babsnet.posapp.model.TransactionDetailReportRow;
import com.babsnet.posapp.repository.TransactionReportRepository;
import com.babsnet.posapp.util.DateUtil;
import com.babsnet.posapp.util.FormatUtil;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

public class TransactionReportController {

    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private TableView<TransactionDetailReportRow> detailTable;
    @FXML private TableColumn<TransactionDetailReportRow, Number> colNo;
    @FXML private TableColumn<TransactionDetailReportRow, String> colDate;
    @FXML private TableColumn<TransactionDetailReportRow, String> colNumber;
    @FXML private TableColumn<TransactionDetailReportRow, String> colProduct;
    @FXML private TableColumn<TransactionDetailReportRow, Integer> colQty;
    @FXML private TableColumn<TransactionDetailReportRow, Double> colBuyPrice;
    @FXML private TableColumn<TransactionDetailReportRow, Double> colPrice;
    @FXML private TableColumn<TransactionDetailReportRow, Double> colSubtotal;
    @FXML private TableColumn<TransactionDetailReportRow, String> colPayment;
    @FXML private TableColumn<TransactionDetailReportRow, String> colUser;
    @FXML public Button exportPdfButton;
    @FXML public Button exportCsvButton;

    @FXML private Label totalSubtotalLabel;

    private final TransactionReportRepository repo = new TransactionReportRepository();

    @FXML
    public void initialize() {
        colNo.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(detailTable.getItems().indexOf(cellData.getValue()) + 1));
        colDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTransDate()));
        colDate.setCellFactory(col -> new TableCell<TransactionDetailReportRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(DateUtil.formatIsoToNice(item));
                }
            }
        });
        colNumber.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTransactionNumber()));
        colProduct.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProductName()));
        colQty.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getQty()).asObject());
        colBuyPrice.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getBuyPrice()).asObject());
        colBuyPrice.setCellFactory(col -> new TableCell<TransactionDetailReportRow, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(FormatUtil.toRupiah(item));
                }
            }
        });
        colPrice.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getPrice()).asObject());
        colPrice.setCellFactory(col -> new TableCell<TransactionDetailReportRow, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(FormatUtil.toRupiah(item));
                }
            }
        });
        colSubtotal.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getSubtotal()).asObject());
        colSubtotal.setCellFactory(col -> new TableCell<TransactionDetailReportRow, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(FormatUtil.toRupiah(item));
                }
            }
        });
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now());
        colPayment.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPaymentMethod()));
        colUser.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUserName()));
        detailTable.getItems().addListener((javafx.collections.ListChangeListener<TransactionDetailReportRow>) c -> updateTotalSubtotalLabel());
        detailTable.getItems().clear();


        updateTotalSubtotalLabel();
    }

    private void updateTotalSubtotalLabel() {
        double total = detailTable.getItems().stream().mapToDouble(TransactionDetailReportRow::getSubtotal).sum();
        totalSubtotalLabel.setText(FormatUtil.toRupiah(total));
    }

    @FXML
    private void handleFilter() {
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();
        if (start == null || end == null) {
            showAlert("Pilih tanggal awal dan akhir dulu!");
            return;
        }
        List<TransactionDetailReportRow> rows = repo.findTransactionDetailsReport(start, end);
        detailTable.getItems().setAll(rows);
        updateTotalSubtotalLabel();
    }

    @FXML
    private void handleExportDetailPdf() {
        LocalDate start = startDatePicker.getValue();
        LocalDate end   = endDatePicker.getValue();
        List<TransactionDetailReportRow> reportRows = detailTable.getItems();

        if (reportRows.isEmpty()) {
            showAlert("Tidak ada data untuk diexport!");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Simpan PDF");
        String defaultNamePdf = "laporan_detail_penjualan"
                + (start != null ? "_" + start : "")
                + (end   != null ? "-" + end   : "")
                + ".pdf";
        fileChooser.setInitialFileName(defaultNamePdf);
        File file = fileChooser.showSaveDialog(exportPdfButton.getScene().getWindow());
        if (file == null) return;

        try {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            document.add(new Paragraph("LAPORAN PENJUALAN DETAIL"));
            document.add(new Paragraph("Periode: " +
                    (start != null ? start : "-") + " s.d " + (end != null ? end : "-")));
            document.add(new Paragraph("\n"));

            Font boldFont    = new Font(Font.HELVETICA, 10, Font.BOLD);
            Font regularFont = new Font(Font.HELVETICA, 10, Font.NORMAL);

            // 10 kolom: No, Tanggal, No Transaksi, Nama Produk, Qty, Harga Beli, Harga Jual, Subtotal, Metode Bayar, Kasir
            PdfPTable table = new PdfPTable(10);
            table.setWidthPercentage(100);
            table.setWidths(new int[]{7, 12, 18, 20, 8, 14, 14, 16, 12, 12});

            String[] headers = {"No.", "Tanggal", "No. Transaksi", "Nama Produk", "Qty",
                    "Harga Beli", "Harga Jual", "Subtotal", "Metode Bayar", "Kasir"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, boldFont));
                cell.setBackgroundColor(new Color(230,230,230));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(5f);
                table.addCell(cell);
            }

            int    no = 1;
            int    totalQty = 0;
            double totalBeli = 0.0;
            double totalJual = 0.0;
            double totalSub  = 0.0;

            for (TransactionDetailReportRow row : reportRows) {
                int qty        = row.getQty();
                double buy     = row.getBuyPrice(); // harga beli per unit
                double sell    = row.getPrice();    // harga jual per unit
                double subtotal= row.getSubtotal(); // biasanya qty * sell

                table.addCell(new Phrase(String.valueOf(no++), regularFont));
                table.addCell(new Phrase(DateUtil.formatIsoToNice(row.getTransDate()), regularFont));
                table.addCell(new Phrase(row.getTransactionNumber(), regularFont));
                table.addCell(new Phrase(row.getProductName(), regularFont));
                table.addCell(new Phrase(String.valueOf(qty), regularFont));
                table.addCell(rightCell(FormatUtil.toRupiah(buy),  regularFont));   // Harga Beli
                table.addCell(rightCell(FormatUtil.toRupiah(sell), regularFont));   // Harga Jual
                table.addCell(rightCell(FormatUtil.toRupiah(subtotal), regularFont)); // Subtotal
                table.addCell(new Phrase(row.getPaymentMethod(), regularFont));
                table.addCell(new Phrase(row.getUserName(), regularFont));

                totalQty  += qty;
                totalBeli += qty * buy;
//                totalJual += qty * sell;
                totalSub  += subtotal;
            }

            // ===== Row TOTAL =====
            PdfPCell totalLabel = new PdfPCell(new Phrase("TOTAL", boldFont));
            totalLabel.setColspan(4);
            totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalLabel.setBackgroundColor(new Color(200,200,200));
            totalLabel.setPadding(6f);
            totalLabel.setNoWrap(true);
            table.addCell(totalLabel);

            table.addCell(centerCell(String.valueOf(totalQty), boldFont));                 // Qty
            PdfPCell totalBeliCell = rightCell(FormatUtil.toRupiah(totalBeli), boldFont);  // Harga Beli
            totalBeliCell.setBackgroundColor(new Color(200,200,200));
            table.addCell(totalBeliCell);

            PdfPCell totalJualCell = new PdfPCell(new Phrase(""));  // Harga Jual
            totalJualCell.setBackgroundColor(new Color(200,200,200));
            table.addCell(totalJualCell);

            PdfPCell totalSubCell = rightCell(FormatUtil.toRupiah(totalSub), boldFont);    // Subtotal
            totalSubCell.setBackgroundColor(new Color(200,200,200));
            table.addCell(totalSubCell);

            // kolom 9 & 10 kosong
            PdfPCell empty9  = new PdfPCell(new Phrase(""));
            empty9.setBackgroundColor(new Color(200,200,200));
            empty9.setPadding(6f);
            empty9.setNoWrap(true);
            table.addCell(empty9);

            PdfPCell empty10 = new PdfPCell(new Phrase(""));
            empty10.setBackgroundColor(new Color(200,200,200));
            empty10.setPadding(6f);
            empty10.setNoWrap(true);
            table.addCell(empty10);

            document.add(table);
            document.close();
            showAlert("Export PDF berhasil: " + file.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Gagal export PDF: " + e.getMessage());
        }
    }


    // helper kecil biar rapi
    private PdfPCell rightCell(String text, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(6f);
        c.setNoWrap(true);           // <- kunci utama: jangan wrap
        return c;
    }
    private PdfPCell centerCell(String text, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(6f);
        c.setNoWrap(true);
        return c;
    }




    private void showAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
            alert.showAndWait();
        });
    }

    @FXML
    private void handleExportDetailCsv() {
        List<TransactionDetailReportRow> reportRows = detailTable.getItems();
        if (reportRows == null || reportRows.isEmpty()) {
            showAlert("Tidak ada data untuk diexport!");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Simpan CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        LocalDate start = startDatePicker.getValue();
        LocalDate end   = endDatePicker.getValue();
        String defaultName = "laporan_detail_penjualan"
                + (start != null ? "_" + start : "")
                + (end   != null ? "-" + end   : "")
                + ".csv";
        fileChooser.setInitialFileName(defaultName);
        File file = fileChooser.showSaveDialog(exportCsvButton.getScene().getWindow());
        if (file == null) return;

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {

            // Tulis BOM agar Excel Windows mendeteksi UTF-8 dengan benar (terutama untuk teks Indonesia)
            writer.write('\uFEFF');

            // Header
            String[] headers = {"No.", "Tanggal", "No. Transaksi", "Nama Produk", "Qty", "Harga Beli","Harga Jual", "Subtotal", "Metode Bayar", "Kasir"};
            writer.write(String.join(",", headers));
            writer.newLine();

            // Data
            int no = 1;
            double totalSubtotal = 0.0;

            for (TransactionDetailReportRow row : reportRows) {
                String tanggal = DateUtil.formatIsoToNice(row.getTransDate());
                String number = row.getTransactionNumber();
                String product = row.getProductName();
                int qty = row.getQty();
                double buyPrice = row.getBuyPrice();
                double price = row.getPrice();
                double subtotal = row.getSubtotal();
                String payment = row.getPaymentMethod();
                String user = row.getUserName();

                totalSubtotal += subtotal;

                // Tulis baris (angka dibiarkan numerik, tidak dibungkus quote; teks di-escape)
                String line = String.join(",",
                        String.valueOf(no++),
                        csv(tanggal),
                        csv(number),
                        csv(product),
                        String.valueOf(qty),
                        csv(FormatUtil.toRupiah(buyPrice)),
                        csv(FormatUtil.toRupiah(price)),
                        csv(FormatUtil.toRupiah(subtotal)),
                        csv(payment),
                        csv(user)
                );
                writer.write(line);
                writer.newLine();
            }

            // Baris TOTAL (letakkan label di kolom 6 agar mirip tabel PDF)
            String totalLine = String.join(",",
                    "", "", "", "", "", "TOTAL",
                    csv(FormatUtil.toRupiah(totalSubtotal)),
                    "", ""
            );
            writer.write(totalLine);
            writer.newLine();

            writer.flush();
            showAlert("Export CSV berhasil: " + file.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Gagal export CSV: " + e.getMessage());
        }
    }

    private static String csv(String s) {
        if (s == null) return "";
        boolean needQuote = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        String v = s.replace("\"", "\"\"");
        return needQuote ? "\"" + v + "\"" : v;
    }

}
