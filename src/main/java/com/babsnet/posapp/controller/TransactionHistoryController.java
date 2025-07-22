package com.babsnet.posapp.controller;

import com.babsnet.posapp.model.Product;
import com.babsnet.posapp.model.Transaction;
import com.babsnet.posapp.model.TransactionDetail;
import com.babsnet.posapp.repository.TransactionRepository;
import com.babsnet.posapp.util.DateUtil;
import com.babsnet.posapp.util.FocusablePage;
import com.babsnet.posapp.util.FormatUtil;
import com.babsnet.posapp.util.MessageDialogUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

public class TransactionHistoryController implements FocusablePage {

    @FXML private TableView<Transaction> transactionTable;
    @FXML private TableColumn<Transaction, Integer> colID;
    @FXML private TableColumn<Transaction, String> colNumber;
    @FXML private TableColumn<Transaction, String> colDate;
    @FXML private TableColumn<Transaction, Double> colTotal;
    @FXML private TableColumn<Transaction, String> colStatus;
    @FXML private TableColumn<Transaction, Void> colAction;

    @FXML private TableView<TransactionDetail> transactionDetailTable;
    @FXML private TableColumn<TransactionDetail, Integer> colIDDetail;
    @FXML private TableColumn<TransactionDetail, String> colProductName;
    @FXML private TableColumn<TransactionDetail, Integer> colQty;
    @FXML private TableColumn<TransactionDetail, Double> colPrice;
    @FXML private TableColumn<TransactionDetail, Double> colSubtotal;
    @FXML private TableColumn<TransactionDetail, Void> colDetailAction;

    @FXML
    private VBox rootBoxHistory;

    @FXML
    private Button refreshButton;


    private final ObservableList<Transaction> transactionList = FXCollections.observableArrayList();
    private final ObservableList<TransactionDetail> detailList = FXCollections.observableArrayList();

    private Transaction selectedTransaction = null;
    private final EventHandler<KeyEvent> shortcutHandler = this::handleKeyEvents;
    private final TransactionRepository transactionRepository = new TransactionRepository();
    @FXML private TextField searchField;
    private final FilteredList<Transaction> filteredTransactionList = new FilteredList<>(transactionList, p -> true);

