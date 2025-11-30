package com.babsnet.posapp.controller;

import com.babsnet.posapp.model.Product;
import com.babsnet.posapp.model.Transaction;
import com.babsnet.posapp.model.User;
import com.babsnet.posapp.repository.ProductRepository;
import com.babsnet.posapp.repository.TransactionRepository;
import com.babsnet.posapp.session.CartSession;
import com.babsnet.posapp.session.SessionManager;
import com.babsnet.posapp.util.ConfigUtil;
import com.babsnet.posapp.util.FormatUtil;
import com.babsnet.posapp.util.MessageDialogUtil;
import com.babsnet.posapp.util.ThermalPrinterUtil;
import com.babsnet.posapp.util.ReceiptPrintUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.converter.IntegerStringConverter;
import java.util.List;

public class CashierController {

    @FXML private TextField barcodeField, paymentField;
    @FXML private TableView<Product> productTable;
    @FXML private TableColumn<Product, Integer> colId;
    @FXML private TableColumn<Product, String> colName;
    @FXML private TableColumn<Product, Integer> colQty;
    @FXML private TableColumn<Product, Double> colPrice;
    @FXML private TableColumn<Product, Double> colSubtotal;
    @FXML private Text totalText;
    @FXML private Label receiptPreview;
    @FXML private ComboBox<String> paymentMethodComboBox;

    private final ProductRepository productRepo = new ProductRepository();
    private final ObservableList<Product> cart = CartSession.getCart();
    User currentUser = SessionManager.getInstance().getCurrentUser();
    private final TransactionRepository transactionRepository = new TransactionRepository();

