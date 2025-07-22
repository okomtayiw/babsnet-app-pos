package com.babsnet.posapp.controller;

import com.babsnet.posapp.model.Product;
import com.babsnet.posapp.model.Purchase;
import com.babsnet.posapp.model.PurchaseItem;
import com.babsnet.posapp.model.Supplier;
import com.babsnet.posapp.repository.ProductRepository;
import com.babsnet.posapp.repository.PurchaseDAO;
import com.babsnet.posapp.repository.SupplierRepository;
import com.babsnet.posapp.util.FocusablePage;
import com.babsnet.posapp.util.FormatUtil;
import com.babsnet.posapp.util.MessageDialogUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;

public class EditPurchaseController implements FocusablePage {

    @FXML private ComboBox<String> supplierComboBox;
    @FXML private TextField invoiceField;
    @FXML private DatePicker purchaseDatePicker;
    @FXML private TextField barcodeField;
    @FXML private TextField buyPriceField;
    @FXML private TextField quantityField;
    @FXML private TableView<PurchaseItem> purchaseTable;
    @FXML private TableColumn<PurchaseItem, Integer> colID;
    @FXML private TableColumn<PurchaseItem, String> colName;
    @FXML private TableColumn<PurchaseItem, Integer> colQty;
    @FXML private TableColumn<PurchaseItem, Double> colBuyPrice;
    @FXML private TableColumn<PurchaseItem, Double> colSubtotal;
    @FXML private Button updateButton;
    @FXML private Button deleteButton;
    @FXML private Button clearButton;
    @FXML private Button addButton;
    @FXML private Button saveChangesButton;
    @FXML private VBox rootVBoxEditPurchase;

    private final ObservableList<PurchaseItem> purchaseItems = FXCollections.observableArrayList();
    private int purchaseId;

    // Shortcut handler khusus halaman ini
    private final EventHandler<KeyEvent> shortcutHandler = this::handleShortcutKeys;