    public void initialize() {

        rootBoxHistory.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null) oldScene.removeEventFilter(KeyEvent.KEY_PRESSED, shortcutHandler);
            if (newScene != null) newScene.addEventFilter(KeyEvent.KEY_PRESSED, shortcutHandler);
        });



        // Gunakan filtered list untuk TableView
        transactionTable.setItems(filteredTransactionList);

        // Listener pencarian
        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            filteredTransactionList.setPredicate(transaction -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lower = newValue.toLowerCase();
                return transaction.getTransactionNumber().toLowerCase().contains(lower);
            });
        });

        // Init transaction columns
        colID.setCellValueFactory(cellData -> new SimpleObjectProperty<>(null));
        colID.setCellFactory(col -> new TableCell<Transaction, Integer>() {
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
        colNumber.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTransactionNumber()));
        colDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTransDate()));
        colDate.setCellFactory(col -> new TableCell<Transaction, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(DateUtil.formatIsoToNice(item));
                }
            }
        });
        colTotal.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getTotal()).asObject());
        colTotal.setCellFactory(tc -> new TableCell<Transaction, Double>() {
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
        transactionTable.setItems(filteredTransactionList);

        // Action col: edit/cancel
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button cancelBtn = new Button("Cancel");
            {
                cancelBtn.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; -fx-font-size:10;");
                cancelBtn.setOnAction(e -> {
                    Transaction trx = getTableView().getItems().get(getIndex());
                    handleCancel(trx);
                });
            }
            private final HBox box = new HBox(5, cancelBtn);
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        // Table detail
        colIDDetail.setCellValueFactory(cellData -> new SimpleObjectProperty<>(null));
        colIDDetail.setCellFactory(col -> new TableCell<TransactionDetail, Integer>() {
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
        colProductName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProductName()));
        colQty.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getQty()).asObject());
        colPrice.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getPrice()).asObject());
        colPrice.setCellFactory(tc -> new TableCell<TransactionDetail, Double>() {
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
        colSubtotal.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getSubtotal()).asObject());
        colSubtotal.setCellFactory(tc -> new TableCell<TransactionDetail, Double>() {
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
        transactionDetailTable.setItems(detailList);

        colDetailAction.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox hbox = new HBox(5, editBtn, deleteBtn);

            {
                editBtn.setMinWidth(45);
                editBtn.setPrefWidth(50);
                editBtn.setStyle("-fx-font-size: 10; -fx-padding: 2 5 2 5;");
                editBtn.setOnAction(e -> {
                    TransactionDetail detail = getTableView().getItems().get(getIndex());
                    handleEditDetail(detail);
                });

                deleteBtn.setMinWidth(45);
                deleteBtn.setPrefWidth(50);
                deleteBtn.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; -fx-font-size:10; -fx-padding: 2 5 2 5;");
                deleteBtn.setOnAction(e -> {
                    TransactionDetail detail = getTableView().getItems().get(getIndex());
                    handleDeleteDetail(detail);
                });

                hbox.setStyle("-fx-alignment: center;");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : hbox);
            }
        });



        // Klik transaksi = load detail
        transactionTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                selectedTransaction = newSel;
                loadTransactionDetails(newSel.getId());
            } else {
                detailList.clear();
            }
        });

        loadTransactions();
    }



    private void handleDeleteDetail(TransactionDetail detail) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Hapus product \"" + detail.getProductName() + "\" dari transaksi?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait();
        if (confirm.getResult() == ButtonType.YES) {
            Transaction transaction = transactionRepository.getTransactionById(detail.getTransactionId());
            if (!"ACTIVE".equalsIgnoreCase(transaction.getStatus())) {
                MessageDialogUtil.showInfo("Status transaction is canceled can not delete");
            } else {
                try {
                    List<TransactionDetail> transactionDetails = TransactionRepository.getTransactionDetails(transaction.getId());
                    if (transactionDetails.size() > 1) {
                        TransactionRepository.deleteTransactionDetail(detail.getId());
                        // Hapus hanya 1 data dari TableView
                        detailList.remove(detail);

                        // Update objek transaction di list dan TableView utama
                        Transaction updated = transactionRepository.getTransactionById(detail.getTransactionId());
                        // Ganti value total di ObservableList transactionList (supaya TableView utama juga update kolom total)
                        for (int i = 0; i < transactionList.size(); i++) {
                            if (transactionList.get(i).getId() == updated.getId()) {
                                transactionList.set(i, updated); // update hanya satu baris!
                                break;
                            }
                        }

                        showAlert(Alert.AlertType.INFORMATION, "Success", "Product dihapus dari transaksi.");
                    } else {
                        MessageDialogUtil.showWarning("Data product can not empty!");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Error", "Gagal menghapus product.");
                }
            }
        }
    }



    private void loadTransactions() {
        transactionList.setAll(TransactionRepository.getAllTransactions());
        transactionTable.getSelectionModel().clearSelection();
        detailList.clear();
    }

    private void loadTransactionDetails(int transactionId) {
        detailList.setAll(TransactionRepository.getTransactionDetails(transactionId));
    }

    private void handleCancel(Transaction trx) {
        if (!"ACTIVE".equalsIgnoreCase(trx.getStatus())) {
            showAlert(Alert.AlertType.INFORMATION, "Transaction", "Transaksi sudah dibatalkan.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Cancel this transaction?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait();
        if (confirm.getResult() == ButtonType.YES) {
            try {
                TransactionRepository.cancelTransaction(trx.getId());
                showAlert(Alert.AlertType.INFORMATION, "Success", "Transaction cancelled.");
                loadTransactions();
            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Error", "Gagal membatalkan transaksi.");
            }
        }
    }

    private void handleEditDetail(TransactionDetail detail) {
        TextInputDialog qtyDialog = new TextInputDialog(String.valueOf(detail.getQty()));
        qtyDialog.setHeaderText("Edit Qty for " + detail.getProductName());
        qtyDialog.setContentText("Qty:");
        qtyDialog.showAndWait().ifPresent(newQtyStr -> {
            try {
                int newQty = Integer.parseInt(newQtyStr);
                double subtotal = newQty * detail.getPrice();
                TransactionRepository.updateTransactionDetail(detail.getId(), newQty, detail.getPrice(), subtotal);

                // Update 1 baris detail di ObservableList
                detail.setQty(newQty);
                detail.setSubtotal(subtotal);
                transactionDetailTable.refresh(); // cukup panggil refresh!

                // Update total transaksi di TableView utama
                Transaction updated = transactionRepository.getTransactionById(detail.getTransactionId());
                for (int i = 0; i < transactionList.size(); i++) {
                    if (transactionList.get(i).getId() == updated.getId()) {
                        transactionList.set(i, updated); // replace agar TableView utama juga update kolom total
                        break;
                    }
                }

                showAlert(Alert.AlertType.INFORMATION, "Success", "Qty updated.");
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Invalid Qty.");
            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Error", "Update failed.");
            }
        });
    }




    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }


    private void handleKeyEvents(javafx.scene.input.KeyEvent event) {
        // ===================== 1. Navigasi di DETAIL TABLE =====================
        if (transactionDetailTable.isFocused()) {
            int selIdx = transactionDetailTable.getSelectionModel().getSelectedIndex();
            int itemCount = transactionDetailTable.getItems().size();

            // Edit (E)
            if (selIdx >= 0 && event.getCode() == KeyCode.E) {
                Platform.runLater(() -> handleEditDetail(detailList.get(selIdx)));
                event.consume();
                return;
            }
            // Delete (Del)
            if (selIdx >= 0 && event.getCode() == KeyCode.DELETE) {
                Platform.runLater(() -> handleDeleteDetail(detailList.get(selIdx)));
                event.consume();
                return;
            }
            // Navigasi UP
            if (event.getCode() == KeyCode.UP && selIdx > 0) {
                Platform.runLater(() -> {
                    transactionDetailTable.getSelectionModel().select(selIdx - 1);
                    transactionDetailTable.scrollTo(selIdx - 1);
                });
                event.consume();
                return;
            }
            // Navigasi DOWN
            if (event.getCode() == KeyCode.DOWN && selIdx < itemCount - 1) {
                Platform.runLater(() -> {
                    transactionDetailTable.getSelectionModel().select(selIdx + 1);
                    transactionDetailTable.scrollTo(selIdx + 1);
                });
                event.consume();
                return;
            }
            // (Opsional) Kalau di baris terakhir lalu DOWN, balik ke table transaksi:
            // if (event.getCode() == KeyCode.DOWN && selIdx == itemCount - 1) {
            //     Platform.runLater(() -> {
            //         transactionTable.requestFocus();
            //         transactionTable.getSelectionModel().select(0);
            //         transactionTable.scrollTo(0);
            //     });
            //     event.consume();
            //     return;
            // }
            // Handler detail table lain tambahkan di sini...
        }

        // ===================== 2. Handler Global dan TRANSACTION TABLE =====================
        // Fokus ke Search CTRL+F
        if (event.isControlDown() && event.getCode() == KeyCode.F) {
            Platform.runLater(() -> {
                searchField.requestFocus();
                searchField.selectAll();
            });
            event.consume();
            return;
        }

        // HOME = row pertama transactionTable
        if (event.getCode() == KeyCode.HOME) {
            if (!filteredTransactionList.isEmpty()) {
                Platform.runLater(() -> {
                    transactionTable.requestFocus();
                    transactionTable.getSelectionModel().select(0);
                    transactionTable.scrollTo(0);
                });
            }
            event.consume();
            return;
        }

        // END = row terakhir transactionTable
        if (event.getCode() == KeyCode.END) {
            int lastIdx = filteredTransactionList.size() - 1;
            if (lastIdx >= 0) {
                Platform.runLater(() -> {
                    transactionTable.requestFocus();
                    transactionTable.getSelectionModel().select(lastIdx);
                    transactionTable.scrollTo(lastIdx);
                });
            }
            event.consume();
            return;
        }

        // UP di tabel transaksi
        if (transactionTable.isFocused() && event.getCode() == KeyCode.UP) {
            int idx = transactionTable.getSelectionModel().getSelectedIndex();
            if (idx > 0) {
                Platform.runLater(() -> {
                    transactionTable.getSelectionModel().select(idx - 1);
                    transactionTable.scrollTo(idx - 1);
                });
            }
            event.consume();
            return;
        }

        // DOWN di tabel transaksi
        if (transactionTable.isFocused() && event.getCode() == KeyCode.DOWN) {
            int idx = transactionTable.getSelectionModel().getSelectedIndex();
            if (idx < filteredTransactionList.size() - 1) {
                Platform.runLater(() -> {
                    transactionTable.getSelectionModel().select(idx + 1);
                    transactionTable.scrollTo(idx + 1);
                });
            }
            event.consume();
            return;
        }

        // CTRL+T: Fokus ke detail table dari transaction table
        if (event.isControlDown() && event.getCode() == KeyCode.T) {
            if (transactionTable.isFocused() && !detailList.isEmpty()) {
                Platform.runLater(() -> {
                    transactionDetailTable.requestFocus();
                    transactionDetailTable.getSelectionModel().select(0);
                    transactionDetailTable.scrollTo(0);
                });
            }
            event.consume();
            return;
        }

        // CTRL+R: Balik ke transactionTable dari detail
        if (event.isControlDown() && event.getCode() == KeyCode.R) {
            if (transactionDetailTable.isFocused() && !filteredTransactionList.isEmpty()) {
                Platform.runLater(() -> {
                    transactionTable.requestFocus();
                    int selectedIdx = transactionTable.getSelectionModel().getSelectedIndex();
                    if (selectedIdx < 0) selectedIdx = 0;
                    transactionTable.getSelectionModel().select(selectedIdx);
                    transactionTable.scrollTo(selectedIdx);
                });
            }
            event.consume();
            return;
        }

        // Cancel transaksi (C) di tabel utama
        if (transactionTable.isFocused() && event.getCode() == KeyCode.C) {
            int selIdx = transactionTable.getSelectionModel().getSelectedIndex();
            if (selIdx >= 0) {
                Platform.runLater(() -> handleCancel(filteredTransactionList.get(selIdx)));
            }
            event.consume();
            return;
        }

        // Refresh
        if (event.isControlDown() && event.getCode() == KeyCode.L) {
            Platform.runLater(this::loadTransactions);
            event.consume();
            return;
        }


        // Show shortcut info
        if (event.getCode() == KeyCode.I) {
            showShortcutInfo();
            event.consume();
            return;
        }
    }


    @FXML
    public void showShortcutInfo() {
        String info = """
        **Keyboard Shortcut Transaction History**
        
        - Ctrl + F  : Fokus kolom pencarian
        - Home      : Pilih transaksi pertama
        - End       : Pilih transaksi terakhir
        - Up/Down   : Navigasi transaksi
        - Ctrl + T  : Fokus ke detail transaksi
        - Ctrl + R  : Balik fokus ke tabel transaksi
        - C         : Batalkan transaksi (fokus di transaksi utama)
        - F5        : Refresh data transaksi
        - E         : Edit qty produk di detail transaksi (fokus di detail)
        - Del       : Hapus produk di detail transaksi (fokus di detail)
        """;

        TextArea area = new TextArea(info);
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefColumnCount(38);
        area.setPrefRowCount(14);

        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("Daftar Shortcut Keyboard");
        dialog.setHeaderText("Keyboard Shortcut Transaction History");
        dialog.getDialogPane().setContent(area);
        dialog.showAndWait();
    }

    @Override
    public void focusRootBox() {
        Platform.runLater(() -> rootBoxHistory.requestFocus());
    }

    public void refreshTransactions(ActionEvent actionEvent) {
        loadTransactions();
    }
}
