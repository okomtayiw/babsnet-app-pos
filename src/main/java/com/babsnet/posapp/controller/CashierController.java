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
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.converter.IntegerStringConverter;
import java.util.ArrayList;
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

            double total = FormatUtil.rupiahToDouble(totalText.getText());
            double payment = FormatUtil.rupiahToDouble(paymentField.getText());
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

        // Header
        receipt.append(centerText(ConfigUtil.get("company.name"), 32)).append("\n");
        receipt.append(centerText(ConfigUtil.get("company.address"), 32)).append("\n");
        receipt.append(centerText("NPWP: " + ConfigUtil.get("company.npwp"), 32)).append("\n");
        receipt.append(repeat("-", 32)).append("\n");


        // Bon & Kasir
        String bonNumber = transaction.getTransactionNumber();
        String cashier = transaction.getUserName();
        receipt.append(String.format("Bon %-22s%-8s\n", bonNumber, cashier));
        receipt.append(repeat("-", 32)).append("\n");

        // Item Header
        receipt.append(String.format("%-18s %3s %10s\n", "Item", "Qty", "Total"));
        receipt.append(repeat("-", 32)).append("\n");

        // Item List
        double totalBelanja = 0;
        int totalQty = 0;
        for (Product p : cart) {
            double subtotal = p.getPrice() * p.getStock();
            totalBelanja += subtotal;
            totalQty += p.getStock();
            appendProductLine(receipt, p.getName(), p.getStock(), subtotal);
        }

        receipt.append(repeat("-", 32)).append("\n");

        // Summary
        double totalDisc = 0;
        double cash = payment;
        double kembalian = cash - totalBelanja;

        receipt.append(formatSummary("Total Item", totalQty, totalBelanja));
        receipt.append(formatSummary("Diskon", 0, totalDisc));
        receipt.append(formatSummary("Total Bayar", 0, totalBelanja));
        receipt.append(formatSummary("Tunai", 0, cash));
        receipt.append(formatSummary("Kembalian", 0, kembalian));

        receipt.append(repeat("-", 32)).append("\n");

        // Footer
        receipt.append(centerText("Terima kasih atas kunjungannya!", 32)).append("\n");

        receiptPreview.setText(receipt.toString());
    }

    private void appendProductLine(StringBuilder receipt, String name, int qty, double subtotal) {
        int maxNameLength = 18;
        List<String> lines = splitTextByLength(simplifyName(name), maxNameLength);

        // Baris pertama: nama + qty + subtotal
        receipt.append(String.format("%-" + maxNameLength + "s %3d %10s\n",
                lines.get(0), qty, formatCurrency(subtotal)));

        // Baris berikutnya (jika nama panjang)
        for (int i = 1; i < lines.size(); i++) {
            receipt.append(String.format("%-" + maxNameLength + "s\n", lines.get(i)));
        }
    }

    private String simplifyName(String name) {
        String[] words = name.split("\\s+");
        if (words.length == 0) return "";

        StringBuilder simplified = new StringBuilder(words[0].toUpperCase());
        for (int i = 1; i < words.length; i++) {
            simplified.append(" ").append(words[i].substring(0, 1).toUpperCase());
        }
        return simplified.toString();
    }

    private List<String> splitTextByLength(String text, int length) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < text.length(); i += length) {
            result.add(text.substring(i, Math.min(i + length, text.length())));
        }
        return result;
    }

    private String centerText(String text, int width) {
        int padding = (width - text.length()) / 2;
        return " ".repeat(Math.max(0, padding)) + text;
    }

    private String repeat(String s, int count) {
        return s.repeat(Math.max(0, count));
    }

    private String formatCurrency(double amount) {
        return String.format("%,.0f", amount); // Tambah pemisah ribuan
    }

    private String formatSummary(String label, int qty, double amount) {
        String qtyPart = qty > 0 ? String.format("%-3d", qty) : "";
        return String.format("%-12s %3s %13s\n", label + ":", qtyPart, formatCurrency(amount));
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
        [F8]       : Pilih produk pertama di tabel
        [F9]       : Pilih produk terakhir di tabel
        [F10]      : Edit Qty produk terpilih
        [F11]      : Print struk
        [F12]      : Copy struk ke clipboard
        """;
        MessageDialogUtil.showInfo(info);
    }

    private void showProductSearchPopup(String keyword) {
        List<Product> result = productRepo.searchProducts(keyword); // implementasi bebas, misal LIKE barcode/nama

        if (result.isEmpty()) {
            MessageDialogUtil.showWarning("Produk tidak ditemukan!");
            return;
        }

        Stage popupStage = new Stage();
        popupStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        TableView<Product> tableView = new TableView<>();
        TableColumn<Product, String> colBarcode = new TableColumn<>("Barcode");
        colBarcode.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getBarcode()));

        TableColumn<Product, String> colName = new TableColumn<>("Name");
        colName.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getName()));

        TableColumn<Product, Double> colPrice = new TableColumn<>("Price");
        colPrice.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getPrice()));
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
        TableColumn<Product, Integer> colStock = new TableColumn<>("stock");
        colStock.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getStock()));
        tableView.getColumns().addAll(colBarcode, colName, colPrice, colStock);
        tableView.setItems(javafx.collections.FXCollections.observableArrayList(result));
        tableView.setPrefWidth(400);
        tableView.setPrefHeight(300);

        // Pilih produk double click/ENTER
        tableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Product selected = tableView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    addProductToCart(selected);
                    popupStage.close();
                }
            }
        });

        tableView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                Product selected = tableView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    addProductToCart(selected);
                    popupStage.close();
                }
            } else if (event.getCode() == KeyCode.ESCAPE) {
                popupStage.close();
            }
        });


        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(tableView);
        root.setPadding(new javafx.geometry.Insets(10));
        Scene scene = new Scene(root);
        popupStage.setScene(scene);
        popupStage.setTitle("Cari Produk");
        popupStage.showAndWait();
        Platform.runLater(() -> tableView.requestFocus());
    }

    // Tambahkan helper untuk add produk dari popup
    private void addProductToCart(Product product) {
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
