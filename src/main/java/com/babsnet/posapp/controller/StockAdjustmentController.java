package com.babsnet.posapp.controller;

import com.babsnet.posapp.model.Product;
import com.babsnet.posapp.model.Purchase;
import com.babsnet.posapp.model.StockAdjustment;
import com.babsnet.posapp.model.User;
import com.babsnet.posapp.repository.ProductRepository;
import com.babsnet.posapp.session.SessionManager;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

public class StockAdjustmentController {

    @FXML
    private TextField barcodeField;

    @FXML
    private TextField productNameField;

    @FXML
    private Label stockInfoLabel;

    @FXML
    private TextField quantityField;

    @FXML
    private ComboBox<String> reasonComboBox;

    @FXML
    private TextArea noteArea;

    @FXML
    private Button adjustButton;

    @FXML
    private Button adjustEditButton;

    @FXML
    private TableView<StockAdjustment> adjustmentTable;

    @FXML
    private TableColumn<StockAdjustment, Integer> colID;

    @FXML
    private TableColumn<StockAdjustment, String> colProduct;

    @FXML
    private TableColumn<StockAdjustment, String> colType;

    @FXML
    private TableColumn<StockAdjustment, Integer> colQty;

    @FXML
    private TableColumn<StockAdjustment, String> colNote;

    @FXML
    private TableColumn<StockAdjustment, String> colDate;

    @FXML
    private TableColumn<StockAdjustment, Void> colAction;

    @FXML private VBox rootBoxAdjustment;

    private boolean statusEdit = false;