    @FXML
    private void initialize() {
        colId.setCellValueFactory(cellData -> new SimpleObjectProperty<>(null));
        colId.setCellFactory(col -> new TableCell<Product, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(String.valueOf(getIndex() + 1));
                }
            }
        });
        Platform.runLater(() -> {
            barcodeField.requestFocus();
            Scene scene = barcodeField.getScene();
            Stage stage = (Stage) scene.getWindow();
            addCashierShortcuts(scene, stage);
        });
        setupTableColumns();
        setupTableActions();
        setupNumberOnlyInput();
        productTable.setItems(cart);
        updateTotal();
        paymentMethodComboBox.setValue("Cash");
    }

    private void addCashierShortcuts(Scene scene, Stage stage) {
        scene.setOnKeyPressed(event -> {
            KeyCode code = event.getCode();

            if (code == KeyCode.F6) {
                paymentMethodComboBox.requestFocus();
                paymentMethodComboBox.show();
                event.consume();
                return;
            }

            if (event.isControlDown() && code == KeyCode.L) {
                handleClearTransaction();
                return;
            }

            if (event.isControlDown() && code == KeyCode.R) {
                handleRefreshProductTable(); // <-- Panggil fungsi refresh di sini!
                return;
            }

            if (event.isControlDown() && code == KeyCode.M) {
                stage.setIconified(true);
                return;
            }

            if (event.isControlDown() && code == KeyCode.B) {
                barcodeField.requestFocus();
                return;
            }

            if (event.isControlDown() && code == KeyCode.I) {
                showShortcutInfo();
                return;
            }


            // ENTER di barcodeField => tambah produk
            if (barcodeField.isFocused() && code == KeyCode.ENTER) {
                addProduct();
            }

            if (event.isControlDown() && code == KeyCode.S) {
                String keyword = barcodeField.getText().trim();
                if (!keyword.isEmpty()) {
                    showProductSearchPopup(keyword); // tampilkan popup search
                } else {
                    showProductSearchPopup(""); // tampilkan semua produk
                }
                event.consume();
                return;
            }

            // ENTER di paymentField => proses pembayaran
            else if (paymentField.isFocused() && code == KeyCode.ENTER) {
                processPayment();
            }
            // Shortcut utama F7-F12
            else {
                switch (code) {
                    case ALT -> paymentField.requestFocus(); // F7: Fokus ke payment
                    case F8 -> focusAndSelectProduct(0);    // F8: Pilih produk pertama di tabel
                    case F9 -> focusAndSelectProduct(productTable.getItems().size() - 1); // F9: Produk terakhir
                    case F10 -> {                           // F10: Edit qty di row terpilih
                        int selectedIdx = productTable.getSelectionModel().getSelectedIndex();
                        if (selectedIdx >= 0) {
                            productTable.edit(selectedIdx, colQty);
                        }
                    }
                    case F11 -> printReceiptToPrinter();    // F11: Print nota
                    case F12 -> handleCopy();// F12: Copy nota
                    default -> { /* ignore */ }
                }
            }
        });
    }


    private void handleRefreshProductTable() {
        productTable.refresh();
        updateTotal();
        Platform.runLater(() -> barcodeField.requestFocus());
    }


    private void focusAndSelectProduct(int index) {

        if (index >= 0 && index < productTable.getItems().size()) {
            productTable.requestFocus();
            productTable.getSelectionModel().select(index);
            productTable.getFocusModel().focus(index);
        }
    }


    @FXML
    private void handleClearTransaction() {
        if (!cart.isEmpty()) {
            if (!confirmDialog("Clear Transaction", "Yakin reset transaksi & hapus semua barang?")) {
                return;
            }
        }
        cart.clear();
        CartSession.clearCart();
        updateTotal();
        barcodeField.clear();
        paymentField.clear();
        receiptPreview.setText("");
        productTable.refresh();
        Platform.runLater(() -> barcodeField.requestFocus());
    }



    private void setupNumberOnlyInput() {
        paymentField.textProperty().addListener((obs, oldText, newText) -> {
            if (!newText.matches("\\d*")) {
                paymentField.setText(newText.replaceAll("[^\\d]", ""));
            }
        });
    }

    private void setupTableColumns() {
        colName.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getName()));
        colQty.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getStock()).asObject());
        colPrice.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getPrice()).asObject());
        colPrice.setCellFactory(tc -> new TableCell<Product, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(FormatUtil.toRupiahNoDecimal(price));
                }
            }
        });
        colSubtotal.setCellValueFactory(cellData -> {
            Product product = cellData.getValue();
            double subtotal = product.getStock() * product.getPrice();
            return new javafx.beans.property.SimpleDoubleProperty(subtotal).asObject();
        });

        colSubtotal.setCellFactory(tc -> new TableCell<Product, Double>() {
            @Override
            protected void updateItem(Double subtotal, boolean empty) {
                super.updateItem(subtotal, empty);
                if (empty || subtotal == null) {
                    setText(null);
                } else {
                    setText(FormatUtil.toRupiahNoDecimal(subtotal));
                }
            }
        });


        colQty.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        colQty.setOnEditCommit(event -> {
            handleQtyEdit(event.getRowValue(), event.getNewValue(), event.getTablePosition().getRow());
            productTable.refresh();
        });
    }

    private void setupTableActions() {
        productTable.setRowFactory(tv -> {
            TableRow<Product> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) removeProductFromCart(row.getItem());
            });
            return row;
        });

        productTable.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) showModifyProductDialog(productTable.getSelectionModel().getSelectedItem());
        });
    }

    private void handleQtyEdit(Product product, int newQty, int rowIndex) {
        if (newQty < 1) newQty = 1;

        if (productRepo.checkStockAvailability(product.getBarcode(), newQty)) {
            MessageDialogUtil.showWarning("Stock tidak mencukupi untuk " + product.getName());
            productTable.refresh();
            return;
        }

        product.setStock(newQty);
        updateTotal();
        productTable.getSelectionModel().select(rowIndex);
        productTable.scrollTo(rowIndex);
        barcodeField.requestFocus();
    }

    private void showModifyProductDialog(Product product) {
        if (product == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Modify Product");
        alert.setHeaderText("What do you want to do with " + product.getName() + "?");
        alert.getButtonTypes().setAll(new ButtonType("Update Quantity"), new ButtonType("Remove"), new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE));

        alert.showAndWait().ifPresent(result -> {
            if (result.getText().equals("Update Quantity")) {
                productTable.edit(productTable.getSelectionModel().getSelectedIndex(), colQty);
            } else if (result.getText().equals("Remove")) {
                removeProductFromCart(product);
            }
        });
    }

    @FXML
    private void addProduct() {
        String barcode = barcodeField.getText().trim();
        if (barcode.isEmpty()) return;

        int qtyToAdd = 1;
        Product existingProduct = cart.stream().filter(p -> barcode.equals(p.getBarcode())).findFirst().orElse(null);

        if (existingProduct != null) {
            int newQty = existingProduct.getStock() + qtyToAdd;
            if (productRepo.checkStockAvailability(barcode, newQty)) {
                showStockWarning(barcode);
                return;
            }
            existingProduct.setStock(existingProduct.getStock() + qtyToAdd);
        } else {
            Product product = productRepo.getProductByBarcode(barcode);
            if (product == null) {
                MessageDialogUtil.showWarning("Product not found!");
                return;
            }
            if (productRepo.checkStockAvailability(barcode, qtyToAdd)) {
                showStockWarning(barcode);
                return;
            }

            product.setStock(qtyToAdd);
            cart.add(product);
        }

        updateTotal();
        productTable.refresh();
        barcodeField.clear();
    }

    @FXML
    private void processPayment() {
        try {

            if(cart.isEmpty()) {
                MessageDialogUtil.showWarning("Product is empty!");
                return;
            }

            double total = cart.stream().mapToDouble(p -> p.getPrice() * p.getStock()).sum();
            double payment = Double.parseDouble(paymentField.getText());
            String paymentMethod = paymentMethodComboBox.getValue();

            if (payment < total) {
                MessageDialogUtil.showWarning("Payment not enough!");
                return;
            }



            int numberTransaction = productRepo.saveTransactionWithDetails(total, currentUser, cart, paymentMethod);

            if (numberTransaction > 0 ) {
                printReceipt(numberTransaction, payment);
                cart.clear();
                updateTotal();
                paymentField.clear();
                CartSession.clearCart();
//                MessageDialogUtil.showInfo("Payment success!");
            } else {
                MessageDialogUtil.showError("Payment failed. Transaction rolled back.");
            }

        } catch (NumberFormatException e) {
            MessageDialogUtil.showError("Invalid payment input.");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void removeProductFromCart(Product item) {
        if (confirmDialog("Remove product", "Remove " + item.getName() + " from cart?")) {
            cart.remove(item);
            productRepo.restoreStockQuantity(item.getBarcode(), item.getStock());
            updateTotal();
        }
    }

    private void updateTotal() {
        double total = cart.stream().mapToDouble(p -> p.getPrice() * p.getStock()).sum();
        totalText.setText(FormatUtil.toRupiahNoDecimal(total));
    }

    @FXML
    public void printReceiptToPrinter() {
        String receiptText = receiptPreview.getText();
        if(!receiptText.isEmpty()) {
            ThermalPrinterUtil.printToThermalPrinter(receiptText);
        } else {
            MessageDialogUtil.showInfo("Text is empty!");
        }

    }

    private boolean confirmDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait().filter(response -> response == ButtonType.OK).isPresent();
    }

    private void showStockWarning(String barcode) {
        MessageDialogUtil.showWarning("Insufficient stock for product barcode: " + barcode);
        barcodeField.clear();
    }



    public void printReceipt(int transactionId, double payment) {
        Transaction transaction = transactionRepository.getTransactionById(transactionId);
        StringBuilder receipt = new StringBuilder();

        receipt.append("\n");

        ReceiptPrintUtil.appendCenteredWrap(receipt, ConfigUtil.get("company.name"), 32);
        ReceiptPrintUtil.appendCenteredWrap(receipt, ConfigUtil.get("company.address"), 32);
        ReceiptPrintUtil.appendCenteredWrap(receipt, ConfigUtil.get("company.city"), 32);

        receipt.append(ReceiptPrintUtil.repeat("-", 32)).append("\n");

        String bonNumber = transaction.getTransactionNumber();
        String cashier = transaction.getUserName();
        receipt.append(String.format("Num. %-22s%-8s\n", bonNumber, cashier));
        receipt.append(ReceiptPrintUtil.repeat("-", 32)).append("\n");

        receipt.append(String.format("%-17s %3s %10s\n", "Item", "Qty", "Total"));
        receipt.append(ReceiptPrintUtil.repeat("-", 32)).append("\n");

        double totalBelanja = 0;
        int totalQty = 0;
        for (Product p : cart) {
            int qty = p.getStock();
            double subtotal = p.getPrice() * qty;
            totalBelanja += subtotal;
            totalQty += qty;

            ReceiptPrintUtil.appendProductLine58(receipt, p.getName(), qty, subtotal);
        }

        receipt.append(ReceiptPrintUtil.repeat("-", 32)).append("\n");

        double totalDisc = 0;
        double cash      = payment;
        double total     = totalBelanja - totalDisc; // total akhir (bukan "Subtotal")
        double kembalian = cash - total;

        ReceiptPrintUtil.appendSummaryQtyAndAmount(receipt, "Total Item", totalQty, totalBelanja);
        ReceiptPrintUtil.appendSummaryAmount(receipt, "Discount", totalDisc);
        ReceiptPrintUtil.appendSummaryAmount(receipt, "Tunai", cash);
        ReceiptPrintUtil.appendSummaryAmount(receipt, "Kembalian", kembalian);
        receipt.append(ReceiptPrintUtil.repeat("-", 32)).append("\n");
        receipt.append(ReceiptPrintUtil.centerText("Terima kasih atas kunjungannya!", 32)).append("\n");
        receiptPreview.setText(receipt.toString());
    }



    @FXML
    private void handleCopy() {
        String receiptText = receiptPreview.getText();
        if (receiptText == null || receiptText.isEmpty()) {
            MessageDialogUtil.showInfo("Tidak ada struk untuk dicopy.");
            return;
        }
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(receiptText);
        clipboard.setContent(content);

        MessageDialogUtil.showInfo("Struk berhasil dicopy ke clipboard!");
    }

    @FXML
    private void showShortcutInfo() {
        String info = """
        Daftar Shortcut Keyboard Kasir:
        [Ctrl+S]   : Search Product
        [Ctrl+L]   : Clear transaksi/cart
        [Enter]    : Tambah produk (di barcodeField) / proses payment (di paymentField)
        [ALT]      : Fokus ke kolom pembayaran
        [F8]       : Pilih produk pertama di tabel (FN + F8)
        [F9]       : Pilih produk terakhir di tabel (FN + F9)
        [F10]      : Edit Qty produk terpilih 
        [F6]       : Pilih metode pembayaran (FN + F6)
        [F11]      : Print struk (FN + F11)
        [F12]      : Copy struk ke clipboard 
        """;
        MessageDialogUtil.showInfo(info);
    }

    private void showProductSearchPopup(String keyword) {
        List<Product> result = productRepo.searchProducts(keyword);

        if (result.isEmpty()) {
            MessageDialogUtil.showWarning("Produk tidak ditemukan!");
            return;
        }

        Stage popupStage = new Stage();
        popupStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        // ====== Table ======
        TableView<Product> tableView = new TableView<>();

        TableColumn<Product, String> colBarcode = new TableColumn<>("Barcode");
        colBarcode.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getBarcode()));
        colBarcode.setPrefWidth(140);

        TableColumn<Product, String> colName = new TableColumn<>("Name");
        colName.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getName()));
        colName.setPrefWidth(260);

        TableColumn<Product, Double> colPrice = new TableColumn<>("Price");
        colPrice.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getPrice()));
        colPrice.setCellFactory(tc -> new TableCell<Product, Double>() {
            @Override protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                setText(empty || price == null ? null : FormatUtil.toRupiahNoDecimal(price));
            }
        });
        colPrice.setPrefWidth(120);

        TableColumn<Product, Integer> colStock = new TableColumn<>("Stock");
        colStock.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getStock()));
        colStock.setPrefWidth(90);

        tableView.getColumns().addAll(colBarcode, colName, colPrice, colStock);
        tableView.setPrefWidth(640);
        tableView.setPrefHeight(360);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        // ====== Data + Filter ======
        var data = javafx.collections.FXCollections.observableArrayList(result);
        var filtered = new javafx.collections.transformation.FilteredList<>(data, p -> true);
        var sorted   = new javafx.collections.transformation.SortedList<>(filtered);
        sorted.comparatorProperty().bind(tableView.comparatorProperty());
        tableView.setItems(sorted);

        // ====== Search field ======
        TextField searchField = new TextField();
        searchField.setPromptText("Cari barcode / nama… (Ctrl+F)");
        if (keyword != null && !keyword.isBlank()) searchField.setText(keyword);

        // filter realtime
        searchField.textProperty().addListener((obs, old, q) -> {
            final String query = q == null ? "" : q.trim().toLowerCase();
            filtered.setPredicate(p -> {
                if (query.isEmpty()) return true;
                String b = p.getBarcode() == null ? "" : p.getBarcode().toLowerCase();
                String n = p.getName()    == null ? "" : p.getName().toLowerCase();
                return b.contains(query) || n.contains(query);
            });
            // auto pilih baris pertama setelah filter
            if (!tableView.getItems().isEmpty()) {
                tableView.getSelectionModel().select(0);
            }
        });

        // ====== Keyboard UX ======
        // Enter pilih item (baik dari table maupun dari search)
        Runnable confirmPick = () -> {
            Product selected = tableView.getSelectionModel().getSelectedItem();
            if (selected == null && tableView.getItems().size() == 1) {
                selected = tableView.getItems().get(0);
            }
            if (selected != null) {
                addProductToCartPopup(selected);
                popupStage.close();
            }
        };

        tableView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) confirmPick.run();
        });

        tableView.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                confirmPick.run();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                popupStage.close();
            }
        });

        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                confirmPick.run();
            } else if (e.getCode() == KeyCode.DOWN) {
                tableView.requestFocus();
                tableView.getSelectionModel().select(0);
            } else if (e.getCode() == KeyCode.ESCAPE) {
                popupStage.close();
            }
        });

        // Ctrl+F fokus ke search
        Scene scene = new Scene(new VBox(8, searchField, tableView));
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN),
                searchField::requestFocus
        );

        VBox root = (VBox) scene.getRoot();
        root.setPadding(new Insets(10));

        popupStage.setScene(scene);
        popupStage.setTitle("Cari Produk");
        popupStage.show();

        // fokus awal ke search
        Platform.runLater(searchField::requestFocus);
    }



    private void addProductToCartPopup(Product product) {
        int qtyToAdd = 1;
        Product existingProduct = cart.stream().filter(p -> product.getBarcode().equals(p.getBarcode())).findFirst().orElse(null);

        if (existingProduct != null) {
            int newQty = existingProduct.getStock() + qtyToAdd;
            if (productRepo.checkStockAvailability(product.getBarcode(), newQty)) {
                showStockWarning(product.getBarcode());
                return;
            }
            existingProduct.setStock(existingProduct.getStock() + qtyToAdd);
        } else {
            if (productRepo.checkStockAvailability(product.getBarcode(), qtyToAdd)) {
                showStockWarning(product.getBarcode());
                return;
            }
            product.setStock(qtyToAdd);
            cart.add(product);
        }

        updateTotal();
        productTable.refresh();
        barcodeField.clear();
    }

}
