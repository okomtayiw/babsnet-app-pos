package com.babsnet.posapp.controller;

import com.babsnet.posapp.model.Product;
import com.babsnet.posapp.model.PurchaseItem;
import com.babsnet.posapp.model.Supplier;
import com.babsnet.posapp.model.User;
import com.babsnet.posapp.repository.ProductRepository;
import com.babsnet.posapp.repository.PurchaseDAO;
import com.babsnet.posapp.repository.SupplierRepository;
import com.babsnet.posapp.session.SessionManager;
import com.babsnet.posapp.util.FocusablePage;
import com.babsnet.posapp.util.FormatUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.time.LocalDate;

public class PurchaseController implements FocusablePage {

    @FXML
    private ComboBox<Supplier> supplierComboBox;

    @FXML
    private TextField invoiceField;

    @FXML
    private DatePicker purchaseDatePicker;

    @FXML
    private TextField barcodeField;

    @FXML
    private TextField buyPriceField;

    @FXML
    private TextField quantityField;

    @FXML
    private TableView<PurchaseItem> purchaseTable;

    @FXML
    private TableColumn<PurchaseItem, Number> colID;

    @FXML
    private TableColumn<PurchaseItem, String> colName;

    @FXML
    private TableColumn<PurchaseItem, Integer> colQty;

    @FXML
    private TableColumn<PurchaseItem, Double> colBuyPrice;

    @FXML
    private TableColumn<PurchaseItem, Double> colSubtotal;

    @FXML
    private Text totalText;

    @FXML
    private Button updateButton;

    @FXML
    private Button deleteButton;

    @FXML
    private VBox rootVBoxPurchase;


    private final EventHandler<KeyEvent> shortcutHandler = this::handleKeyEvents;

    private final ObservableList<PurchaseItem> purchaseItems = FXCollections.observableArrayList();
    User currentUser = SessionManager.getInstance().getCurrentUser();

