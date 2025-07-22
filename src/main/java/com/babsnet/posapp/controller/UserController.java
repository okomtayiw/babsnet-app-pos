package com.babsnet.posapp.controller;

import com.babsnet.posapp.model.User;
import com.babsnet.posapp.repository.UserRepository;
import com.babsnet.posapp.util.MessageDialogUtil;
import com.babsnet.posapp.util.PasswordUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class UserController {
    @FXML private TextField usernameField, firstNameField, lastNameField, phoneField, emailField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> colId, colUsername, colName, colPhone, colEmail, colRole;

    private final UserRepository userRepo = new UserRepository();
    private User selectedUser = null;

    @FXML
    public void initialize() {
        roleComboBox.setItems(FXCollections.observableArrayList("ADMIN", "KASIR", "OWNER"));
        loadUserTable();

        colId.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getId())));
        colUsername.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUsername()));
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFirstName() + " " + data.getValue().getLastName()));
        colPhone.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPhoneNumber()));
        colEmail.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmail()));
        colRole.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRole()));

        userTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) showUserDetail(newSel);
        });
    }

    private void loadUserTable() {
        userTable.setItems(FXCollections.observableArrayList(userRepo.findAll()));
    }

    private void showUserDetail(User u) {
        selectedUser = u;
        usernameField.setText(u.getUsername());
        firstNameField.setText(u.getFirstName());
        lastNameField.setText(u.getLastName());
        phoneField.setText(u.getPhoneNumber());
        emailField.setText(u.getEmail());
        roleComboBox.setValue(u.getRole());
        passwordField.clear(); // demi keamanan, tidak pernah tampilkan hash ke field
    }

    @FXML
    private void handleNew() {
        selectedUser = null;
        clearForm();
    }

    @FXML
    private void handleSave() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();
        String role = roleComboBox.getValue();

        if (username.isEmpty() || role == null) {
            MessageDialogUtil.showWarning("Username & role wajib diisi!");
            return;
        }

        // Jika new user, password wajib diisi
        if (selectedUser == null && password.isEmpty()) {
            MessageDialogUtil.showWarning("Password wajib diisi untuk user baru!");
            return;
        }

        User user = selectedUser == null ? new User() : selectedUser;
        user.setUsername(username);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhoneNumber(phone);
        user.setEmail(email);
        user.setRole(role);

        // Kalau password diisi, hash dan update
        if (!password.isEmpty()) {
            user.setPassword(PasswordUtil.hashPassword(password));
        } else if (selectedUser == null) {
            // User baru harus ada password
            MessageDialogUtil.showWarning("Password wajib diisi untuk user baru!");
            return;
        }

        boolean success;
        if (selectedUser == null) {
            success = userRepo.insert(user);
        } else {
            success = userRepo.update(user);
        }

        if (success) {
            MessageDialogUtil.showInfo("User berhasil disimpan!");
            clearForm();
            loadUserTable();
        } else {
            MessageDialogUtil.showError("Gagal simpan user. Username mungkin sudah digunakan.");
        }
    }

    @FXML
    private void handleDelete() {
        User user = userTable.getSelectionModel().getSelectedItem();
        if (user == null) {
            MessageDialogUtil.showWarning("Pilih user lebih dulu.");
            return;
        }
        if (!MessageDialogUtil.showConfirm("Yakin hapus user?")) return;
        if (userRepo.delete(user.getId())) {
            MessageDialogUtil.showInfo("User dihapus.");
            clearForm();
            loadUserTable();
        } else {
            MessageDialogUtil.showError("Gagal hapus user.");
        }
    }

    @FXML
    private void handleClear() {
        clearForm();
    }

    private void clearForm() {
        selectedUser = null;
        usernameField.clear();
        passwordField.clear();
        firstNameField.clear();
        lastNameField.clear();
        phoneField.clear();
        emailField.clear();
        roleComboBox.getSelectionModel().clearSelection();
        userTable.getSelectionModel().clearSelection();
    }
}
