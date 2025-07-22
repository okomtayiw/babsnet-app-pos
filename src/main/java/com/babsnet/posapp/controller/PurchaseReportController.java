package com.babsnet.posapp.controller;

import com.babsnet.posapp.model.PurchaseDetailReportRow;
import com.babsnet.posapp.repository.PurchaseReportRepository;
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

public class PurchaseReportController {

    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private TableView<PurchaseDetailReportRow> detailTable;
    @FXML private TableColumn<PurchaseDetailReportRow, Number> colNo;
    @FXML private TableColumn<PurchaseDetailReportRow, String> colDate;
    @FXML private TableColumn<PurchaseDetailReportRow, String> colNumber;
    @FXML private TableColumn<PurchaseDetailReportRow, String> colProduct;
    @FXML private TableColumn<PurchaseDetailReportRow, Integer> colQty;
    @FXML private TableColumn<PurchaseDetailReportRow, Double> colBuyPrice;
    @FXML private TableColumn<PurchaseDetailReportRow, Double> colSubtotal;
    @FXML private TableColumn<PurchaseDetailReportRow, String> colSupplier;
    @FXML private TableColumn<PurchaseDetailReportRow, String> colUser;
    @FXML public Button exportPdfButton;
    @FXML private Label totalSubtotalLabel;

    private final PurchaseReportRepository repo = new PurchaseReportRepository();

    @FXML
    public void initialize() {
        colNo.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(detailTable.getItems().indexOf(cellData.getValue()) + 1));
        colDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPurchaseDate()));
        colDate.setCellFactory(col -> new TableCell<PurchaseDetailReportRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : DateUtil.formatIsoToNice(item));
            }
        });
        colNumber.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPurchaseNumber()));
        colProduct.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProductName()));
        colQty.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getQty()).asObject());
        colBuyPrice.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getBuyPrice()).asObject());
        colBuyPrice.setCellFactory(col -> new TableCell<PurchaseDetailReportRow, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : FormatUtil.toRupiah(item));
            }
        });
        colSubtotal.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getSubtotal()).asObject());
        colSubtotal.setCellFactory(col -> new TableCell<PurchaseDetailReportRow, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : FormatUtil.toRupiah(item));
            }
        });
        colSupplier.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSupplierName()));
        colUser.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUserName()));
        detailTable.getItems().addListener((javafx.collections.ListChangeListener<PurchaseDetailReportRow>) c -> updateTotalSubtotalLabel());
        detailTable.getItems().clear();

        updateTotalSubtotalLabel();
    }

    private void updateTotalSubtotalLabel() {
        double total = detailTable.getItems().stream().mapToDouble(PurchaseDetailReportRow::getSubtotal).sum();
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
        List<PurchaseDetailReportRow> rows = repo.findPurchaseDetailsReport(start, end);
        detailTable.getItems().setAll(rows);
        updateTotalSubtotalLabel();
    }

    @FXML
    private void handleExportDetailPdf() {
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();
        List<PurchaseDetailReportRow> reportRows = detailTable.getItems();

        if (reportRows.isEmpty()) {
            showAlert("Tidak ada data untuk diexport!");
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Simpan PDF");
        fileChooser.setInitialFileName("laporan_detail_pembelian.pdf");
        File file = fileChooser.showSaveDialog(exportPdfButton.getScene().getWindow());
        if (file == null) return;

        try {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            document.add(new Paragraph("LAPORAN PEMBELIAN DETAIL"));
            document.add(new Paragraph("Periode: " +
                    (start != null ? start : "-") +
                    " s.d " +
                    (end != null ? end : "-")));
            document.add(new Paragraph("\n"));

            Font boldFont = new Font(Font.HELVETICA, 10, Font.BOLD);
            Font regularFont = new Font(Font.HELVETICA, 10, Font.NORMAL);

            // Contoh: No., Tanggal, No. Pembelian, Nama Produk, Qty, Harga Beli, Subtotal, Supplier, User
            PdfPTable table = new PdfPTable(9);
            table.setWidthPercentage(100);
            table.setWidths(new int[]{7, 12, 18, 20, 8, 12, 15, 15, 15});

            String[] headers = {"No.", "Tanggal", "No. Pembelian", "Nama Produk", "Qty", "Harga Beli", "Subtotal", "Supplier", "User"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, boldFont));
                cell.setBackgroundColor(new Color(230, 230, 230));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(5f);
                table.addCell(cell);
            }

            int no = 1;
            double totalSubtotal = 0;
            for (PurchaseDetailReportRow row : reportRows) {
                table.addCell(new Phrase(String.valueOf(no++), regularFont));
                table.addCell(new Phrase(DateUtil.formatIsoToNice(row.getPurchaseDate()), regularFont));
                table.addCell(new Phrase(row.getPurchaseNumber(), regularFont));
                table.addCell(new Phrase(row.getProductName(), regularFont));
                table.addCell(new Phrase(String.valueOf(row.getQty()), regularFont));
                table.addCell(new Phrase(FormatUtil.toRupiah(row.getBuyPrice()), regularFont));
                table.addCell(new Phrase(FormatUtil.toRupiah(row.getSubtotal()), regularFont));
                table.addCell(new Phrase(row.getSupplierName(), regularFont));
                table.addCell(new Phrase(row.getUserName(), regularFont));

                totalSubtotal += row.getSubtotal();
            }

            // Baris TOTAL
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

            // Kosongkan supplier & user pada total
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