    public void initialize() {

        rootVBoxPurchase.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null) oldScene.removeEventFilter(KeyEvent.KEY_PRESSED, shortcutHandler);
            if (newScene != null) newScene.addEventFilter(KeyEvent.KEY_PRESSED, shortcutHandler);
        });
        ObservableList<Supplier> suppliers =  FXCollections.observableArrayList(SupplierRepository.getAllSuppliers());
        supplierComboBox.setItems(suppliers);
        purchaseDatePicker.setValue(LocalDate.now());
        if (!suppliers.isEmpty()) {
            supplierComboBox.setValue(suppliers.getFirst());
        }

        supplierComboBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Supplier item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getName());
            }
        });
        supplierComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Supplier item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getName());
            }
        });

        purchaseTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            updateButton.setDisable(newSelection == null);
            if (newSelection != null) {
                barcodeField.setText(newSelection.getBarcode());
                buyPriceField.setText(String.valueOf(newSelection.getBuyPrice()));
                quantityField.setText(String.valueOf(newSelection.getQuantity()));
            }
        });

        purchaseTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean hasSelection = newSelection != null;
            updateButton.setDisable(!hasSelection);
            deleteButton.setDisable(!hasSelection);

            if (hasSelection) {
                barcodeField.setText(newSelection.getBarcode());
                buyPriceField.setText(String.valueOf(newSelection.getBuyPrice()));
                quantityField.setText(String.valueOf(newSelection.getQuantity()));
            }
        });

        colID.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(purchaseTable.getItems().indexOf(cellData.getValue()) + 1));
        colName.setCellValueFactory(data -> {
            Product product = data.getValue().getProduct();
            return new SimpleStringProperty(product != null ? product.getName() : "Unknown");
        });
        colQty.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getQuantity()).asObject());
        colBuyPrice.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getBuyPrice()).asObject());
        colBuyPrice.setCellFactory(tc -> new TableCell<PurchaseItem, Double>() {
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
        colSubtotal.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getSubTotal()).asObject());
        colSubtotal.setCellFactory(tc -> new TableCell<PurchaseItem, Double>() {
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
        purchaseTable.setItems(purchaseItems);

    }

    private void handleKeyEvents(KeyEvent event) {
        // 1. Jika fokus di purchaseTable
        if (purchaseTable.isFocused()) {
            int selIdx = purchaseTable.getSelectionModel().getSelectedIndex();
            int itemCount = purchaseTable.getItems().size();

            // Edit (F2)
            if (selIdx >= 0 && event.getCode() == KeyCode.F2) {
                Platform.runLater(this::updateSelectedProduct);
                event.consume();
                return;
            }
            // Delete (Del)
            if (selIdx >= 0 && event.getCode() == KeyCode.DELETE) {
                Platform.runLater(this::deleteSelectedProduct);
                event.consume();
                return;
            }
            // Navigasi UP
            if (event.getCode() == KeyCode.UP && selIdx > 0) {
                Platform.runLater(() -> {
                    purchaseTable.getSelectionModel().select(selIdx - 1);
                    purchaseTable.scrollTo(selIdx - 1);
                });
                event.consume();
                return;
            }
            // Navigasi DOWN
            if (event.getCode() == KeyCode.DOWN && selIdx < itemCount - 1) {
                Platform.runLater(() -> {
                    purchaseTable.getSelectionModel().select(selIdx + 1);
                    purchaseTable.scrollTo(selIdx + 1);
                });
                event.consume();
                return;
            }
            // HOME = pilih baris pertama
            if (event.getCode() == KeyCode.HOME && itemCount > 0) {
                Platform.runLater(() -> {
                    purchaseTable.getSelectionModel().select(0);
                    purchaseTable.scrollTo(0);
                });
                event.consume();
                return;
            }
            // END = pilih baris terakhir
            if (event.getCode() == KeyCode.END && itemCount > 0) {
                Platform.runLater(() -> {
                    purchaseTable.getSelectionModel().select(itemCount - 1);
                    purchaseTable.scrollTo(itemCount - 1);
                });
                event.consume();
                return;
            }
            // Balik ke barcode field (Ctrl+R)
            if (event.isControlDown() && event.getCode() == KeyCode.R) {
                Platform.runLater(() -> barcodeField.requestFocus());
                event.consume();
                return;
            }
        }

        // 2. Jika fokus di barcodeField atau input lain
        if (barcodeField.isFocused() || buyPriceField.isFocused() || quantityField.isFocused()) {
            // Fokus ke tabel (Ctrl+T)
            if (event.isControlDown() && event.getCode() == KeyCode.T) {
                if (!purchaseItems.isEmpty()) {
                    Platform.runLater(() -> {
                        purchaseTable.requestFocus();
                        purchaseTable.getSelectionModel().select(0);
                        purchaseTable.scrollTo(0);
                    });
                }
                event.consume();
                return;
            }
        }

        if (event.isControlDown() && event.getCode() == KeyCode.S) {
            Platform.runLater(() -> supplierComboBox.requestFocus());
            supplierComboBox.show();
            event.consume();
            return;
        }


        // 3. Shortcut global
        // Ctrl+F: Fokus ke barcodeField
        if (event.isControlDown() && event.getCode() == KeyCode.F) {
            Platform.runLater(() -> {
                barcodeField.requestFocus();
                barcodeField.selectAll();
            });
            event.consume();
            return;
        }

        if (event.isControlDown() && event.getCode() == KeyCode.D) {
            Platform.runLater(() -> {
                purchaseDatePicker.requestFocus();
                // Paksa buka popup kalender
                purchaseDatePicker.show();
            });
            event.consume();
            return;
        }

        // Ctrl+L: Reset purchase list (seperti refresh di history)
        if (event.isControlDown() && event.getCode() == KeyCode.L) {
            Platform.runLater(this::clearForm);
            event.consume();
            return;
        }

        // Fokus ke invoiceField (Ctrl+N)
        if (event.isControlDown() && event.getCode() == KeyCode.N) {
            Platform.runLater(() -> {
                invoiceField.requestFocus();
                invoiceField.selectAll();
            });
            event.consume();
            return;
        }

        if (event.isControlDown() && event.getCode() == KeyCode.B) {
            Platform.runLater(() -> {
                buyPriceField.requestFocus();
                buyPriceField.selectAll();
            });
            event.consume();
            return;
        }

        // Fokus ke Quantity (Ctrl+Q)
        if (event.isControlDown() && event.getCode() == KeyCode.Q) {
            Platform.runLater(() -> {
                quantityField.requestFocus();
                quantityField.selectAll();
            });
            event.consume();
            return;
        }

        // Save purchase (Ctrl+Enter)
        if (event.isControlDown() && event.getCode() == KeyCode.ENTER) {
            Platform.runLater(this::savePurchase);
            event.consume();
            return;
        }

        // Shortcut: Ctrl+A untuk tambah produk ke tabel
        if (event.isControlDown() && event.getCode() == KeyCode.A) {
            Platform.runLater(this::addProductToPurchase);
            event.consume();
            return;
        }

        // (Opsional) Enter di Quantity field -> Add Product juga
        if (quantityField.isFocused() && event.getCode() == KeyCode.ENTER) {
            Platform.runLater(this::addProductToPurchase);
            event.consume();
            return;
        }

        // Show shortcut info (Ctrl+I)
        if (event.isControlDown() && event.getCode() == KeyCode.I) {
            showShortcutInfo();
            event.consume();
        }
    }

    @FXML
    public void showShortcutInfo() {
        String info = """
            Keyboard Shortcut Purchase

            - Ctrl + F   : Fokus ke kolom barcode
            - Ctrl + S   : Fokus & buka Supplier
            - Ctrl + D   : Fokus & buka DatePicker (pilih tanggal)
            - Ctrl + N   : Fokus ke Invoice Number (Nomor Invoice)
            - Ctrl + B   : Fokus ke Buy Price
            - Ctrl + Q   : Fokus ke Quantity
            - Ctrl + T   : Fokus ke tabel purchase
            - Ctrl + R   : Balik fokus ke barcodeField
            - Ctrl + A   : Tambah produk ke tabel (Add Product)
            - Home/End   : Pilih produk pertama/terakhir di tabel
            - Up/Down    : Navigasi produk di tabel
            - F2         : Edit produk (fokus di tabel)
            - Del        : Hapus produk (fokus di tabel)
            - Ctrl + L   : Reset form/input list baru
            - Ctrl + I   : Tampilkan info shortcut keyboard
            - Ctrl + Enter: Simpan purchase (fokus di tombol Save)
            """;
        TextArea area = new TextArea(info);
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefColumnCount(36);
        area.setPrefRowCount(17);
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("Daftar Shortcut Keyboard");
        dialog.setHeaderText("Keyboard Shortcut Purchase");
        dialog.getDialogPane().setContent(area);
        dialog.showAndWait();
    }



    @FXML
    public void deleteSelectedProduct() {
        PurchaseItem selectedItem = purchaseTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) return;

        purchaseItems.remove(selectedItem);  // hapus dari list
        purchaseTable.getSelectionModel().clearSelection(); // clear selection setelah hapus

        updateTotal();

        barcodeField.clear();
        buyPriceField.clear();
        quantityField.clear();
    }



    @FXML
    public void updateSelectedProduct() {
        PurchaseItem selectedItem = purchaseTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) return;

        try {
            double buyPrice = Double.parseDouble(buyPriceField.getText());
            int quantity = Integer.parseInt(quantityField.getText());

            selectedItem.setBuyPrice(buyPrice);
            selectedItem.setQuantity(quantity);
            selectedItem.setBarcode(barcodeField.getText());

            // Refresh TableView
            purchaseTable.refresh();

            updateTotal();

            barcodeField.clear();
            buyPriceField.clear();
            quantityField.clear();
            purchaseTable.getSelectionModel().clearSelection();

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter valid numbers for price and quantity.");
        }
    }


    @FXML
    public void addProductToPurchase() {
        String barcode = barcodeField.getText();
        double buyPrice;
        int quantity;

        try {
            buyPrice = Double.parseDouble(buyPriceField.getText());
            quantity = Integer.parseInt(quantityField.getText());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter valid numbers for price and quantity.");
            return;
        }
        Product productExisting = ProductRepository.getProductByBarcode(barcode);
        if (productExisting == null) {
            showAlert(Alert.AlertType.ERROR, "Invalid Input", "Product is empty please input data product");
        }

        PurchaseItem existingItem = purchaseItems.stream()
                .filter(item -> item.getBarcode().equals(barcode))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
            existingItem.setBuyPrice(buyPrice);
            existingItem.setSubTotal(existingItem.getQuantity() + quantity * buyPrice);
        } else {
            PurchaseItem item = new PurchaseItem();
            item.setProduct(productExisting);
            item.setBarcode(barcode);
            item.setBuyPrice(buyPrice);
            item.setQuantity(quantity);
            item.setSubTotal(quantity * buyPrice);
            purchaseItems.add(item);
        }

        // Refresh TableView
        purchaseTable.refresh();
        updateTotal();

        barcodeField.clear();
        buyPriceField.clear();
        quantityField.clear();
    }

    private void updateTotal() {
        double total = purchaseItems.stream().mapToDouble(PurchaseItem::getSubTotal).sum();
        totalText.setText(FormatUtil.toRupiahNoDecimal(total));
    }

    @FXML
    public void savePurchase() {
        Supplier selectedSupplier = supplierComboBox.getValue();
        String invoice = invoiceField.getText();
        LocalDate purchaseDate = purchaseDatePicker.getValue();
        double total = purchaseItems.stream().mapToDouble(PurchaseItem::getSubTotal).sum();

        if (selectedSupplier == null || invoice.isEmpty() || purchaseDate == null || purchaseItems.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Incomplete Data", "Please fill all purchase information and add at least one product.");
            return;
        }

        try {
            int purchaseId = PurchaseDAO.insertPurchase(invoice, purchaseDate, selectedSupplier.getId(), total, currentUser);
            PurchaseDAO.insertPurchaseDetails(purchaseId, purchaseItems);

            showAlert(Alert.AlertType.INFORMATION, "Success", "Purchase saved successfully!");
            clearForm();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to save purchase.");
        }
    }


    private void clearForm() {
        supplierComboBox.setValue(null);
        invoiceField.clear();
        purchaseDatePicker.setValue(null);
        purchaseItems.clear();
        totalText.setText("0.00");
        barcodeField.clear();
        buyPriceField.clear();
        quantityField.clear();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @Override
    public void focusRootBox() {
        Platform.runLater(() -> rootVBoxPurchase.requestFocus());
    }
}
