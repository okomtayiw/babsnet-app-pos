package com.babsnet.posapp.controller;

import com.babsnet.posapp.model.ProductType;
import com.babsnet.posapp.repository.ProductTypeRepository;
import com.babsnet.posapp.util.MessageDialogUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class ProductTypeController {
    @FXML private TextField typeNameField;
    @FXML private TableView<ProductType> tableType;
    @FXML private TableColumn<ProductType, Integer> colId;
    @FXML private TableColumn<ProductType, String> colTypeName;
    @FXML private Button addBtn, updateBtn, deleteBtn, clearBtn;

    private ProductType selectedType = null;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getId()).asObject());
        colTypeName.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getTypeName()));

        tableType.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                selectedType = newSel;
                typeNameField.setText(newSel.getTypeName());
            }
        });

        loadTypeList();
    }

    private void loadTypeList() {
        ObservableList<ProductType> list = FXCollections.observableArrayList(ProductTypeRepository.getAll());
        tableType.setItems(list);
    }

    @FXML
    private void handleAdd() {
        String name = typeNameField.getText().trim();
        if (name.isEmpty()) {
            showAlert("Nama tipe produk tidak boleh kosong!");
            return;
        }
        if (ProductTypeRepository.isTypeNameExist(name)) {
            showAlert("Nama tipe '" + name + "' sudah ada!");
            return;
        }
        ProductType t = new ProductType();
        t.setTypeName(name);
        if (ProductTypeRepository.insert(t)) {
            showAlert("Tipe produk berhasil ditambah!");
            loadTypeList();
            handleClear();
        } else {
            showAlert("Gagal menambah tipe produk.");
        }
    }

    @FXML
    private void handleUpdate() {
        if (selectedType == null) {
            showAlert("Pilih data yang akan diupdate.");
            return;
        }
        String name = typeNameField.getText().trim();
        if (name.isEmpty()) {
            showAlert("Nama tipe produk tidak boleh kosong!");
            return;
        }
        // Cek jika nama sudah ada di id lain
        if (ProductTypeRepository.typeNameExistsForOtherId(name, selectedType.getId())) {
            showAlert("Nama tipe \"" + name + "\" sudah digunakan tipe lain!");
            return;
        }

        selectedType.setTypeName(name);
        if (ProductTypeRepository.update(selectedType)) {
            showAlert("Update berhasil.");
            loadTypeList();
            handleClear();
        } else {
            showAlert("Gagal update.");
        }
    }

    @FXML
    private void handleDelete() {
        if (selectedType == null) {
            showAlert("Pilih data yang akan dihapus.");
            return;
        }
        if (ProductTypeRepository.isProductTypeUsed(selectedType.getId())) {
            MessageDialogUtil.showWarning("Tipe produk ini masih digunakan pada produk. Tidak dapat dihapus!");
            return;
        }
        if (ProductTypeRepository.delete(selectedType.getId())) {
            showAlert("Data berhasil dihapus.");
            loadTypeList();
            handleClear();
        } else {
            showAlert("Gagal hapus data.");
        }
    }

    @FXML
    private void handleClear() {
        typeNameField.clear();
        tableType.getSelectionModel().clearSelection();
        selectedType = null;
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.showAndWait();
    }
}
