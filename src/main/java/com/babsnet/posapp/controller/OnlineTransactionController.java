package com.babsnet.posapp.controller;

import com.babsnet.posapp.model.*;
import com.babsnet.posapp.util.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Window;
import javafx.util.Callback;


import java.io.File;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;



public class OnlineTransactionController {

    @FXML private Node root;

    @FXML private TextField tfSearch;
    @FXML private ComboBox<String> cbStatus;
    @FXML private Button btnRefresh;
    @FXML private ProgressIndicator loading;

    @FXML private Button btnCheckStatus;
    @FXML private Button btnPrintStruk;
    @FXML private Button btnDownload;

    @FXML private TableView<OnlineTxnJoined> table;
    @FXML private TableColumn<OnlineTxnJoined, Long> colId;
    @FXML private TableColumn<OnlineTxnJoined, String> colDate;
    @FXML private TableColumn<OnlineTxnJoined, String> colStatus;
    @FXML private TableColumn<OnlineTxnJoined, String> colCustomer;
    @FXML private TableColumn<OnlineTxnJoined, String> colCategory;
    @FXML private TableColumn<OnlineTxnJoined, String> colProduct;
    @FXML private TableColumn<OnlineTxnJoined, BigDecimal> colUnit;
    @FXML private TableColumn<OnlineTxnJoined, String> colMessage;
    @FXML private TableColumn<OnlineTxnJoined, String> colRef;
    @FXML private TableColumn<OnlineTxnJoined, String> colAction;

    private final com.babsnet.posapp.repository.OnlineTransactionDao onlineDao = new com.babsnet.posapp.repository.OnlineTransactionDao();

    private final ObservableList<OnlineTxnJoined> master = FXCollections.observableArrayList();
    private final FilteredList<OnlineTxnJoined> filtered = new FilteredList<>(master);
    private SortedList<OnlineTxnJoined> sorted;

