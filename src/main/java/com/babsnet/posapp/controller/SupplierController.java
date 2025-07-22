package com.babsnet.posapp.controller;

import com.babsnet.posapp.model.Supplier;
import com.babsnet.posapp.repository.SupplierRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class SupplierController {
    @FXML private TextField nameField, addressField, phoneField, emailField;
    @FXML private TextArea notesField;
    @FXML private TableView<Supplier> supplierTable;
    @FXML private TableColumn<Supplier, Integer> colId;
    @FXML private TableColumn<Supplier, String> colName, colAddress, colPhone, colEmail, colNotes;

    private Supplier selectedSupplier = null;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getId()).asObject());
        colName.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getName()));
        colAddress.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getAddress()));
        colPhone.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getPhone()));
        colEmail.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getEmail()));
        colNotes.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getNotes()));

        supplierTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                selectedSupplier = newSel;
                nameField.setText(newSel.getName());
                addressField.setText(newSel.getAddress());
                phoneField.setText(newSel.getPhone());
                emailField.setText(newSel.getEmail());
                notesField.setText(newSel.getNotes());
            }
        });

        loadSuppliers();
    }

    private void loadSuppliers() {
        ObservableList<Supplier> list = FXCollections.observableArrayList(SupplierRepository.getAllSuppliers());
        supplierTable.setItems(list);
    }

    @FXML
    private void handleAdd() {
        Supplier s = new Supplier();
        s.setName(nameField.getText());
        s.setAddress(addressField.getText());
        s.setPhone(phoneField.getText());
        s.setEmail(emailField.getText());
        s.setNotes(notesField.getText());
        if (SupplierRepository.insertSupplier(s)) {
            showAlert("Supplier berhasil ditambahkan!");
            loadSuppliers();
            handleClear();
        } else {
            showAlert("Gagal tambah supplier!");
        }
    }

    @FXML
    private void handleUpdate() {
        if (selectedSupplier == null) {
            showAlert("Pilih supplier yang akan diupdate.");
            return;
        }
        selectedSupplier.setName(nameField.getText());
        selectedSupplier.setAddress(addressField.getText());
        selectedSupplier.setPhone(phoneField.getText());
        selectedSupplier.setEmail(emailField.getText());
        selectedSupplier.setNotes(notesField.getText());
        if (SupplierRepository.updateSupplier(selectedSupplier)) {
            showAlert("Supplier berhasil diupdate!");
            loadSuppliers();
            handleClear();
        } else {
            showAlert("Gagal update supplier!");
        }
    }

    @FXML
    private void handleDelete() {
        if (selectedSupplier == null) {
            showAlert("Pilih supplier yang akan dihapus.");
            return;
        }
        if (SupplierRepository.deleteSupplier(selectedSupplier.getId())) {
            showAlert("Supplier berhasil dihapus!");
            loadSuppliers();
            handleClear();
        } else {
            showAlert("Gagal hapus supplier!");
        }
    }

    @FXML
    private void handleClear() {
        selectedSupplier = null;
        nameField.clear();
        addressField.clear();
        phoneField.clear();
        emailField.clear();
        notesField.clear();
        supplierTable.getSelectionModel().clearSelection();
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.showAndWait();
    }
}
