package com.babsnet.posapp;

import com.babsnet.posapp.licensing.LicenseGate;
import com.babsnet.posapp.licensing.LicenseMonitor;
import com.babsnet.posapp.licensing.LicensePaths;  
import com.babsnet.posapp.licensing.LicenseStatus;
import com.babsnet.posapp.session.SessionManager;
import com.babsnet.posapp.util.ConfigUtil;
import com.babsnet.posapp.util.SceneSwitcher;
import javafx.application.Application;
import javafx.stage.Stage;

import java.time.Duration;
import java.nio.file.Path;

public class Main extends Application {

    private LicenseMonitor licenseMonitor;

    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(Application.STYLESHEET_MODENA);
        stage.hide();

        if (Boolean.parseBoolean(ConfigUtil.get("DEV_ALLOW_NO_LICENSE"))) {
            routeAfterLicense();
            return;
        }

        LicenseStatus status = LicenseGate.check();
        if (status == LicenseStatus.VALID) {
            routeAfterLicense();
        } else {
            SceneSwitcher.showLicenseStage(status);
        }
        if (!Boolean.parseBoolean(ConfigUtil.get("DEV_ALLOW_NO_LICENSE"))) {
            startPeriodicMonitor();
        }
    }

    private void startPeriodicMonitor() {
        long hours = Long.getLong("LICENSE_CHECK_HOURS", 6L);
        licenseMonitor = new LicenseMonitor(Duration.ofHours(hours));
        licenseMonitor.start();

        Path licPath = LicensePaths.defaultLicensePath();
        licenseMonitor.watchFileChangesAsync(licPath);

    }

    private void routeAfterLicense() {
        if (SessionManager.getInstance().isLoggedIn()) {
            SceneSwitcher.showHomeStage();
        } else {
            SceneSwitcher.showLoginStage();
        }
    }

    @Override
    public void stop() {
        if (licenseMonitor != null) licenseMonitor.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