    private Product selectedProduct = null;
    private StockAdjustment selectedForEdit = null;
    private final ObservableList<StockAdjustment> adjustmentList = FXCollections.observableArrayList();
    private final javafx.event.EventHandler<javafx.scene.input.KeyEvent> shortcutHandler = this::handleShortcutKeys;
    public void initialize() {

        adjustmentTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                editAdjustment(newSelection); // fungsi untuk isi field form dengan data yang terpilih
                statusEdit = true; // opsional: set mode edit otomatis
            } else {
                clearForm();
                statusEdit = false;
            }
        });
        // Setup reason
        reasonComboBox.setItems(FXCollections.observableArrayList(
                "REJECTED", "EXPIRED", "DAMAGED", "LOST", "RETURN", "CORRECTION"
        ));

        rootBoxAdjustment.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null) oldScene.removeEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, shortcutHandler);
            if (newScene != null) newScene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, shortcutHandler);
        });

        // Setup Table columns
        colID.setCellValueFactory(cellData -> new SimpleObjectProperty<>(null)); // dummy value, tidak pakai ID dari DB
        colID.setCellFactory(col -> new TableCell<StockAdjustment, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    // +1 biar urutan mulai dari 1
                    setText(String.valueOf(getIndex() + 1));
                }
            }
        });
        colProduct.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProductName()));
        colType.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getReason()));
        colQty.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getQuantity()).asObject());
        colNote.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescription()));
        colDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAdjustDate()));

        adjustmentTable.setItems(adjustmentList);
        addActionButtonsToTable();

        adjustEditButton.setDisable(true);

        // Load existing adjustments
        refreshAdjustmentTable();

        // Barcode listener: ketika enter di barcodeField, otomatis load produk
        barcodeField.setOnAction(event -> onBarcodeEntered());

        // Atau ada tombol 🔍 di FXML → onFindProduct
    }

    private void onBarcodeEntered() {
        String barcode = barcodeField.getText();
        selectedProduct = ProductRepository.getProductByBarcode(barcode);

        if (selectedProduct != null) {
            productNameField.setText(selectedProduct.getName());
            stockInfoLabel.setText("Stock: " + selectedProduct.getStock());
        } else {
            productNameField.setText("");
            stockInfoLabel.setText("Product not found!");
        }
    }

    @FXML
    private void onFindProduct() {
        onBarcodeEntered();
    }

    private void refreshAdjustmentTable() {
        List<StockAdjustment> adjustments = ProductRepository.getAllStockAdjustments();
        adjustmentList.setAll(adjustments);
    }

    @FXML
    private void adjustStock() {
        if (selectedProduct == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Scan barcode dan pastikan produk ditemukan.");
            return;
        }
        String reason = reasonComboBox.getValue();
        User currentUser = SessionManager.getInstance().getCurrentUser();

        int quantity;
        try {
            quantity = Integer.parseInt(quantityField.getText());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Invalid Quantity", "Please enter a valid quantity.");
            return;
        }

        if (reason == null || quantity <= 0) {
            showAlert(Alert.AlertType.WARNING, "Incomplete Data", "Please complete all fields.");
            return;
        }

        try {
            ProductRepository.adjustStock(selectedProduct.getId(), quantity, reason, noteArea.getText(), currentUser);
            showAlert(Alert.AlertType.INFORMATION, "Success", "Stock adjusted successfully.");
            clearForm();
            refreshAdjustmentTable();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to adjust stock.");
        }
    }

    @FXML
    private void clearForm() {
        barcodeField.clear();
        productNameField.clear();
        stockInfoLabel.setText("");
        selectedProduct = null;
        quantityField.clear();
        reasonComboBox.setValue(null);
        noteArea.clear();
        adjustEditButton.setDisable(true);
        adjustButton.setDisable(false);
        selectedForEdit = null;
    }

    private void addActionButtonsToTable() {
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox pane = new HBox(6, editBtn, deleteBtn);

            {
                editBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size:10;");
                deleteBtn.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; -fx-font-size:10;");

                editBtn.setOnAction(event -> {
                    StockAdjustment data = getTableView().getItems().get(getIndex());
                    editAdjustment(data);
                });
                deleteBtn.setOnAction(event -> {
                    StockAdjustment data = getTableView().getItems().get(getIndex());
                    deleteAdjustment(data);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private void deleteAdjustment(StockAdjustment adjustment) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to delete this stock adjustment?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Delete Confirmation");
        alert.showAndWait();

        if (alert.getResult() == ButtonType.YES) {
            boolean success = ProductRepository.deleteStockAdjustment(adjustment.getId());
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Deleted", "Adjustment deleted successfully.");
                refreshAdjustmentTable();
            } else {
                showAlert(Alert.AlertType.ERROR, "Failed", "Failed to delete adjustment.");
            }
        }
    }

    private void editAdjustment(StockAdjustment adjustment) {
        selectedForEdit = adjustment;

        barcodeField.setText(adjustment.getBarcode());
        onBarcodeEntered(); // auto load product & info
        quantityField.setText(String.valueOf(Math.abs(adjustment.getQuantity())));
        reasonComboBox.setValue(adjustment.getReason());
        noteArea.setText(adjustment.getDescription());

        adjustEditButton.setText("Save Update");
        adjustEditButton.setDisable(false);
        adjustButton.setDisable(true);
        statusEdit = true;
    }

    @FXML
    public void adjustEditStock() {
        if (selectedForEdit == null) return;
        if (selectedProduct == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Scan barcode dan pastikan produk ditemukan.");
            return;
        }
        String reason = reasonComboBox.getValue();
        User currentUser = SessionManager.getInstance().getCurrentUser();

        int quantity;
        try {
            quantity = Integer.parseInt(quantityField.getText());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Invalid Quantity", "Please enter a valid quantity.");
            return;
        }

        if (reason == null || quantity <= 0) {
            showAlert(Alert.AlertType.WARNING, "Incomplete Data", "Please complete all fields.");
            return;
        }

        try {
            ProductRepository.updateStockAdjustment(selectedForEdit.getId(),
                    selectedProduct.getId(), quantity, reason, noteArea.getText(), currentUser);
            showAlert(Alert.AlertType.INFORMATION, "Success", "Stock adjustment updated.");
            clearForm();
            refreshAdjustmentTable();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to update adjustment.");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void handleShortcutKeys(javafx.scene.input.KeyEvent event) {
        var code = event.getCode();

        // Input shortcuts (form)
        if (event.isControlDown() && code == javafx.scene.input.KeyCode.B) {
            barcodeField.requestFocus(); barcodeField.selectAll(); event.consume();
        }
        else if (event.isControlDown() && code == javafx.scene.input.KeyCode.Q) {
            quantityField.requestFocus(); quantityField.selectAll(); event.consume();
        }
        else if (event.isControlDown() && code == javafx.scene.input.KeyCode.R) {
            reasonComboBox.requestFocus(); reasonComboBox.show(); event.consume();
        }
        else if (event.isControlDown() && code == javafx.scene.input.KeyCode.N) {
            noteArea.requestFocus(); event.consume();
        }
        // Tambah baru
        else if (!statusEdit && event.isControlDown() && code == javafx.scene.input.KeyCode.A) {
            adjustButton.fire(); event.consume();
        }


        // Clear/reset
        else if (event.isControlDown() && code == javafx.scene.input.KeyCode.L) {
            clearForm(); statusEdit = false; event.consume();
        }
        // Fokus ke tabel
        else if (event.isControlDown() && code == javafx.scene.input.KeyCode.F) {
            if (!adjustmentList.isEmpty()) {
                adjustmentTable.requestFocus();
                int idx = adjustmentTable.getSelectionModel().getSelectedIndex();
                if (idx < 0) idx = 0;
                adjustmentTable.getSelectionModel().select(idx);
                adjustmentTable.scrollTo(idx);
            }
            event.consume();
        }
        // Delete baris (jika table fokus)
        else if (code == javafx.scene.input.KeyCode.DELETE && adjustmentTable.isFocused()) {
            int selIdx = adjustmentTable.getSelectionModel().getSelectedIndex();
            if (selIdx >= 0) {
                StockAdjustment selected = adjustmentTable.getItems().get(selIdx);
                deleteAdjustment(selected);
            }
            event.consume();
        }



        else if (statusEdit && event.isControlDown() && code == javafx.scene.input.KeyCode.S && adjustmentTable.isFocused()) {
            if (!adjustEditButton.isDisabled()) {
                adjustEditButton.fire();
                statusEdit = false;
            }
            event.consume();
        }



        else if ((event.isControlDown() && code == javafx.scene.input.KeyCode.S) ) {
            if (statusEdit && !adjustEditButton.isDisabled()) {
                adjustEditButton.fire();
                statusEdit = false;
            } else if (!statusEdit && !adjustButton.isDisabled()) {
                adjustButton.fire();
            }
            event.consume();
        }
        else if (event.isControlDown() && code == KeyCode.ENTER) {
            onFindProduct();
            event.consume();
        }
        // Show shortcut info
        else if (event.isControlDown() && code == javafx.scene.input.KeyCode.I) {
            showShortcutInfo(); event.consume();
        }
    }

    @FXML
    public void showShortcutInfo() {
        String info = """
    Keyboard Shortcut Stock Adjustment

    - Ctrl + B  : Fokus ke Barcode
    - Ctrl + Q  : Fokus ke Quantity
    - Ctrl + R  : Fokus & buka Alasan (Reason)
    - Ctrl + N  : Fokus ke Catatan (Note)
    - Ctrl + A  : Tambah (adjust) stok
    - Ctrl + E  : Simpan perubahan (edit)
    - Ctrl + L  : Clear/reset form
    - Ctrl + F  : Fokus ke tabel adjustment
    - Del       : Hapus baris terpilih (fokus tabel)
    - Ctrl + I  : Tampilkan info shortcut ini
    - Ctrl + S  : Simpan data
    """;
        TextArea area = new TextArea(info);
        area.setEditable(false); area.setWrapText(true); area.setPrefColumnCount(36); area.setPrefRowCount(13);
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("Daftar Shortcut Keyboard");
        dialog.setHeaderText("Keyboard Shortcut Stock Adjustment");
        dialog.getDialogPane().setContent(area);
        dialog.showAndWait();
    }


}
