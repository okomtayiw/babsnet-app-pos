package com.babsnet.posapp.controller;

import com.babsnet.posapp.model.User;
import com.babsnet.posapp.repository.UserRepository;
import com.babsnet.posapp.session.SessionManager;
import com.babsnet.posapp.util.AppStage;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    private final UserRepository userRepo = new UserRepository();

    @FXML
    public void initialize() {
        usernameField.setOnAction(e -> passwordField.requestFocus());
        passwordField.setOnAction(e -> handleLogin());
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        User user = userRepo.validateUser(username, password);

        if (user != null) {
            // Simpan sesi terlebih dahulu
            SessionManager.getInstance().login(user);

            // Tutup window login
            Stage loginStage = (Stage) usernameField.getScene().getWindow();
            loginStage.close();

            // Buka window home
            openHomeWindow();

        } else {
            clearForm();
            showAlert("Username/password salah!");
        }
    }

    private void openHomeWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/babsnet/posapp/home.fxml"));
            Scene scene = new Scene(loader.load(), 900, 700);
            Stage homeStage = new AppStage();
            homeStage.setScene(scene);
            homeStage.setTitle("POS Babsnet");
            homeStage.setResizable(true);
            homeStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Terjadi kesalahan saat membuka halaman utama.");
        }
    }

    private void clearForm(){
        usernameField.clear();
        passwordField.clear();
        usernameField.requestFocus();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Info");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
