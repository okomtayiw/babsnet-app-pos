package com.babsnet.posapp.util;

import com.babsnet.posapp.model.*;
import com.babsnet.posapp.repository.OnlineTransactionDao;
import com.babsnet.posapp.session.SessionManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.babsnet.posapp.util.ProcessLoading.showProcessingAndWaitSimple;

public class PriceListDialogPrepaid extends Dialog<Void> {

    // UI
    private final TextField tfSearch = new TextField();
    private final ComboBox<String> cbCategory = new ComboBox<>();
    private final Button btnRefresh = new Button("Refresh");
    private final ProgressIndicator loading = new ProgressIndicator();

    private final TableView<PriceItem> table = new TableView<>();
    private final TableColumn<PriceItem, String> colCode   = new TableColumn<>("Code");
    private final TableColumn<PriceItem, String> colDesc   = new TableColumn<>("Description");
    private final TableColumn<PriceItem, String> colNom    = new TableColumn<>("Nominal");
    private final TableColumn<PriceItem, String> colCat    = new TableColumn<>("Category");
    private final TableColumn<PriceItem, Long>   colPrice  = new TableColumn<>("Price");
    private final TableColumn<PriceItem, String> colType   = new TableColumn<>("Type");
    private final TableColumn<PriceItem, String> colStatus = new TableColumn<>("Status");

    private final Label statusLabel = new Label("Memuat…");

    // Data
    private final ObservableList<PriceItem> master = FXCollections.observableArrayList();
    private final FilteredList<PriceItem> filtered = new FilteredList<>(master);
    private SortedList<PriceItem> sorted;
    private final OnlineTransactionDao onlineDao = new OnlineTransactionDao();

    private final NumberFormat rupiah = NumberFormat.getInstance(new Locale("id", "ID"));
    private final ApiClient api = IakCodeUtil.buildApiClient();
    long totalFee = Long.parseLong(ConfigUtil.get("fee.payment.online.prepaid"));
    long feeNonNominal = Long.parseLong(ConfigUtil.get("fee.payment.online.prepaid.non.nominal"));
    private final Label lblBalance = new Label("Rp 0");
    User currentUser = SessionManager.getInstance().getCurrentUser();

    public PriceListDialogPrepaid(Window owner) {
        setTitle("Daftar Produk");
        setHeaderText("Cari & pilih produk. Double-click baris untuk transaksi.");

        // === KUNCI: dialog ini modeless (tidak memblok UI lain) ===
        if (owner != null) initOwner(owner);
        initModality(Modality.NONE); // <—

        getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
        getDialogPane().setPrefSize(900, 600);

        // Fokuskan search ketika dialog tampil
        setOnShown(e -> Platform.runLater(tfSearch::requestFocus));

        buildUI();
        wireEvents();
        initTable();
        loadBalance();
        loadData();
    }

    private void loadBalance() {
        javafx.concurrent.Task<Long> task = new javafx.concurrent.Task<>() {
            @Override protected Long call() throws Exception {
                // method sinkron: bisa kamu set breakpoint di sini (di ApiClient)
                return api.fetchBalanceSync();
            }
        };

        task.setOnSucceeded(e -> {
            Long bal = task.getValue();
            lblBalance.setText("Rp " + rupiah.format(bal == null ? 0L : bal));
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            lblBalance.setText("—");
            // mudah di-debug: stacktrace kelihatan di console
            ex.printStackTrace();
            MessageDialogUtil.showError("Fetch balance gagal: " + ex.getMessage());
        });

        new Thread(task, "balance-task").start(); // non-blocking UI
    }

    private void buildUI() {
        // Top bar
        tfSearch.setPromptText("Cari kode/nominal/deskripsi…");
        cbCategory.setPromptText("Semua kategori");

        loading.setVisible(false);
        loading.setPrefSize(18, 18);


        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label balanceTitle = new Label("Balance:");
        balanceTitle.setStyle("-fx-font-weight: bold;");
        lblBalance.setStyle("-fx-font-weight: bold; -fx-text-fill: #2e7d32;"); // hijau elegan
        HBox balanceBox = new HBox(6, balanceTitle, lblBalance);
        balanceBox.setAlignment(Pos.CENTER_RIGHT);

        HBox topBar = new HBox(10,
                new Label("Search"), tfSearch,
                new Label("Kategori"), cbCategory,
                btnRefresh, loading, spacer, balanceBox
        );
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(10));
        HBox.setHgrow(tfSearch, Priority.ALWAYS);

