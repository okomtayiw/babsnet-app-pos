package com.babsnet.posapp.controller;


import com.babsnet.posapp.model.UnitProduct;
import com.babsnet.posapp.repository.UnitProductRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class UnitProductController {
    @FXML private TableView<UnitProduct> tableUnit;
    @FXML private TableColumn<UnitProduct, Integer> colId;
    @FXML private TableColumn<UnitProduct, String> colName, colAbbr;
    @FXML private TextField nameField, abbrField;
    @FXML private Button addBtn, updateBtn, deleteBtn, clearBtn;

    private final UnitProductRepository repo = new UnitProductRepository();
    private UnitProduct selectedUnit;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getId()).asObject());
        colName.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getName()));
        colAbbr.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getAbbreviation()));

        tableUnit.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                selectedUnit = newSel;
                nameField.setText(newSel.getName());
                abbrField.setText(newSel.getAbbreviation());
            }
        });

        loadUnits();
    }

    private void loadUnits() {
        ObservableList<UnitProduct> list = repo.getAllUnits();
        tableUnit.setItems(list);
    }

    @FXML
    private void handleAdd() {
        if (nameField.getText().isEmpty()) {
            showAlert("Name tidak boleh kosong!");
            return;
        }
        // Cek jika nama sudah ada
        if (repo.unitNameExists(nameField.getText())) {
            showAlert("Nama unit sudah digunakan. Masukkan nama lain!");
            return;
        }
        UnitProduct unit = new UnitProduct();
        unit.setName(nameField.getText());
        unit.setAbbreviation(abbrField.getText());
        if (repo.insertUnit(unit)) {
            showAlert("Unit berhasil ditambahkan!");
            loadUnits();
            handleClear();
        } else {
            showAlert("Gagal tambah unit!");
        }
    }

    @FXML
    private void handleUpdate() {
        if (selectedUnit == null) {
            showAlert("Pilih unit yang akan diupdate.");
            return;
        }
        // Cek apakah nama baru sudah digunakan di unit lain
        if (repo.unitNameExistsForOtherId(nameField.getText(), selectedUnit.getId())) {
            showAlert("Nama unit sudah digunakan unit lain! Silakan masukkan nama lain.");
            return;
        }
        selectedUnit.setName(nameField.getText());
        selectedUnit.setAbbreviation(abbrField.getText());
        if (repo.updateUnit(selectedUnit)) {
            showAlert("Unit berhasil diupdate!");
            loadUnits();
            handleClear();
        } else {
            showAlert("Gagal update unit!");
        }
    }

    @FXML
    private void handleDelete() {
        if (selectedUnit == null) {
            showAlert("Pilih unit yang akan dihapus.");
            return;
        }
        if (repo.isUnitUsed(selectedUnit.getName())) {
            showAlert("Unit sedang digunakan di produk lain. Tidak bisa dihapus!");
            return;
        }
        if (repo.deleteUnit(selectedUnit.getId())) {
            showAlert("Unit berhasil dihapus!");
            loadUnits();
            handleClear();
        } else {
            showAlert("Gagal hapus unit!");
        }
    }

    @FXML
    private void handleClear() {
        selectedUnit = null;
        nameField.clear();
        abbrField.clear();
        tableUnit.getSelectionModel().clearSelection();
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.showAndWait();
    }
}

