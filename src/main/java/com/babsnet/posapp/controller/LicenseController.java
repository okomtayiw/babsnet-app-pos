package com.babsnet.posapp.controller;

import com.babsnet.posapp.licensing.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;

public class LicenseController {

    @FXML private TextField txtHardwareId;
    @FXML private Label lblStatus;

    public void initialize() {
        txtHardwareId.setText(HardwareFingerprint.getHardwareId());
        updateStatus(LicenseGate.check());
    }

    public void initStatus(LicenseStatus status) { updateStatus(status); }

    private void updateStatus(LicenseStatus st) {
        String msg = switch (st) {
            case MISSING   -> "License belum terpasang.";
            case EXPIRED   -> "License sudah kadaluarsa.";
            case MISMATCH  -> "License bukan untuk PC ini (hardware mismatch).";
            case READ_ERROR-> "Gagal membaca license.";
            case VALID     -> "License valid.";
        };
        lblStatus.setText(msg);
    }

    @FXML
    private void onCopyHw() {
        var cb = javafx.scene.input.Clipboard.getSystemClipboard();
        var cc = new javafx.scene.input.ClipboardContent();
        cc.putString(txtHardwareId.getText());
        cb.setContent(cc);
    }

    @FXML
    private void onImportLicense() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("License (*.lic)", "*.lic"));
        File file = fc.showOpenDialog(txtHardwareId.getScene().getWindow());
        if (file == null) return;

        try {
            LicenseVerifier.installLicense(Path.of(file.getAbsolutePath()));
            LicenseStatus st = LicenseGate.check();
            updateStatus(st);
            if (st == LicenseStatus.VALID) {
                new Alert(Alert.AlertType.INFORMATION, "License valid. Silakan login.").showAndWait();
                ((Stage) txtHardwareId.getScene().getWindow()).close();
                com.babsnet.posapp.util.SceneSwitcher.showLoginStage();
            } else {
                new Alert(Alert.AlertType.ERROR, "License masih belum valid.").showAndWait();
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Gagal pasang license: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onExit() {
        ((Stage) txtHardwareId.getScene().getWindow()).close();
    }
}
