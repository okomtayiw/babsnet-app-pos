package com.babsnet.posapp;

import com.babsnet.posapp.session.SessionManager;
import com.babsnet.posapp.util.SceneSwitcher;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(Application.STYLESHEET_MODENA);

        // Cek apakah user sudah login atau belum
        if (SessionManager.getInstance().isLoggedIn()) {
            SceneSwitcher.showHomeStage();
        } else {
            SceneSwitcher.showLoginStage();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
