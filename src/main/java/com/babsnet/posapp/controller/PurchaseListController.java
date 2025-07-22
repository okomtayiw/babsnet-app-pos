package com.babsnet.posapp.controller;

import com.babsnet.posapp.model.Purchase;
import com.babsnet.posapp.model.PurchaseItem;
import com.babsnet.posapp.repository.PurchaseDAO;
import com.babsnet.posapp.util.FocusablePage;
import com.babsnet.posapp.util.FormatUtil;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

public class PurchaseListController implements FocusablePage {

    @FXML
    private TableView<Purchase> purchaseTable;

    @FXML
    private TableColumn<Purchase, Integer> colId;

    @FXML
    private TableColumn<Purchase, String> colNumber;

    @FXML
    private TableColumn<Purchase, String> colDate;

    @FXML
    private TableColumn<Purchase, String> colSupplier;

    @FXML
    private TableColumn<Purchase, Double> colTotal;

    @FXML
    private TableColumn<Purchase, String> colStatus;

    @FXML
    private TableView<PurchaseItem> purchaseDetailTable;

    @FXML
    private TableColumn<PurchaseItem, Integer> colIdItemDetail;

    @FXML
    private TableColumn<PurchaseItem, String> colProductName;

    @FXML
    private TableColumn<PurchaseItem, Integer> colQty;

    @FXML
    private TableColumn<PurchaseItem, Double> colBuyPrice;

    @FXML
    private TableColumn<PurchaseItem, Double> colSubtotal;

    @FXML
    private Button editButton;

    @FXML
    private Button cancelButton;

    private Stage editPurchaseStage;

    @FXML
    private TextField searchField;

    @FXML
    private VBox rootPurchaseList;