    private final NumberFormat rupiah = NumberFormat.getInstance(new Locale("id", "ID"));
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
    private final ApiClient api = IakCodeUtil.buildApiClient();
    @FXML
    public void initialize() {
        // top filters
        cbStatus.setItems(FXCollections.observableArrayList("", "PENDING", "SUCCESS", "FAILED", "CANCELLED"));
        cbStatus.getSelectionModel().selectFirst();

        loading.setVisible(false);
        btnRefresh.disableProperty().bind(loading.visibleProperty());

        // table columns
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colDate.setCellValueFactory(cd -> javafx.beans.binding.Bindings.createStringBinding(
                () -> cd.getValue().getTransDate() == null ? "-" : dtf.format(cd.getValue().getTransDate())
        ));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colCustomer.setCellValueFactory(new PropertyValueFactory<>("customerId"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colProduct.setCellValueFactory(new PropertyValueFactory<>("productCode"));
        colUnit.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        colMessage.setCellValueFactory(new PropertyValueFactory<>("providerMessage"));
        colRef.setCellValueFactory(new PropertyValueFactory<>("refId"));

        // format rupiah
        colUnit.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : rupiah.format(v));
            }
        });

        btnCheckStatus.disableProperty().bind(
                table.getSelectionModel().selectedItemProperty().isNull()
        );
        btnPrintStruk.disableProperty().bind(
                table.getSelectionModel().selectedItemProperty().isNull()
        );
        if (btnDownload != null){
            btnDownload.setOnAction(this::downloadReport);
        }




        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("Tidak ada data"));
        table.setItems(filtered);

        // sorting + filtering
        sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);

        tfSearch.textProperty().addListener((o, old, v) -> applyFilter());
        cbStatus.valueProperty().addListener((o, old, v) -> applyFilter());
        btnRefresh.setOnAction(e -> loadData());
        if (btnCheckStatus != null) btnCheckStatus.setOnAction(this::checkStatusSelected);
        if (btnPrintStruk != null) btnPrintStruk.setOnAction(this::printReceiptSelected);
        setupActionColumn();

        // auto load
        Platform.runLater(this::loadData);
    }

    private void downloadReport(ActionEvent e) {
        Window owner = (root != null && root.getScene() != null) ? root.getScene().getWindow() : null;

        Optional<ExportParams> pickedOpt = askExportParams(owner);
        if (pickedOpt.isEmpty()) return;
        ExportParams picked = pickedOpt.get();

        // pakai inclusive end versi kamu (00:00:00 s.d. 23:59:59)
        LocalDateTime start = picked.getStart().atStartOfDay();
        LocalDateTime end   = picked.getEnd().atTime(23, 59, 59);

        String ext = picked.getExportFormat() == ExportFormatEnum.CSV ? "csv" : "pdf";
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(ext.toUpperCase() + " Files", "*." + ext));
        String fname = String.format("laporan-transaksi_%s_%s.%s", picked.getStart(), picked.getEnd(), ext);
        fc.setInitialFileName(fname);
        File chosen = fc.showSaveDialog(owner);
        if (chosen == null) return;
        File target = ensureExtension(chosen, ext);

        Task<File> task = new Task<>() {
            @Override protected File call() {
                updateMessage("Mengambil data…");
                try {
                    List<ReportRow> list = onlineDao.listReportRowsByDateRange(start, end);
                    updateMessage(ext.equals("csv") ? "Menulis CSV…" : "Menyusun PDF…");
                    if (ext.equals("csv")) {
                        Utils.writeCsvReport(list, target);
                    } else {
                        Utils.writePdfReport(list, target, picked.getStart(), picked.getEnd());
                    }
                    return target;
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        };

        Dialog<Void> loadingDlg = Utils.buildLoadingDialog("Membuat Laporan", "Mohon tunggu…", task, owner);
        loadingDlg.show();

        task.setOnSucceeded(ev -> MessageDialogUtil.showInfo("Laporan tersimpan:\n" + target.getAbsolutePath()));
        task.setOnFailed(ev -> {
            Throwable ex = task.getException();
            MessageDialogUtil.showError("Gagal membuat laporan: " + (ex != null ? ex.getMessage() : "unknown error"));
        });

        Thread th = new Thread(task, "export-report");
        th.setDaemon(true);
        th.start();
    }

    private Optional<ExportParams> askExportParams(Window owner) {

        Optional<DateRange> dr = askDateRange(owner);
        if (dr.isEmpty()) return Optional.empty();

        ChoiceDialog<ExportFormatEnum> fmtDlg = new ChoiceDialog<>(ExportFormatEnum.CSV, ExportFormatEnum.values());
        fmtDlg.setTitle("Pilih Format Laporan");
        fmtDlg.setHeaderText("Pilih format laporan");
        fmtDlg.setContentText("Format:");
        if (owner != null) fmtDlg.initOwner(owner);

        Optional<ExportFormatEnum> fmt = fmtDlg.showAndWait();
        if (fmt.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new ExportParams(dr.get().getStart(), dr.get().getEnd(), fmt.get()));
    }

    private Optional<DateRange> askDateRange(Window owner) {
        Dialog<DateRange> dlg = new Dialog<>();
        dlg.setTitle("Rentang Tanggal Laporan");
        if (owner != null) dlg.initOwner(owner);
        dlg.initModality(Modality.WINDOW_MODAL);

        ButtonType okType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

        // Default: 7 hari terakhir
        DatePicker dpStart = new DatePicker(LocalDate.now().minusDays(7));
        DatePicker dpEnd   = new DatePicker(LocalDate.now());


        Callback<DatePicker, DateCell> pastOnly = picker -> new DateCell() {
            @Override public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (!empty && date.isAfter(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-opacity: 0.5;");
                }
            }
        };
        dpStart.setDayCellFactory(pastOnly);
        dpEnd.setDayCellFactory(pastOnly);

        // UI layout
        GridPane gp = new GridPane();
        gp.setHgap(10);
        gp.setVgap(8);
        gp.setPadding(new Insets(12));
        gp.addRow(0, new Label("Start Date"), dpStart);
        gp.addRow(1, new Label("End Date"),   dpEnd);

        dlg.getDialogPane().setContent(gp);


        Node okBtn = dlg.getDialogPane().lookupButton(okType);
        Runnable validate = () -> {
            LocalDate s = dpStart.getValue();
            LocalDate e = dpEnd.getValue();
            boolean invalid = (s == null || e == null || s.isAfter(e));
            okBtn.setDisable(invalid);
        };
        dpStart.valueProperty().addListener((o, a, b) -> validate.run());
        dpEnd.valueProperty().addListener((o, a, b) -> validate.run());
        validate.run();

        // Hasil dialog
        dlg.setResultConverter(bt -> {
            if (bt == okType) {
                return new DateRange(dpStart.getValue(), dpEnd.getValue());
            }
            return null;
        });

        Platform.runLater(dpStart::requestFocus);
        return dlg.showAndWait();
    }

    private static File ensureExtension(File f, String ext) {
        String name = f.getName().toLowerCase(Locale.ROOT);
        return name.endsWith("." + ext) ? f : new File(f.getParentFile(), f.getName() + "." + ext);
    }





    private void setupActionColumn() {
        colAction.setText("Aksi");
        colAction.setCellFactory(col -> new TableCell<OnlineTxnJoined, String>() {
            private final Button btnDetail = new Button("Detail");
            private final Button btnDelete = new Button("Delete");
            private final HBox box = new HBox(8, btnDetail, btnDelete);

            {
                box.setFillHeight(true);

                btnDetail.setOnAction(e -> {
                    OnlineTxnJoined row = getCurrentRow();
                    if (row != null) viewDetail(row);
                });

                btnDelete.setOnAction(e -> {
                    OnlineTxnJoined row = getCurrentRow();
                    if (row != null) deleteTransaction(row);
                });

                // opsional: tooltip biar UX enak
                btnDetail.setTooltip(new Tooltip("Lihat detail transaksi"));
                btnDelete.setTooltip(new Tooltip("Hapus transaksi (hanya untuk FAILED)"));
            }

            private OnlineTxnJoined getCurrentRow() {
                int idx = getIndex();
                if (idx < 0 || idx >= getTableView().getItems().size()) return null;
                return getTableView().getItems().get(idx);
            }

            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                OnlineTxnJoined r = getCurrentRow();
                if (r == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                // hanya FAILED yang boleh dihapus
                boolean deletable = "FAILED".equalsIgnoreCase(r.getStatus());
                btnDelete.setDisable(!deletable);

                setGraphic(box);
                setText(null);
            }
        });
        colAction.setMinWidth(100);
    }

    private void deleteTransaction(OnlineTxnJoined row) {
        if (!"FAILED".equalsIgnoreCase(row.getStatus())) {
            MessageDialogUtil.showError("Hanya transaksi berstatus FAILED yang bisa dihapus.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Hapus transaksi dengan Ref ID: " + row.getRefId() + " ?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Konfirmasi Hapus");
        var ans = confirm.showAndWait();
        if (ans.isEmpty() || ans.get() != ButtonType.YES) return;

        Task<Integer> task = new Task<>() {
            @Override protected Integer call() {
                updateMessage("Menghapus transaksi…");
                try {
                    return onlineDao.deleteCascade(row.getId());
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        };


        Window owner = (root != null && root.getScene() != null) ? root.getScene().getWindow() : null;
        Dialog<Void> loadingDlg = Utils.buildLoadingDialog(
                "Menghapus", "Memproses penghapusan…", task, owner);
        loadingDlg.show();


        task.setOnSucceeded(e -> {
            Integer affected = task.getValue();
            if (affected != null && affected > 0) {

                master.remove(row);
                MessageDialogUtil.showInfo("Transaksi berhasil dihapus.");
            } else {
                MessageDialogUtil.showError("Tidak ada baris yang dihapus. Pastikan status di DB = FAILED.");
            }

        });


        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            MessageDialogUtil.showError("Gagal menghapus: " + (ex != null ? ex.getMessage() : "unknown error"));

        });


        task.setOnCancelled(e -> {
            if (loadingDlg.isShowing()) loadingDlg.hide();
        });

        Thread th = new Thread(task, "delete-txn-task");
        th.setDaemon(true);
        th.start();
    }



    private void viewDetail(OnlineTxnJoined row) {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Detail Transaksi");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);

        GridPane gp = new GridPane();
        gp.setHgap(10);
        gp.setVgap(6);
        gp.setPadding(new javafx.geometry.Insets(12));

        int r = 0;
        gp.addRow(r++, new Label("Tanggal"), new Label(row.getTransDate() == null ? "-" : dtf.format(row.getTransDate())));
        gp.addRow(r++, new Label("Customer"), new Label(nullToDash(row.getCustomerId())));
        gp.addRow(r++, new Label("Kategori"), new Label(nullToDash(row.getCategory())));
        gp.addRow(r++, new Label("Produk"),   new Label(nullToDash(row.getProductCode())));
        gp.addRow(r++, new Label("Status"),   new Label(nullToDash(row.getStatus())));
        gp.addRow(r++, new Label("RC"),       new Label(nullToDash(row.getRc())));
        gp.addRow(r++, new Label("Pesan"),    new Label(nullToDash(row.getProviderMessage())));
        gp.addRow(r++, new Label("Ref ID"),   new Label(nullToDash(row.getRefId())));
        gp.addRow(r++, new Label("Nominal"),  new Label(row.getUnitPrice()==null ? "-" : rupiah.format(row.getUnitPrice())));

        dlg.getDialogPane().setContent(gp);
        dlg.show();
    }

    private static String nullToDash(String s) { return (s == null || s.isBlank()) ? "-" : s; }



    private void printReceiptSelected(ActionEvent e) {
        OnlineTxnJoined row = table.getSelectionModel().getSelectedItem();
        if (row == null) {
            MessageDialogUtil.showError("Pilih salah satu transaksi dulu.");
            return;
        }

        setLoading(true);
        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return onlineDao.findById(row.getId()).orElse(null);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                })
                .whenComplete((tx, err) -> Platform.runLater(() -> {
                    setLoading(false);
                    if (err != null) {
                        MessageDialogUtil.showError("Gagal ambil data transaksi: " + err.getMessage());
                        return;
                    }
                    if (tx == null) {
                        MessageDialogUtil.showError("Data transaksi tidak ditemukan.");
                        return;
                    }

                    Window owner = (root != null && root.getScene() != null) ? root.getScene().getWindow() : null;
                    ThermalPrintPreviewDialog.show(owner, tx);
                }));
    }


    private void applyFilter() {
        String q = (tfSearch.getText() == null ? "" : tfSearch.getText().trim().toLowerCase(Locale.ROOT));
        String st = cbStatus.getValue();

        filtered.setPredicate(row -> {
            if (row == null) return false;

            boolean matchStatus = (st == null || st.isBlank()) || st.equalsIgnoreCase(row.getStatus());
            boolean matchSearch = q.isBlank()
                    || contains(row.getTransactionNumber(), q)
                    || contains(row.getCustomerId(), q)
                    || contains(row.getCategory(), q)
                    || contains(row.getProductCode(), q)
                    || contains(row.getDescription(), q)
                    || contains(row.getRefId(), q)
                    || contains(row.getRc(), q);

            return matchStatus && matchSearch;
        });
    }

    private boolean contains(String s, String q) {
        return s != null && s.toLowerCase(Locale.ROOT).contains(q);
    }

    private void setLoading(boolean v) {
        loading.setVisible(v);
    }

    private void loadData() {
        setLoading(true);

        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return onlineDao.listJoined(500);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .whenComplete((list, err) -> Platform.runLater(() -> {
                    setLoading(false);
                    if (err != null) {
                        new Alert(Alert.AlertType.ERROR, "Gagal memuat data: " + err.getMessage()).show();
                        master.clear();
                        return;
                    }
                    master.setAll(list);
                    applyFilter();
                }));
    }

    @FXML
    private void openProductDialogPrePaid() {
        Window owner = (root != null && root.getScene() != null) ? root.getScene().getWindow() : null;
        new PriceListDialogPrepaid(owner).show();
    }
    @FXML
    public void openProductDialogPostPaid(ActionEvent actionEvent) {
        Window owner = (root != null && root.getScene() != null) ? root.getScene().getWindow() : null;
        new PriceListDialogPostpaid(owner).show();
    }

    // ================== AKSI: CHECK STATUS ==================
    private void checkStatusSelected(javafx.event.ActionEvent e) {
        OnlineTxnJoined row = table.getSelectionModel().getSelectedItem();
        if (row == null) {
            MessageDialogUtil.showError("Pilih salah satu transaksi dulu.");
            return;
        }
        if (row.getRefId() == null || row.getRefId().isBlank()) {
            MessageDialogUtil.showError("Transaksi tidak memiliki Ref ID.");
            return;
        }

        setLoading(true);
        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        var opt = onlineDao.findByRefId(row.getRefId());
                        OnlineTransaction ot = opt.orElseThrow(() -> new SQLException("Transaksi tidak ditemukan: " + row.getRefId()));

                        if ("POSTPAID".equalsIgnoreCase(ot.paymentMethod)) {
                            InquiryResponsePostPaid r = api.checkStatusPostPaid(row.getRefId());
                            if (r == null || r.data == null) throw new IllegalStateException("Respons POSTPAID kosong");
                            return r;
                        } else {
                            PrepaidTopupResponse r = api.checkStatusTopup(row.getRefId());
                            if (r == null || r.data == null) throw new IllegalStateException("Respons TOPUP kosong");
                            return r;
                        }
                    } catch (SQLException ex) {
                        throw new RuntimeException(ex);
                    }
                })
                .whenComplete((response, err) -> Platform.runLater(() -> {
                    setLoading(false);
                    if (err != null) {
                        MessageDialogUtil.showError("Gagal cek status: " + err.getMessage());
                        return;
                    }
                    switch (response) {
                        case null -> {
                            MessageDialogUtil.showError("Respons cek status tidak valid (null).");
                            return;
                        }
                        case InquiryResponsePostPaid p -> {
                            if (p.data == null) {
                                MessageDialogUtil.showError("Respons POSTPAID tidak valid (data null).");
                                return;
                            }

                            var info = IakCodeUtil.classifyRc(p.data.response_code);
                            OnlineTxnStatus newStatus = OnlineTxnStatus.fromIak(info.getStatus());

                            // update DB
                            try {
                                Long trId = p.data.tr_id;
                                Long trIdParam = null;
                                if (trId != null && trId != 0L) {
                                    trIdParam = trId;
                                }
                                onlineDao.updateAfterProviderResponse(
                                        row.getId(),
                                        p.data.response_code,
                                        0,
                                        p.data.message,
                                        trIdParam,
                                        p.data.noref,
                                        ApiClient.toJsonSafely(p),
                                        newStatus
                                );
                            } catch (Exception ex) {
                                MessageDialogUtil.showError("Status terambil, tapi gagal update DB: " + ex.getMessage());
                            }

                            row.setRc(p.data.response_code);
                            row.setProviderMessage(p.data.message);
                            row.setStatus(newStatus.name());
                            table.refresh();

                            // feedback ke user
                            switch (info.getStatus()) {
                                case SUCCESS -> {
                                    MessageDialogUtil.showInfo(p.data.message);
                                }
                                case FAILED -> MessageDialogUtil.showError(info.getSolution());
                            }
                        }
                        case PrepaidTopupResponse resp -> {
                            if (resp.data == null) {
                                MessageDialogUtil.showError("Respons TOPUP tidak valid (data null).");
                                return;
                            }

                            var info = IakCodeUtil.classifyRc(resp.data.rc);
                            OnlineTxnStatus newStatus = OnlineTxnStatus.fromIak(info.getStatus());

                            // update DB
                            try {
                                Long trId = resp.data.trId;
                                Long trIdParam = null;
                                if (trId != null && trId != 0L) {
                                    trIdParam = trId;
                                }
                                onlineDao.updateAfterProviderResponse(
                                        row.getId(),
                                        resp.data.rc,
                                        resp.data.status,
                                        resp.data.message,
                                        trIdParam,
                                        resp.data.sn,
                                        ApiClient.toJsonSafely(resp),
                                        newStatus
                                );
                            } catch (Exception ex) {
                                // lanjutkan walau gagal update DB, tapi infokan
                                MessageDialogUtil.showError("Status terambil, tapi gagal update DB: " + ex.getMessage());
                            }

                            // update tampilan baris
                            row.setRc(resp.data.rc);
                            row.setProviderMessage(resp.data.message);
                            row.setStatus(newStatus.name());
                            table.refresh();

                            // feedback ke user
                            switch (info.getStatus()) {
                                case SUCCESS -> {
                                    String ok = "Transaksi berhasil"
                                            + ((resp.data.trId != 0) ? "\nTRX: " + resp.data.trId : "")
                                            + ((resp.data.sn != null && !resp.data.sn.isBlank()) ? "\nSN : " + resp.data.sn : "");
                                    MessageDialogUtil.showInfo(ok);
                                }
                                case FAILED -> MessageDialogUtil.showError(info.getSolution());
                                case PENDING -> {
                                    // bila benar-benar masih proses, jalankan progress modeless
                                    if (IakCodeUtil.isStillProcessing(resp.data.rc, resp.data.message, resp.data.status)) {
                                        Window owner = root.getScene() != null ? root.getScene().getWindow() : null;
                                        ProcessLoading.showProcessingAndWaitSimple(api, resp.data.refId, owner);
                                    } else {
                                        MessageDialogUtil.showInfo(info.getSolution());
                                    }
                                }
                            }
                        }
                        default -> {
                            MessageDialogUtil.showError("Tipe respons tidak dikenali: " + response.getClass().getSimpleName());
                            return;
                        }
                    }

                }));
    }

}
