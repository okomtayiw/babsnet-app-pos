package com.babsnet.posapp.util;

import com.babsnet.posapp.model.*;
import com.babsnet.posapp.repository.OnlineTransactionDao;
import com.babsnet.posapp.session.SessionManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.babsnet.posapp.util.PriceListDialogPrepaid.subtotal;
import static com.babsnet.posapp.util.Utils.buildLoadingDialog;

public class PriceListDialogPostpaid extends Dialog<Void> {

    // UI
    private final TextField tfSearch = new TextField();
    private final ComboBox<String> cbCategory = new ComboBox<>();
    private final ComboBox<String> cbType = new ComboBox<>();
    private final Button btnRefresh = new Button("Refresh");
    private final ProgressIndicator loading = new ProgressIndicator();

    private final TableView<PostpaidProduct> table = new TableView<>();
    private final TableColumn<PostpaidProduct, String>  colCode   = new TableColumn<>("Code");
    private final TableColumn<PostpaidProduct, String>  colName   = new TableColumn<>("Name");
    private final TableColumn<PostpaidProduct, String>  colType   = new TableColumn<>("Type");
    private final TableColumn<PostpaidProduct, String>  colCat    = new TableColumn<>("Category");
    private final TableColumn<PostpaidProduct, Integer> colStatus = new TableColumn<>("Status");
    private final TableColumn<PostpaidProduct, Long>    colFee    = new TableColumn<>("Fee");
    private final TableColumn<PostpaidProduct, Long>    colKomisi = new TableColumn<>("Komisi");

    private final Label statusLabel = new Label("Memuat…");

    // Data
    private final ObservableList<PostpaidProduct> master = FXCollections.observableArrayList();
    private final FilteredList<PostpaidProduct> filtered = new FilteredList<>(master);
    private SortedList<PostpaidProduct> sorted;

    private final NumberFormat rupiah = NumberFormat.getInstance(new Locale("id", "ID"));
    private final ApiClient api = IakCodeUtil.buildApiClient();
    private Consumer<PostpaidProduct> onSelect = p -> {};
    private final Label lblBalance = new Label("Rp 0");
    User currentUser = SessionManager.getInstance().getCurrentUser();
    private static final NumberFormat NF_ID = NumberFormat.getInstance(new Locale("id","ID"));
    private final OnlineTransactionDao onlineDao = new OnlineTransactionDao();
    public PriceListDialogPostpaid(Window owner) {

        setTitle("Daftar Produk (Postpaid/Emoney/Internet)");
        setHeaderText("Cari & pilih produk. Double-click baris untuk melanjutkan.");

        if (owner != null) initOwner(owner);
        initModality(Modality.NONE); // modeless

        getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
        getDialogPane().setPrefSize(950, 600);

        setOnShown(e -> Platform.runLater(tfSearch::requestFocus));

        buildUI();
        wireEvents();
        initTable();
        loadBalance();
        loadData();
    }

    private void loadBalance() {
        Task<Long> task = new Task<>() {
            @Override protected Long call() throws Exception {
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
            ex.printStackTrace();
            MessageDialogUtil.showError("Fetch balance gagal: " + ex.getMessage());
        });

        new Thread(task, "balance-task").start(); // non-blocking UI
    }


    public void setOnSelect(Consumer<PostpaidProduct> onSelect) {
        this.onSelect = (onSelect != null) ? onSelect : p -> {};
    }