    private final ObservableList<Purchase> purchaseList = FXCollections.observableArrayList();
    private final EventHandler<KeyEvent> shortcutHandler = this::handleShortcutKeys;
    public void initialize() {

        colId.setCellValueFactory(cellData -> new SimpleObjectProperty<>(null)); // dummy value, tidak pakai ID dari DB
        colId.setCellFactory(col -> new TableCell<Purchase, Integer>() {
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
        colNumber.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPurchaseNumber()));
        colDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPurchaseDate().toString()));
        colSupplier.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSupplierName()));
        colTotal.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getTotal()).asObject());
        colTotal.setCellFactory(tc -> new TableCell<Purchase, Double>() {
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
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));

        colIdItemDetail.setCellValueFactory(cellData -> new SimpleObjectProperty<>(null));
        colIdItemDetail.setCellFactory(col -> new TableCell<PurchaseItem, Integer>() {
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

        colProductName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProduct().getName()));
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

        purchaseTable.setItems(purchaseList);
        refreshPurchaseList();

        purchaseTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            boolean selected = newSel != null;
            editButton.setDisable(!selected);
            cancelButton.setDisable(!selected);

            if (selected) {
                List<PurchaseItem> details = PurchaseDAO.getPurchaseDetails(newSel.getId());
                purchaseDetailTable.setItems(FXCollections.observableArrayList(details));
            } else {
                purchaseDetailTable.getItems().clear();
            }
        });

        colStatus.setCellFactory(column -> new TableCell<Purchase, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    if (status.equalsIgnoreCase("ACTIVE")) {
                        setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                    } else if (status.equalsIgnoreCase("CANCELLED")) {
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: black;");
                    }
                }
            }
        });

        rootPurchaseList.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null) oldScene.removeEventFilter(KeyEvent.KEY_PRESSED, shortcutHandler);
            if (newScene != null) newScene.addEventFilter(KeyEvent.KEY_PRESSED, shortcutHandler);
        });



    }

    @FXML
    public void refreshPurchaseList() {
        purchaseList.setAll(PurchaseDAO.getAllPurchases());
        purchaseDetailTable.getItems().clear();
    }

    @FXML
    public void editPurchase() {
        Purchase selectedPurchase = purchaseTable.getSelectionModel().getSelectedItem();
        if (selectedPurchase == null) return;

        try {
            if (editPurchaseStage != null && editPurchaseStage.isShowing()) {
                editPurchaseStage.toFront();
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/babsnet/posapp/editpurchase.fxml"));
            Scene scene = new Scene(loader.load());

            EditPurchaseController editController = loader.getController();
            List<PurchaseItem> items = PurchaseDAO.getPurchaseDetails(selectedPurchase.getId());
            editController.loadPurchase(selectedPurchase, items);

            editPurchaseStage = new Stage();
            editPurchaseStage.setTitle("Edit Purchase");
            editPurchaseStage.setScene(scene);
            editPurchaseStage.setOnCloseRequest(event -> editPurchaseStage = null);
            editPurchaseStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @FXML
    public void cancelPurchase() {
        Purchase selected = purchaseTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Cancel this purchase?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait();

        if (alert.getResult() == ButtonType.YES) {
            PurchaseDAO.cancelAndRollback(selected.getId());
            refreshPurchaseList();
        }
    }

    @FXML
    public void searchPurchase() {
        String keyword = searchField.getText().trim().toLowerCase();
        if (keyword.isEmpty()) {
            refreshPurchaseList();
            return;
        }

        List<Purchase> allPurchases = PurchaseDAO.getAllPurchases();
        List<Purchase> filtered = allPurchases.stream()
                .filter(p -> p.getPurchaseNumber().toLowerCase().contains(keyword))
                .toList();

        purchaseList.setAll(filtered);
        purchaseTable.setItems(purchaseList);
    }

    @FXML
    public void clearSearch() {
        searchField.clear();
        refreshPurchaseList();
    }

    private void handleShortcutKeys(KeyEvent event) {
        // Ignore jika sedang di input field
//        if (event.getTarget() instanceof TextInputControl) return;

        // === Ctrl+O: Fokus ke tabel & select baris pertama ===
        if (event.isControlDown() && event.getCode() == KeyCode.O) {
            if (!purchaseList.isEmpty()) {
                purchaseTable.requestFocus();
                purchaseTable.getSelectionModel().select(0);
                purchaseTable.scrollTo(0);
            }
            event.consume();
            return;
        }

        // Tabel: Home/End/Up/Down/Edit/Cancel
        if (purchaseTable.isFocused()) {
            int idx = purchaseTable.getSelectionModel().getSelectedIndex();
            int lastIdx = purchaseList.size() - 1;

            if (event.getCode() == KeyCode.HOME && lastIdx >= 0) {
                purchaseTable.getSelectionModel().select(0);
                purchaseTable.scrollTo(0);
                event.consume();
            } else if (event.getCode() == KeyCode.END && lastIdx >= 0) {
                purchaseTable.getSelectionModel().select(lastIdx);
                purchaseTable.scrollTo(lastIdx);
                event.consume();
            } else if (event.getCode() == KeyCode.UP && idx > 0) {
                purchaseTable.getSelectionModel().select(idx - 1);
                purchaseTable.scrollTo(idx - 1);
                event.consume();
            } else if (event.getCode() == KeyCode.DOWN && idx < lastIdx) {
                purchaseTable.getSelectionModel().select(idx + 1);
                purchaseTable.scrollTo(idx + 1);
                event.consume();
            }
            // === Ganti Edit ke Ctrl+E (bukan F2 lagi) ===
            else if (event.isControlDown() && event.getCode() == KeyCode.E && idx >= 0) {
                editPurchase();
                event.consume();
            }
            // === DEL untuk cancel ===
            else if (event.getCode() == KeyCode.DELETE && idx >= 0) {
                cancelPurchase();
                event.consume();
            }
        }

        // Ctrl+F: Fokus ke search
        if (event.isControlDown() && event.getCode() == KeyCode.F) {
            searchField.requestFocus();
            searchField.selectAll();
            event.consume();
        }

        // Ctrl+L: Clear search
        if (event.isControlDown() && event.getCode() == KeyCode.L) {
            clearSearch();
            event.consume();
        }

        // Ctrl+R: Refresh list
        if (event.isControlDown() && event.getCode() == KeyCode.R) {
            refreshPurchaseList();
            event.consume();
        }

        // Ctrl+I: Show shortcut info (khusus halaman ini!)
        if (event.isControlDown() && event.getCode() == KeyCode.I) {
            showShortcutInfo();
            event.consume();
        }
    }


    @FXML
    public void showShortcutInfo() {
        String info = """
        Keyboard Shortcut List Purchase

        - Ctrl + F    : Fokus kolom pencarian (search)
        - Ctrl + L    : Bersihkan pencarian
        - Ctrl + R    : Refresh data purchase
        - Ctrl + O    : Fokus ke tabel purchase & pilih baris pertama
        - Ctrl + E    : Edit purchase (fokus di tabel)
        - Ctrl + I    : Tampilkan shortcut keyboard halaman ini
        - Home/End    : Pilih baris pertama/terakhir di tabel
        - Up/Down     : Navigasi baris tabel
        - Delete      : Batalkan/cancel purchase (fokus di tabel)
    """;
        TextArea area = new TextArea(info);
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefColumnCount(34);
        area.setPrefRowCount(12);
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("Daftar Shortcut Keyboard");
        dialog.setHeaderText("Keyboard Shortcut List Purchase");
        dialog.getDialogPane().setContent(area);
        dialog.showAndWait();
    }



    @Override
    public void focusRootBox() {

    }
}