        // Table
        table.setPlaceholder(new Label("Tidak ada data"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.getColumns().setAll(colCode, colDesc, colNom, colCat, colPrice, colType, colStatus);

        // Bottom bar
        HBox bottom = new HBox(statusLabel);
        bottom.setAlignment(Pos.CENTER_LEFT);
        bottom.setPadding(new Insets(8, 10, 8, 10));
        bottom.setStyle("-fx-background-color: -fx-base;");

        BorderPane root = new BorderPane(table, topBar, null, bottom, null);
        getDialogPane().setContent(root);
    }

    private void wireEvents() {
        tfSearch.textProperty().addListener((o, old, val) -> applyFilter());
        cbCategory.valueProperty().addListener((o, old, val) -> applyFilter());
        btnRefresh.setOnAction(e -> loadData());
        btnRefresh.disableProperty().bind(loading.visibleProperty());

        // double-click row => buka dialog transaksi
        table.setRowFactory(tv -> {
            TableRow<PriceItem> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (!row.isEmpty() && e.getClickCount() == 2) {
                    showPhonePriceDialog(row.getItem());
                }
            });
            return row;
        });
    }

    private void initTable() {
        colCode.setCellValueFactory(new PropertyValueFactory<>("code"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nominal"));
        colCat.setCellValueFactory(new PropertyValueFactory<>("category"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colPrice.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Long value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : rupiah.format(value));
            }
        });

        colCat.setSortType(TableColumn.SortType.ASCENDING);
        colType.setSortType(TableColumn.SortType.ASCENDING);
        colPrice.setComparator(Comparator.nullsLast(Long::compareTo));

        sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
        table.getSortOrder().setAll(colCat, colType, colPrice);
    }

    private void loadData() {
        setLoading(true, "Memuat price list…");
        api.fetchPriceList("active").whenComplete((list, err) -> {
            Platform.runLater(() -> {
                if (err != null) {
                    setLoading(false, "Gagal memuat: " + err.getMessage());
                    err.printStackTrace();
                    master.clear();
                    return;
                }

                master.setAll(list);

                // isi ComboBox kategori dari product_category
                Set<String> cats = master.stream()
                        .map(PriceItem::getCategory)
                        .filter(Objects::nonNull)
                        .filter(s -> !s.isBlank())
                        .collect(Collectors.toCollection(() -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));

                var items = FXCollections.<String>observableArrayList(cats);
                items.add(0, ""); // baris pertama = semua kategori
                cbCategory.setItems(items);
                if (cbCategory.getValue() == null) cbCategory.getSelectionModel().selectFirst();

                applyFilter();
                setLoading(false, "Berhasil memuat: " + master.size() + " item");
            });
        });
    }

    private void applyFilter() {
        String q = Optional.ofNullable(tfSearch.getText()).orElse("").trim().toLowerCase(Locale.ROOT);
        String cat = cbCategory.getValue();

        filtered.setPredicate(rowMatches(q, cat));
        statusLabel.setText("Menampilkan: " + filtered.size() + " item");
    }

    private Predicate<PriceItem> rowMatches(String q, String cat) {
        return item -> {
            if (item == null) return false;

            boolean matchSearch = q.isEmpty()
                    || (item.getCode() != null && item.getCode().toLowerCase().contains(q))
                    || (item.getNominal() != null && item.getNominal().toLowerCase().contains(q))
                    || (item.getDescription() != null && item.getDescription().toLowerCase().contains(q));

            boolean matchCat = (cat == null || cat.isBlank())
                    || (item.getCategory() != null && item.getCategory().equalsIgnoreCase(cat));

            return matchSearch && matchCat;
        };
    }

    private void setLoading(boolean v, String status) {
        loading.setVisible(v);
        statusLabel.setText(status);
    }

    // ==== Dialog transaksi nomor HP + harga jual (double-click) ====
    private void showPhonePriceDialog(PriceItem item) {
        String label = Optional.ofNullable(item.getNominal())
                .filter(s -> !s.isBlank())
                .orElse(Optional.ofNullable(item.getDescription()).orElse(item.getCode()));

        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Transaksi " + label);
        dialog.setHeaderText("Masukkan nomor HP dan harga jual");

        // Modal terhadap PriceListDialog saja (tidak memblok window lain)
        if (getDialogPane().getScene() != null) {
            Window parent = getDialogPane().getScene().getWindow();
            if (parent != null) {
                dialog.initOwner(parent);
                dialog.initModality(Modality.WINDOW_MODAL);
            }
        }

        ButtonType okType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

        TextField tfNumber = new TextField();
        tfNumber.setPromptText("Nomor Customer");

        TextField tfBuy = new TextField();
        tfBuy.setPromptText("Harga beli");

        Long buyPrice = item.getPrice();
        if (buyPrice != null && buyPrice > 0) tfBuy.setText(String.valueOf(buyPrice));

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(10));
        grid.addRow(0, new Label("Produk"), new Label(label));
        grid.addRow(1, new Label("Customer Number"), tfNumber);
        grid.addRow(3, new Label("Harga beli"), tfBuy);
        dialog.getDialogPane().setContent(grid);

        Node okBtn = dialog.getDialogPane().lookupButton(okType);
        okBtn.setDisable(true);
        tfBuy.setEditable(false);

        tfBuy.textProperty().addListener((o, old, v) -> {
            if (!v.matches("\\d*")) {
                tfBuy.setText(v.replaceAll("[^\\d]", ""));
            }
        });


        if ("pulsa".equalsIgnoreCase(item.getType())) {
            tfBuy.textProperty().addListener((o, old, v) -> {
                if (!v.matches("\\d*")) tfBuy.setText(v.replaceAll("[^\\d]", ""));
                okBtn.setDisable(!isDestinationValid(tfNumber.getText().trim())
                        || tfBuy.getText().trim().isBlank());
            });

            tfNumber.textProperty().addListener((o, old, v) -> {
                okBtn.setDisable(!isDestinationValid(v.trim())
                        || tfBuy.getText().trim().isBlank());
            });


        } else {

            tfBuy.textProperty().addListener((o, old, v) -> {
                if (!v.matches("\\d*")) tfBuy.setText(v.replaceAll("[^\\d]", ""));
                okBtn.setDisable(tfNumber.getText().trim().isEmpty()
                        || tfBuy.getText().trim().isEmpty());
            });

            tfNumber.textProperty().addListener((o, old, v) -> {
                okBtn.setDisable(v.trim().isEmpty()
                        || tfBuy.getText().trim().isEmpty());
            });


            okBtn.setDisable(tfNumber.getText().trim().isEmpty()
                    || tfBuy.getText().trim().isEmpty());
        }



        dialog.setResultConverter(btn -> {
            if (btn == okType) {
                return new String[]{tfNumber.getText().trim(),
                        tfBuy.getText().trim()
                };
            } else {
                return null;
            }
        });


        dialog.showAndWait().ifPresent(res -> {
            String customerId = res[0];
            String priceBuy = res[1];
            PlnInquiryResponse inquiryPlnResponse;
            if(item.getType().equalsIgnoreCase(ProductCategoryOnline.PLN.toString())) {
                inquiryPlnResponse = api.inquiryPln(customerId);
                if (inquiryPlnResponse.isSuccess()) {
                    onPlnInquirySuccess(item,inquiryPlnResponse, priceBuy);
                } else {
                    MessageDialogUtil.showError(inquiryPlnResponse.data.message);
                }
            } else {
                doTopupWithHandling(item, customerId, priceBuy);
            }

        });
    }

    private boolean isDestinationValid(String s) {
        if (s == null) return false;
        String x = s.trim();
        if (x.isEmpty()) return false;
        String normalized = x.startsWith("+62") ? "0" + x.substring(3) : x;
        return normalized.matches("0\\d{7,14}");
    }

    // panggil ini ketika inquiry PLN SUKSES
    private void onPlnInquirySuccess(PriceItem item, PlnInquiryResponse inquiry, String priceBuy) {
        var d = inquiry.data;

        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Konfirmasi PLN");
        dlg.setHeaderText("Pastikan data pelanggan sudah benar");

        if (getDialogPane().getScene() != null) {
            var owner = getDialogPane().getScene().getWindow();
            if (owner != null) dlg.initOwner(owner);
            dlg.initModality(javafx.stage.Modality.WINDOW_MODAL);
        }

        ButtonType okType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(8);
        g.setPadding(new Insets(12));
        int r = 0;
        g.addRow(r++, new Label("Customer ID"), new Label(ntd(d.customerId)));
        g.addRow(r++, new Label("Meter No"), new Label(ntd(d.meterNo)));
        g.addRow(r++, new Label("Subscriber"), new Label(ntd(d.subscriberId)));
        g.addRow(r++, new Label("Nama"), new Label(ntd(d.name)));
        g.addRow(r++, new Label("Daya/Segmen"), new Label(ntd(d.segmentPower)));
        dlg.getDialogPane().setContent(g);

        dlg.showAndWait().ifPresent(btn -> {
            if (btn == okType) {
                // gunakan ID dari inquiry biar konsisten
                String cid = (d.customerId != null && !d.customerId.isBlank())
                        ? d.customerId
                        : d.subscriberId;

                doTopupWithHandling(item, cid, priceBuy); // ⟵ lanjut transaksi (topup)
            }
            // else: batal
        });
    }

    public static BigDecimal subtotal(BigDecimal buyPrice, long totalFee) {
        if (totalFee < 0) {
            throw new IllegalArgumentException("totalFee tidak boleh negatif");
        }
        BigDecimal buy = Objects.requireNonNullElse(buyPrice, BigDecimal.ZERO);
        return buy.add(BigDecimal.valueOf(totalFee));
    }

    private static String safeNominal(String s) {
        if (s == null) return "0";
        String t = s.trim();
        return t.matches("\\d+") ? t : "0";
    }



    private void doTopupWithHandling(PriceItem item, String customerId, String priceBuy) {
        String refId = java.util.UUID.randomUUID() + "BABS";
        String nominal = "0";
        BigDecimal sellPrice;
        if(item.getType().equalsIgnoreCase(ProductCategoryOnline.PLN.toString()) ||  item.getType().equalsIgnoreCase(ProductCategoryOnline.PULSA.toString())) {
            nominal = safeNominal(item.getNominal());
        }
        if(!nominal.equals("0")){
            sellPrice = subtotal(new BigDecimal(nominal) , totalFee);
        } else {
            sellPrice = new BigDecimal(priceBuy).add(BigDecimal.valueOf(feeNonNominal));
        }

        var tx = new OnlineTransaction();
        tx.paymentMethod = "PREPAID";
        tx.userId        = (long) currentUser.getId();
        tx.category      = item.getType();
        tx.productCode   = item.getCode();
        tx.customerId    = customerId;
        tx.refId         = refId;
        tx.buyPrice      = new BigDecimal(priceBuy);
        tx.total         = sellPrice;
        tx.denom         = new BigDecimal(nominal.replaceAll("[^0-9-]", "")).longValueExact();
        tx.sellPrice = sellPrice;
        if(!nominal.equals("0")){
            tx.fee = new BigDecimal(totalFee);
        } else {
            tx.fee = new BigDecimal(feeNonNominal);
        }
        tx.margin = sellPrice.subtract(new BigDecimal(item.getPrice()));
        var det = new OnlineTransactionDetail();
        det.productCode  = item.getCode();
        det.description  = item.getDescription();
        det.qty          = 1;
        det.unitPrice    = tx.sellPrice;
        det.subtotal     = tx.total;

        long txId;
        try {
            txId = onlineDao.insert(tx, java.util.List.of(det));
        } catch (Exception ex) {
            MessageDialogUtil.showError("Gagal menyimpan transaksi (DB): " + ex.getMessage());
            return;
        }

        var result = api.topupRequest(customerId, item.getCode(), refId);
        if (result == null || result.data == null) {
            MessageDialogUtil.showError("Respons topup tidak valid.");
            try {
                onlineDao.updateAfterProviderResponse(
                        txId, null, null, "INVALID_RESPONSE", null, null,
                        null, OnlineTxnStatus.FAILED
                );
            } catch (Exception ignore) {}
            return;
        }

        var info = IakCodeUtil.classifyRc(result.data.rc);
        var newStatus = OnlineTxnStatus.fromIak(info.getStatus());

        try {
            Long trId = result.data.trId;
            Long trIdParam = null;
            if (trId != null && trId != 0L) {
                trIdParam = trId;
            }
            onlineDao.updateAfterProviderResponse(
                    txId,
                    result.data.rc,
                    result.data.status,
                    result.data.message,
                    trIdParam,
                    result.data.sn,
                    ApiClient.toJsonSafely(result),
                    newStatus
            );
        } catch (Exception ex) {
            MessageDialogUtil.showError(ex.getMessage());
        }
        switch (info.getStatus()) {
            case SUCCESS -> {
                var d = result.data;
                String ok = "Sukses: " + info.getDescription()
                        + ((d.trId != 0) ? "\nTRX: " + d.trId : "")
                        + ((d.sn != null && !d.sn.isBlank()) ? "\nSN : " + d.sn : "");
                MessageDialogUtil.showInfo(ok);
            }
            case FAILED -> {
                MessageDialogUtil.showError(info.getSolution());
            }
            case PENDING -> {
                if (IakCodeUtil.isStillProcessing(result.data.rc, result.data.message, result.data.status)) {
                    var owner = table.getScene().getWindow();
                    showProcessingAndWaitSimple(api, result.data.refId, owner);   // modeless progress
                } else {
                    MessageDialogUtil.showInfo(info.getSolution()); // mis. rc 201 undefined
                }
            }
        }
    }

    private static String ntd(String s) { // null-to-dash
        return (s == null || s.isBlank()) ? "-" : s;
    }

}