    private void buildUI() {
        tfSearch.setPromptText("Cari code/name…");
        cbCategory.setPromptText("Semua category");
        cbType.setPromptText("Semua type");

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
                new Label("Category"), cbCategory,
                new Label("Type"), cbType,
                btnRefresh, loading, spacer, balanceBox
        );
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(10));
        HBox.setHgrow(tfSearch, Priority.ALWAYS);

        table.setPlaceholder(new Label("Tidak ada data"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.getColumns().setAll(colCode, colName, colType, colCat, colStatus, colFee, colKomisi);

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
        cbType.valueProperty().addListener((o, old, val) -> applyFilter());
        btnRefresh.setOnAction(e -> loadData());
        btnRefresh.disableProperty().bind(loading.visibleProperty());

        table.setRowFactory(tv -> {
            TableRow<PostpaidProduct> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (!row.isEmpty() && e.getClickCount() == 2) {
                    showDialogPostPaid(row.getItem());
                }
            });
            return row;
        });


    }

    private void showDialogPostPaid(PostpaidProduct item) {
        String label = Optional.ofNullable(item.getName())
                .filter(s -> !s.isBlank())
                .orElse(Optional.ofNullable(item.getCode()).orElse("Produk"));

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Input Customer - " + label);
        dialog.setHeaderText("Masukkan nomor/ID pelanggan");

        // Modal ke parent dialog jika ada (tetap ringan & simple)
        if (getDialogPane().getScene() != null) {
            Window parent = getDialogPane().getScene().getWindow();
            if (parent != null) {
                dialog.initOwner(parent);
                dialog.initModality(Modality.WINDOW_MODAL);
            }
        }

        ButtonType okType =
                new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

        TextField tfCustomer = new TextField();
        tfCustomer.setPromptText("Customer Number");
        TextField tfMonth = new TextField();
        TextField tfYear = new TextField();
        TextField tfIdentitas = new TextField();
        TextField tfNominal = new TextField();
        TextField tfBillCode = new TextField();
        TextField tfBillKey = new TextField();
        if(item.getType().equalsIgnoreCase("bpjs")){
            tfMonth.setPromptText("Month");
            tfMonth.setVisible(true);
        }

        if(item.getType().equalsIgnoreCase("pbb")){
            tfYear.setPromptText("Year");
            tfYear.setVisible(true);
        }

        if(item.getType().equalsIgnoreCase("pajak-kendaraan")){
            tfIdentitas.setPromptText("Identitas");
            tfIdentitas.setVisible(true);
        }

        if(item.getType().equalsIgnoreCase("emoney")
                || item.getType().equalsIgnoreCase("dm-member")
                || item.getType().equalsIgnoreCase("dm-nonmember")){
            tfNominal.setPromptText("Nominal");
            tfNominal.setVisible(true);
        }

        if(item.getType().equalsIgnoreCase("pendidikan")){
            tfBillCode.setPromptText("Bill Code");
            tfBillCode.setVisible(true);
            tfBillKey.setPromptText("Bill Key");
            tfBillKey.setVisible(true);
        }


        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(10));
        grid.addRow(0, new Label("Produk"), new Label(label));
        grid.addRow(1, new Label("Customer Number"), tfCustomer);
        if(item.getType().equalsIgnoreCase("bpjs")) {
            grid.addRow(2, new Label("Month"), tfMonth);
        }
        if(item.getType().equalsIgnoreCase("pbb")) {
            grid.addRow(2, new Label("Year"), tfMonth);
        }
        if(item.getType().equalsIgnoreCase("pajak-kendaraan")){
            grid.addRow(2, new Label("Identitas"), tfIdentitas);
        }
        if(item.getType().equalsIgnoreCase("emoney")
                || item.getType().equalsIgnoreCase("dm-member")
                || item.getType().equalsIgnoreCase("dm-nonmember")){
            grid.addRow(2, new Label("Nominal"), tfNominal);
        }
        if(item.getType().equalsIgnoreCase("pendidikan")){
            grid.addRow(2, new Label("Bill Code"), tfBillCode);
            grid.addRow(3, new Label("Bill Key"), tfBillKey);
        }
        dialog.getDialogPane().setContent(grid);

        Node okBtn = dialog.getDialogPane().lookupButton(okType);
        okBtn.setDisable(true);

        tfCustomer.textProperty().addListener((o, old, v) -> {
            okBtn.setDisable(v == null || v.trim().isEmpty());
        });

        if(item.getType().equalsIgnoreCase("bpjs")) {
            tfMonth.textProperty().addListener((o, old, v) -> {
                okBtn.setDisable(v == null || v.trim().isEmpty());
            });
        }

        if(item.getType().equalsIgnoreCase("pbb")) {
            tfYear.textProperty().addListener((o, old, v) -> {
                okBtn.setDisable(v == null || v.trim().isEmpty());
            });
        }

        if(item.getType().equalsIgnoreCase("pajak-kendaraan")){
            tfIdentitas.textProperty().addListener((o, old, v) -> {
                okBtn.setDisable(v == null || v.trim().isEmpty());
            });
        }

        if(item.getType().equalsIgnoreCase("emoney")
                || item.getType().equalsIgnoreCase("dm-member")
                || item.getType().equalsIgnoreCase("dm-nonmember")){
            tfNominal.textProperty().addListener((o, old, v) -> {
                okBtn.setDisable(v == null || v.trim().isEmpty());
            });
        }
        if(item.getType().equalsIgnoreCase("pendidikan")){
            tfBillCode.textProperty().addListener((o, old, v) -> {
                okBtn.setDisable(v == null || v.trim().isEmpty());
            });

            tfBillKey.textProperty().addListener((o, old, v) -> {
                okBtn.setDisable(v == null || v.trim().isEmpty());
            });
        }


        dialog.setOnShown(e -> Platform.runLater(tfCustomer::requestFocus));

        dialog.setResultConverter(btn -> btn == okType ? tfCustomer.getText().trim() : null);

        dialog.showAndWait().ifPresent(customerId -> {
            String month = null;
            String year = null;
            String identitas = null;
            String nominal = null;
            String billCode = null;
            String billKey = null;
            if (item.getType().equalsIgnoreCase("bpjs")){
               month = tfMonth.getText().trim();
            } else if (item.getType().equalsIgnoreCase("pbb")){
                year = tfYear.getText().trim();
            } else if (item.getType().equalsIgnoreCase("pajak-kendaraan")){
                identitas = tfIdentitas.getText().trim();
            } else if (item.getType().equalsIgnoreCase("emoney")
                    || item.getType().equalsIgnoreCase("dm-member")
                    || item.getType().equalsIgnoreCase("dm-nonmember")){
                nominal = tfNominal.getText().trim();
            } else if (item.getType().equalsIgnoreCase("pendidikan")){
                billCode = tfBillCode.getText().trim();
                billKey = tfBillKey.getText().trim();
            }
            String refId = UUID.randomUUID() + "babs";
            doInquiryOnFx(
                    customerId,
                    item,
                    month,
                    refId,
                    year,
                    identitas,
                    nominal, billCode, billKey);
        });
    }

    private void doInquiryOnFx(String customerId, PostpaidProduct item, String month, String refId, String year, String identitas, String nominal, String billCode, String billKey) {
        Task<InquiryResponsePostPaid> task = new Task<>() {
            @Override protected InquiryResponsePostPaid call() {
                return  api.sendInquiryPascaPostPaid(
                        customerId,
                        item.getCode(),
                        refId, month,
                        item.getType(),
                        year,
                        identitas,
                        nominal, billCode, billKey);
            }
        };
        task.setOnSucceeded(e -> {
            InquiryResponsePostPaid r = task.getValue();
            showInquiryPopupAndMaybePay(r, item);
        });
        task.setOnFailed(e -> {
            task.getException().printStackTrace();
            MessageDialogUtil.showError("Inquiry gagal: " + task.getException().getMessage());
        });
        new Thread(task, "inq-pasca-task").start();
    }


    private void showInquiryPopupAndMaybePay(InquiryResponsePostPaid r, PostpaidProduct item) {
        var idr = NumberFormat.getInstance(new Locale("id", "ID"));
        var data = r.data;

        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Inquiry Result - " + (item.getName() != null ? item.getName() : item.getCode()));
        ButtonType payType = new ButtonType("Bayar", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(payType, ButtonType.CLOSE);


        // header ringkas
        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(6); g.setPadding(new Insets(12));
        int rIdx = 0;
        g.addRow(rIdx++, new Label("Produk"), new Label((item.getName()!=null?item.getName():item.getCode()) + " (" + ntd(data.code) + ")"));
        g.addRow(rIdx++, new Label("TRX ID"), new Label(String.valueOf(data.tr_id)));
        g.addRow(rIdx++, new Label("Customer"), new Label(ntd(data.hp)));
        g.addRow(rIdx++, new Label("Nama"), new Label(ntd(data.tr_name)));
        g.addRow(rIdx++, new Label("Periode"), new Label(ntd(data.period)));
        if (data.nominal != null) g.addRow(rIdx++, new Label("Nominal"), new Label(idr.format(data.nominal)));
        if (data.admin   != null) g.addRow(rIdx++, new Label("Admin"),   new Label(idr.format(data.admin)));
        if (data.price   != null) g.addRow(rIdx++, new Label("Total"),   new Label(idr.format(data.price)));
        if (data.selling_price != null) g.addRow(rIdx++, new Label("Selling Price"), new Label(idr.format(data.selling_price)));
        if (data.balance != null) g.addRow(rIdx++, new Label("Balance"), new Label(idr.format(data.balance)));
        g.addRow(rIdx++, new Label("Ref ID"), new Label(ntd(data.ref_id)));
        g.addRow(rIdx++, new Label("Message"), bold(ntd(data.message)));

        String descText = humanizeDesc(data.desc);
        TextArea ta = new TextArea(descText);
        ta.setEditable(false);
        ta.setWrapText(true);
        ta.setPrefRowCount(12);
        TitledPane descPane = new TitledPane("Rincian", ta);
        descPane.setExpanded(true);

        VBox root = new VBox(8, g, descPane);
        root.setPadding(new Insets(8));
        dlg.getDialogPane().setContent(root);
        dlg.getDialogPane().setPrefWidth(700);

         if(data.tr_id == null) {
             Node payBtn = dlg.getDialogPane().lookupButton(payType);
             payBtn.setDisable(true);
         }

        dlg.showAndWait().ifPresent(btn -> {
            if (btn == payType) {
                // lanjut proses bayar
                var tx = new OnlineTransaction();
                long totalFee = Long.parseLong(ConfigUtil.get("fee.payment.online.postpaid"));
                long feeNonNominal = Long.parseLong(ConfigUtil.get("fee.payment.online.prepaid.non.nominal"));
                var sellPrice = subtotal(r.data.nominal , totalFee);
                tx.paymentMethod = "POSTPAID";
                tx.userId        = (long) currentUser.getId();
                tx.category      = item.getType();
                tx.productCode   = item.getCode();
                tx.customerId    = r.data.tr_id.toString();
                tx.refId         = r.data.ref_id;
                tx.buyPrice      = r.data.price;
                tx.denom         = r.data.nominal.longValueExact();
                if(sellPrice.equals(r.data.price)){
                    sellPrice = r.data.price.add(BigDecimal.valueOf(feeNonNominal));
                    tx.fee = new BigDecimal(feeNonNominal);
                    tx.sellPrice = sellPrice;
                    tx.total = sellPrice;
                } else if (sellPrice.compareTo(r.data.price) > 0 ){
                    tx.sellPrice      = sellPrice;
                    tx.fee = new BigDecimal(totalFee);
                    tx.total = sellPrice;
                } else {
                    sellPrice = subtotal(r.data.price, totalFee);
                    tx.sellPrice = sellPrice;
                    tx.fee = new BigDecimal(totalFee);
                    tx.total = sellPrice;
                }

                tx.margin = sellPrice.subtract(r.data.price);
                var det = new OnlineTransactionDetail();
                det.productCode  = item.getCode();
                det.description  = r.data.desc.toString();
                det.qty          = 1;
                det.unitPrice    = tx.sellPrice;
                det.subtotal     = tx.total;

                long txId = 0L;
                try {
                    txId = onlineDao.insert(tx, List.of(det));
                } catch (Exception ex) {
                    MessageDialogUtil.showError("Gagal menyimpan transaksi (DB): " + ex.getMessage());
                }
                doPayPostPaid(item, r, txId);
            }
        });
    }

    private void doPayPostPaid(PostpaidProduct item, InquiryResponsePostPaid inq, long txId) {
        Task<InquiryResponsePostPaid> task = new Task<>() {
            @Override protected InquiryResponsePostPaid call() {
                updateMessage("Mengirim permintaan pembayaran…");
                InquiryResponsePostPaid res = api.payPascaSync(item, inq);
                updateMessage("Menerima respons…");
                return res;
            }
        };


        Window owner = Utils.ownerOf(table);
        Dialog<Void> loading = buildLoadingDialog(
                "Memproses Pembayaran", "Mohon tunggu…", task, owner);
        loading.show();

        task.setOnSucceeded(e -> {
            InquiryResponsePostPaid result = task.getValue();
            if (result == null || result.data == null) {
                MessageDialogUtil.showError("Response tidak valid.");
                try {
                    onlineDao.updateAfterProviderResponse(
                            txId, null, null, "INVALID_RESPONSE", null, null,
                            null, OnlineTxnStatus.FAILED
                    );
                } catch (Exception ignore) {}
                return;
            }
            var info = IakCodeUtil.classifyRc(result.data.response_code);
            var newStatus = OnlineTxnStatus.fromIak(info.getStatus());
            try {
                onlineDao.updateAfterProviderResponse(
                        txId,
                        result.data.response_code,
                        0,
                        result.data.message,
                        result.data.tr_id,
                        result.data.noref,
                        ApiClient.toJsonSafely(result),
                        newStatus
                );
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
            MessageDialogUtil.showInfo(
                    "Pembayaran selesai.\nStatus: " + result.data.response_code + "\nMsg: " + result.data.message
            );

        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            ex.printStackTrace();
            MessageDialogUtil.showError("Bayar gagal: " + (ex != null ? ex.getMessage() : "unknown error"));
        });

        task.setOnCancelled(e -> {
             MessageDialogUtil.showInfo("Pembayaran dibatalkan.");
        });

        Thread th = new Thread(task, "pay-pasca-task");
        th.setDaemon(true);
        th.start();
    }


    public static String humanizeDesc(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) return "-";
        StringBuilder sb = new StringBuilder();
        flattenJson(node, sb, "");
        return sb.toString().trim();
    }

    private static void flattenJson(JsonNode node,
                                    StringBuilder sb, String indent) {
        if (node.isObject()) {
            node.fields().forEachRemaining(e -> {
                String key = titleCase(e.getKey());
                JsonNode v = e.getValue();
                if (v.isValueNode()) {
                    sb.append(indent).append(key).append(": ").append(fmtValue(v)).append("\n");
                } else if (v.isArray()) {
                    sb.append(indent).append(key).append(":\n");
                    int i = 1;
                    for (var it : v) {
                        if (it.isValueNode()) {
                            sb.append(indent).append("  - ").append(fmtValue(it)).append("\n");
                        } else {
                            sb.append(indent).append("  #").append(i++).append("\n");
                            flattenJson(it, sb, indent + "    ");
                        }
                    }
                } else { // nested object
                    sb.append(indent).append(key).append(":\n");
                    flattenJson(v, sb, indent + "  ");
                }
            });
        } else if (node.isArray()) {
            int i = 1;
            for (var it : node) {
                if (it.isValueNode()) {
                    sb.append(indent).append("- ").append(fmtValue(it)).append("\n");
                } else {
                    sb.append(indent).append("#").append(i++).append("\n");
                    flattenJson(it, sb, indent + "  ");
                }
            }
        } else { // single value
            sb.append(indent).append(fmtValue(node)).append("\n");
        }
    }

    private static String fmtValue(JsonNode v) {
        if (v.isNumber()) {
            try {
                return NF_ID.format(v.numberValue()); // 123456 → 123.456
            } catch (Exception ignore) { /* fallthrough */ }
        }
        if (v.isBoolean()) return v.asBoolean() ? "Yes" : "No";
        String s = v.asText("");
        return s.isBlank() ? "-" : s;
    }

    private static String titleCase(String key) {
        String s = key.replace('_', ' ').replace('-', ' ');
        String[] parts = s.trim().split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            out.append(Character.toUpperCase(p.charAt(0)))
                    .append(p.length() > 1 ? p.substring(1) : "")
                    .append(' ');
        }
        return out.toString().trim();
    }


    private static Label bold(String s) { Label l = new Label(s); l.setStyle("-fx-font-weight: bold;"); return l; }
    private static String ntd(String s) { return (s == null || s.isBlank()) ? "-" : s; }
    private static String prettyJson(JsonNode node) {
        try {
            if (node == null || node.isNull()) return "-";
            return new ObjectMapper()
                    .writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (Exception e) { return node.toString(); }
    }




    private void initTable() {
        colCode.setCellValueFactory(new PropertyValueFactory<>("code"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colCat.setCellValueFactory(new PropertyValueFactory<>("category"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colFee.setCellValueFactory(new PropertyValueFactory<>("fee"));
        colKomisi.setCellValueFactory(new PropertyValueFactory<>("komisi"));

        colFee.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Long v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : rupiah.format(v));
            }
        });
        colKomisi.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Long v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : rupiah.format(v));
            }
        });
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); return; }
                setText(v == 1 ? "Active" : "Off");
                setStyle(v == 1 ? "-fx-text-fill: -fx-text-base-color;" : "-fx-text-fill: #d9534f;");
            }
        });

        colCat.setSortType(TableColumn.SortType.ASCENDING);
        colType.setSortType(TableColumn.SortType.ASCENDING);

        sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
        table.getSortOrder().setAll(colCat, colType, colName);
    }

    private void loadData() {
        setLoading(true, "Memuat daftar produk…");
        api.fetchPostpaidProducts("active").whenComplete((list, err) -> {
            Platform.runLater(() -> {
                if (err != null) {
                    setLoading(false, "Gagal memuat: " + err.getMessage());
                    master.clear();
                    return;
                }
                master.setAll(list);

                // isi filter Category & Type
                Set<String> cats = master.stream()
                        .map(PostpaidProduct::getCategory)
                        .filter(Objects::nonNull).filter(s -> !s.isBlank())
                        .collect(Collectors.toCollection(() -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));

                Set<String> types = master.stream()
                        .map(PostpaidProduct::getType)
                        .filter(Objects::nonNull).filter(s -> !s.isBlank())
                        .collect(Collectors.toCollection(() -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));

                var catItems = FXCollections.<String>observableArrayList(cats);
                var typeItems = FXCollections.<String>observableArrayList(types);
                catItems.add(0, "");  // kosong = semua
                typeItems.add(0, "");
                cbCategory.setItems(catItems);
                cbType.setItems(typeItems);
                if (cbCategory.getValue() == null) cbCategory.getSelectionModel().selectFirst();
                if (cbType.getValue() == null) cbType.getSelectionModel().selectFirst();

                applyFilter();
                setLoading(false, "Berhasil memuat: " + master.size() + " item");
            });
        });
    }

    private void applyFilter() {
        String q = Optional.ofNullable(tfSearch.getText()).orElse("").trim().toLowerCase(Locale.ROOT);
        String cat = cbCategory.getValue();
        String typ = cbType.getValue();

        filtered.setPredicate(rowMatches(q, cat, typ));
        statusLabel.setText("Menampilkan: " + filtered.size() + " item");
    }

    private Predicate<PostpaidProduct> rowMatches(String q, String cat, String typ) {
        return p -> {
            if (p == null) return false;

            boolean matchSearch = q.isEmpty()
                    || (p.getCode() != null && p.getCode().toLowerCase(Locale.ROOT).contains(q))
                    || (p.getName() != null && p.getName().toLowerCase(Locale.ROOT).contains(q));

            boolean matchCat = (cat == null || cat.isBlank())
                    || (p.getCategory() != null && p.getCategory().equalsIgnoreCase(cat));

            boolean matchType = (typ == null || typ.isBlank())
                    || (p.getType() != null && p.getType().equalsIgnoreCase(typ));

            return matchSearch && matchCat && matchType;
        };
    }

    private void setLoading(boolean v, String status) {
        loading.setVisible(v);
        statusLabel.setText(status);
    }
}