    @FXML
    public void initialize() {
        // Pasang event filter hanya saat halaman ini aktif di scene
        rootVBoxEditPurchase.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null) oldScene.removeEventFilter(KeyEvent.KEY_PRESSED, shortcutHandler);
            if (newScene != null) newScene.addEventFilter(KeyEvent.KEY_PRESSED, shortcutHandler);
        });
        // Agar root bisa menerima fokus langsung, supaya shortcut selalu aktif
        Platform.runLater(() -> rootVBoxEditPurchase.requestFocus());
    }

    public void loadPurchase(Purchase purchase, List<PurchaseItem> items) {
        ObservableList<String> supplierNames = SupplierRepository.getAllSuppliers().stream()
                .map(Supplier::getName)
                .collect(FXCollections::observableArrayList, ObservableList::add, ObservableList::addAll);

        supplierComboBox.setItems(supplierNames);
        this.purchaseId = purchase.getId();
        supplierComboBox.setValue(purchase.getSupplierName());
        invoiceField.setText(purchase.getPurchaseNumber());
        purchaseDatePicker.setValue(purchase.getPurchaseDate());

        purchaseItems.setAll(items);

        colID.setCellValueFactory(cellData -> new SimpleObjectProperty<>(null)); // dummy value
        colID.setCellFactory(col -> new TableCell<PurchaseItem, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null); setGraphic(null);
                } else {
                    setText(String.valueOf(getIndex() + 1));
                }
            }
        });
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProduct().getName()));
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

        purchaseTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean selected = newSelection != null;
            updateButton.setDisable(!selected);
            deleteButton.setDisable(!selected);
            clearButton.setDisable(!selected);
            if (selected) {
                barcodeField.setText(newSelection.getBarcode());
                buyPriceField.setText(String.valueOf(newSelection.getBuyPrice()));
                quantityField.setText(String.valueOf(newSelection.getQuantity()));
                addButton.setDisable(true);
                saveChangesButton.setDisable(true);
            } else {
                addButton.setDisable(false);
                saveChangesButton.setDisable(false);
            }
        });
    }

    private void handleShortcutKeys(KeyEvent event) {
        // Supaya shortcut tetap aktif walau di field. Jika ingin nonaktif saat di field, aktifkan baris ini:
        // if (event.getTarget() instanceof TextInputControl) return;

        KeyCode code = event.getCode();

        if (event.isControlDown() && code == KeyCode.B) {
            barcodeField.requestFocus(); barcodeField.selectAll(); event.consume();
        }
        else if (event.isControlDown() && code == KeyCode.P) {
            buyPriceField.requestFocus(); buyPriceField.selectAll(); event.consume();
        }
        else if (event.isControlDown() && code == KeyCode.Q) {
            quantityField.requestFocus(); quantityField.selectAll(); event.consume();
        }
        else if (event.isControlDown() && code == KeyCode.N) {
            invoiceField.requestFocus(); invoiceField.selectAll(); event.consume();
        }
        else if (event.isControlDown() && code == KeyCode.S) {
            supplierComboBox.requestFocus(); supplierComboBox.show(); event.consume();
        }
        else if (event.isControlDown() && code == KeyCode.D) {
            purchaseDatePicker.requestFocus(); purchaseDatePicker.show(); event.consume();
        }
        else if (event.isControlDown() && code == KeyCode.A) {
            addProductToPurchase(); event.consume();
        }
        else if (event.isControlDown() && code == KeyCode.E) {
            updateSelectedProduct(); event.consume();
        }
        else if (code == KeyCode.DELETE) {
            deleteSelectedProduct(); event.consume();
        }
        else if (event.isControlDown() && code == KeyCode.L) {
            clearSelectedProduct(); event.consume();
        }
        else if (event.isControlDown() && code == KeyCode.T) {
            if (!purchaseItems.isEmpty()) {
                purchaseTable.requestFocus();
                int idx = purchaseTable.getSelectionModel().getSelectedIndex();
                if (idx < 0) idx = 0;
                purchaseTable.getSelectionModel().select(idx);
                purchaseTable.scrollTo(idx);
            }
            event.consume();
        }
        else if (event.isControlDown() && code == KeyCode.C) {
            cancel();
            event.consume();
        }
        else if (event.isControlDown() && code == KeyCode.ENTER) {
            saveChanges(); event.consume();
        }
        else if (purchaseTable.isFocused()) {
            int idx = purchaseTable.getSelectionModel().getSelectedIndex();
            int lastIdx = purchaseTable.getItems().size() - 1;
            if (code == KeyCode.HOME && lastIdx >= 0) {
                purchaseTable.getSelectionModel().select(0); purchaseTable.scrollTo(0); event.consume();
            } else if (code == KeyCode.END && lastIdx >= 0) {
                purchaseTable.getSelectionModel().select(lastIdx); purchaseTable.scrollTo(lastIdx); event.consume();
            } else if (code == KeyCode.UP && idx > 0) {
                purchaseTable.getSelectionModel().select(idx - 1); purchaseTable.scrollTo(idx - 1); event.consume();
            } else if (code == KeyCode.DOWN && idx < lastIdx) {
                purchaseTable.getSelectionModel().select(idx + 1); purchaseTable.scrollTo(idx + 1); event.consume();
            }
        }
        else if (event.isControlDown() && code == KeyCode.I) {
            showShortcutInfo(); event.consume();
        }
    }

    @FXML
    public void showShortcutInfo() {
        String info = """
        Keyboard Shortcut Edit Purchase
        - Ctrl + B   : Fokus ke Barcode
        - Ctrl + P   : Fokus ke Buy Price
        - Ctrl + Q   : Fokus ke Quantity
        - Ctrl + N   : Fokus ke Invoice Number
        - Ctrl + S   : Fokus & buka Supplier
        - Ctrl + D   : Fokus & buka Date
        - Ctrl + T   : Fokus ke tabel purchase
        - Ctrl + A   : Tambah produk ke tabel
        - Ctrl + E   : Edit produk (jika terpilih)
        - Ctrl + L   : Clear produk terpilih
        - Delete     : Hapus produk
        - Ctrl + Enter: Simpan perubahan
        - Home/End   : Pilih produk pertama/terakhir di tabel
        - Up/Down    : Navigasi produk di tabel
        - Ctrl + I   : Tampilkan info shortcut keyboard
        """;
        TextArea area = new TextArea(info);
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefColumnCount(36);
        area.setPrefRowCount(16);
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("Daftar Shortcut Keyboard");
        dialog.setHeaderText("Keyboard Shortcut Edit Purchase");
        dialog.getDialogPane().setContent(area);
        dialog.showAndWait();
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
            showAlert("Invalid Input", "Please enter valid numbers for price and quantity.");
            return;
        }
        Product product = ProductRepository.getProductByBarcode(barcode);
        if (product == null) {
            showAlert("Invalid Input", "Product not found. Please add the product first.");
            return;
        }
        PurchaseItem existingItem = purchaseItems.stream()
                .filter(item -> item.getBarcode().equals(barcode))
                .findFirst()
                .orElse(null);
        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
            existingItem.setBuyPrice(buyPrice);
        } else {
            PurchaseItem item = new PurchaseItem();
            item.setProduct(product);
            item.setBarcode(barcode);
            item.setBuyPrice(buyPrice);
            item.setQuantity(quantity);
            item.setSubTotal(quantity * buyPrice);
            purchaseItems.add(item);
        }
        purchaseTable.refresh();
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
            selectedItem.setSubTotal(quantity * buyPrice);
            purchaseTable.refresh();
            barcodeField.clear();
            buyPriceField.clear();
            quantityField.clear();
            purchaseTable.getSelectionModel().clearSelection();
            saveChangesButton.setDisable(false);
        } catch (NumberFormatException e) {
            showAlert("Invalid Input", "Please enter valid numbers for price and quantity.");
        }
    }

    @FXML
    public void deleteSelectedProduct() {
        PurchaseItem selectedItem = purchaseTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) return;
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("Are you sure you want to delete product \"" + selectedItem.getProduct().getName() + "\"?");
        if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        purchaseItems.remove(selectedItem);
        purchaseTable.refresh();
        barcodeField.clear();
        buyPriceField.clear();
        quantityField.clear();
    }

    @FXML
    public void saveChanges() {
        String supplier = supplierComboBox.getValue();
        String invoice = invoiceField.getText();
        LocalDate purchaseDate = purchaseDatePicker.getValue();
        try {
            if(!purchaseItems.isEmpty()) {
                PurchaseDAO.updatePurchase(purchaseId, invoice, purchaseDate, supplier, purchaseItems);
                showAlert("Success", "Purchase updated successfully");
            } else {
                MessageDialogUtil.showInfo("Product is empty please add product.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to update purchase");
        }
    }

    @FXML
    public void cancel() {
        supplierComboBox.getScene().getWindow().hide();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void clearSelectedProduct() {
        purchaseTable.refresh();
        addButton.setDisable(false);
        saveChangesButton.setDisable(false);
        updateButton.setDisable(true);
        deleteButton.setDisable(true);
        clearButton.setDisable(true);
        purchaseTable.getSelectionModel().clearSelection();
        barcodeField.clear();
        buyPriceField.clear();
        quantityField.clear();
    }

    @Override
    public void focusRootBox() {
        Platform.runLater(() -> rootVBoxEditPurchase.requestFocus());
    }
}
