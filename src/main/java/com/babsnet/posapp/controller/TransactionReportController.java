package com.babsnet.posapp.controller;

import com.babsnet.posapp.model.Transaction;
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
import java.io.File;
import java.io.FileOutputStream;
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
        LocalDate end = endDatePicker.getValue();
        List<TransactionDetailReportRow> reportRows = detailTable.getItems();

        if (reportRows.isEmpty()) {
            showAlert("Tidak ada data untuk diexport!");
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Simpan PDF");
        fileChooser.setInitialFileName("laporan_detail_penjualan.pdf");
        File file = fileChooser.showSaveDialog(exportPdfButton.getScene().getWindow());
        if (file == null) return;

        try {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            document.add(new Paragraph("LAPORAN PENJUALAN DETAIL"));
            document.add(new Paragraph("Periode: " +
                    (start != null ? start : "-") +
                    " s.d " +
                    (end != null ? end : "-")));
            document.add(new Paragraph("\n"));

            // Font untuk header dan total
            Font boldFont = new Font(Font.HELVETICA, 10, Font.BOLD);
            Font regularFont = new Font(Font.HELVETICA, 10, Font.NORMAL);

            // Tabel 9 kolom (No. + data)
            PdfPTable table = new PdfPTable(9);
            table.setWidthPercentage(100);
            table.setWidths(new int[]{7, 12, 18, 20, 8, 12, 15, 15, 15});

            // Header (tebal & background light gray)
            String[] headers = {"No.", "Tanggal", "No. Transaksi", "Nama Produk", "Qty", "Harga", "Subtotal", "Metode Bayar", "Kasir"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, boldFont));
                cell.setBackgroundColor(new Color(230, 230, 230));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(5f);
                table.addCell(cell);
            }

            // Data rows
            int no = 1;
            double totalSubtotal = 0;
            for (TransactionDetailReportRow row : reportRows) {
                table.addCell(new Phrase(String.valueOf(no++), regularFont));
                table.addCell(new Phrase(DateUtil.formatIsoToNice(row.getTransDate()), regularFont));
                table.addCell(new Phrase(row.getTransactionNumber(), regularFont));
                table.addCell(new Phrase(row.getProductName(), regularFont));
                table.addCell(new Phrase(String.valueOf(row.getQty()), regularFont));
                table.addCell(new Phrase(FormatUtil.toRupiah(row.getPrice()), regularFont));
                table.addCell(new Phrase(FormatUtil.toRupiah(row.getSubtotal()), regularFont));
                table.addCell(new Phrase(row.getPaymentMethod(), regularFont));
                table.addCell(new Phrase(row.getUserName(), regularFont));

                totalSubtotal += row.getSubtotal();
            }

            // Baris TOTAL (tebal & background lebih gelap)
            PdfPCell totalCell = new PdfPCell(new Phrase("TOTAL", boldFont));
            totalCell.setColspan(6);
            totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalCell.setBackgroundColor(new Color(200, 200, 200));
            totalCell.setPaddingRight(10f);
            totalCell.setPadding(6f);
            table.addCell(totalCell);

            PdfPCell totalValue = new PdfPCell(new Phrase(FormatUtil.toRupiah(totalSubtotal), boldFont));
            totalValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalValue.setBackgroundColor(new Color(200, 200, 200));
            totalValue.setPadding(6f);
            table.addCell(totalValue);

            // Kolom Metode Bayar & Kasir dikosongkan
            PdfPCell empty1 = new PdfPCell(new Phrase(""));
            empty1.setBackgroundColor(new Color(200, 200, 200));
            empty1.setPadding(6f);
            table.addCell(empty1);
            PdfPCell empty2 = new PdfPCell(new Phrase(""));
            empty2.setBackgroundColor(new Color(200, 200, 200));
            empty2.setPadding(6f);
            table.addCell(empty2);

            document.add(table);
            document.close();
            showAlert("Export PDF berhasil: " + file.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Gagal export PDF: " + e.getMessage());
        }
    }


    private void showAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
            alert.showAndWait();
        });
    }
}
