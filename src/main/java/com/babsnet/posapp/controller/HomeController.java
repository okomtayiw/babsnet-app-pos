package com.babsnet.posapp.controller;

import com.babsnet.posapp.model.User;
import com.babsnet.posapp.session.SessionManager;
import com.babsnet.posapp.util.AppStage;
import com.babsnet.posapp.util.FocusablePage;
import com.babsnet.posapp.util.SceneSwitcher;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;



public class HomeController {

    @FXML
    private StackPane contentContainer;

    private String currentCenterView = "";
    private Stage cashierStage = null;

    @FXML private Button btnUsers;
    @FXML private Button btnSuppliers;

    private boolean globalShortcutAttached = false;



    @FXML
    public void initialize() {

        if (!SessionManager.getInstance().isLoggedIn()) {
            Platform.runLater(() -> {
                Stage stage = (Stage) contentContainer.getScene().getWindow();
                stage.close();
                SceneSwitcher.showLoginStage();
            });
            return;
        }

        // Tampilkan dashboard saat awal
        goToHomeContent();

        // Pasang global shortcut NAVIGASI saja (F1-F7, F12, ESC)
        Platform.runLater(() -> {

            User user = SessionManager.getInstance().getCurrentUser();
            String role = (user != null) ? user.getRole() : "";
            if (!"ADMIN".equalsIgnoreCase(role)) {
                if (btnUsers != null) {
                    btnUsers.setVisible(false);
                    btnUsers.setManaged(false);
                }
            }
            Scene scene = contentContainer.getScene();
            attachGlobalShortcut(scene);
        });


    }

    private void attachGlobalShortcut(Scene scene) {
        if (globalShortcutAttached) return;
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            KeyCode code = event.getCode();

            // F1–F7, F12, ESCAPE: SELALU AKTIF di manapun fokusnya
            switch (code) {
                case F1 -> { goToCashier(); event.consume(); }
                case F2 -> { goToTransactionHistory(); event.consume(); }
                case F3 -> { goToProduct(); event.consume(); }
                case F4 -> { goToPurchase(); event.consume(); }
                case F5 -> { goToPurchaseList(); event.consume(); }
                case F6 -> { goToAdjustStock(); event.consume(); }
                case F7 -> { goToReport(); event.consume(); }
                case F8 -> { goToReportPurchase(); event.consume(); }
                case F12 -> { goToHomeContent(); event.consume(); }
                case ESCAPE -> { logout(); event.consume(); }
                default -> {
                    // Untuk shortcut lain, jika sedang fokus di TextInputControl, abaikan!
                    if (event.getTarget() instanceof javafx.scene.control.TextInputControl) {
                        return;
                    }
                    // ... (opsional: tambahkan shortcut global lain di sini)
                }
            }
        });
        globalShortcutAttached = true;
    }


    /**
     * Ganti konten tengah (center) tanpa ganggu shortcut global.
     * Fokus halaman baru jika perlu (opsional pakai FocusablePage interface).
     */
    private void setCenterContent(String fxmlPath) {
        if (currentCenterView.equals(fxmlPath)) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node content = loader.load();

            contentContainer.getChildren().setAll(content);
            currentCenterView = fxmlPath;

            // Jika halaman implement FocusablePage, auto focus
            Object controller = loader.getController();
            if (controller instanceof FocusablePage page) {
                Platform.runLater(page::focusRootBox);
            }
        } catch (Exception e) {
            showAlert("Gagal membuka halaman: " + fxmlPath);
            e.printStackTrace();
        }
    }

    @FXML
    public void goToCashier() {
        try {
            if (cashierStage != null && cashierStage.isShowing()) {
                cashierStage.toFront();
                return;
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/babsnet/posapp/cashier.fxml"));
            Scene scene = new Scene(loader.load());

            cashierStage = new AppStage();
            cashierStage.setTitle("Kasir");
            cashierStage.setScene(scene);
            cashierStage.setResizable(true);
            cashierStage.setOnCloseRequest(event -> cashierStage = null);
            cashierStage.show();

        } catch (Exception e) {
            showAlert("Gagal membuka halaman Kasir");
            e.printStackTrace();
        }
    }

    // Navigasi Sidebar/Menu
    @FXML public void goToHome() { goToHomeContent(); }
    @FXML public void goToSetting() { goToSettingContent(); }
    @FXML public void goToUsers() { setCenterContent("/com/babsnet/posapp/UserView.fxml"); }
    @FXML public void goToSuppliers() { setCenterContent("/com/babsnet/posapp/supplier_page.fxml");}
    @FXML public void goToTransactionHistory() { setCenterContent("/com/babsnet/posapp/transactionhistory.fxml"); }
    @FXML public void goToProduct() { setCenterContent("/com/babsnet/posapp/product.fxml"); }
    @FXML public void goToPurchase() { setCenterContent("/com/babsnet/posapp/purchase.fxml"); }
    @FXML public void goToPurchaseList() { setCenterContent("/com/babsnet/posapp/purchaselist.fxml"); }
    @FXML public void goToAdjustStock() { setCenterContent("/com/babsnet/posapp/stockadjustment.fxml"); }
    @FXML public void goToReport() { setCenterContent("/com/babsnet/posapp/TransactionReportView.fxml"); }
    @FXML public void goToReportPurchase() { setCenterContent("/com/babsnet/posapp/purchase_report.fxml"); }

    // Untuk isi dashboard/home (default isi tengah)
    public void goToHomeContent() {
        setCenterContent("/com/babsnet/posapp/dashboard.fxml");
    }

    public void goToSettingContent() {
        setCenterContent("/com/babsnet/posapp/setting_page.fxml");
    }

    // Logout (esc)
    @FXML
    public void logout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Logout Confirmation");
        confirm.setHeaderText("Logout dari aplikasi?");
        confirm.setContentText("Anda yakin ingin logout?");

        ButtonType yesBtn = new ButtonType("Yes", ButtonBar.ButtonData.OK_DONE);
        ButtonType noBtn = new ButtonType("No", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(yesBtn, noBtn);

        confirm.showAndWait().ifPresent(result -> {
            if (result == yesBtn) {
                SessionManager.getInstance().logout();
                Stage currentStage = (Stage) contentContainer.getScene().getWindow();
                currentStage.close();
                SceneSwitcher.showLoginStage();
            }
        });
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }


}
