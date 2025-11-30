package com.babsnet.posapp.util;

import com.babsnet.posapp.licensing.LicenseStatus;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.function.Consumer;

public class SceneSwitcher {

    public static void switchScene(Node sourceNode, String fxmlPath, Consumer<Object> controllerSetup) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneSwitcher.class.getResource(fxmlPath));
            Scene scene = new Scene(loader.load());

            if (controllerSetup != null) {
                controllerSetup.accept(loader.getController());
            }

            Stage stage = (Stage) sourceNode.getScene().getWindow();
            stage.setScene(scene);
            stage.setMinWidth(800);
            stage.setMinHeight(600);
            stage.setTitle("POS Babsnet");
            KeyShortcutUtil.addCloseOnEsc(stage, scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void switchScene(Node sourceNode, String fxmlPath) {
        switchScene(sourceNode, fxmlPath, null);
    }

    public static void showLoginStage() {
        try {
            FXMLLoader loader = new FXMLLoader(SceneSwitcher.class.getResource("/com/babsnet/posapp/login.fxml"));
            Scene scene = new Scene(loader.load(), 400, 300); // ukuran fix
            Stage stage = new AppStage();
            stage.setTitle("Login POS");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showHomeStage() {
        try {
            FXMLLoader loader = new FXMLLoader(SceneSwitcher.class.getResource("/com/babsnet/posapp/home.fxml"));
            Scene scene = new Scene(loader.load(), 900, 700);
            Stage stage = new AppStage();
            stage.setScene(scene);
            stage.setTitle("POS Babsnet");
            stage.setResizable(true);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showLicenseStage(LicenseStatus status) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneSwitcher.class.getResource("/com/babsnet/posapp/license.fxml"));
            Scene scene = new Scene(loader.load(), 520, 360);
            Stage stage = new AppStage();
            stage.setTitle("Aktivasi Lisensi");
            stage.setScene(scene);
            stage.setResizable(false);

            // opsional: kirim status ke controller
            Object ctrl = loader.getController();
            if (ctrl instanceof com.babsnet.posapp.controller.LicenseController lc) {
                lc.initStatus(status);
            }

            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
