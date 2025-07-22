package com.babsnet.posapp.util;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

public class KeyShortcutUtil {
    /**
     * Adds ESC and F4 key shortcut to close the stage (window).
     * Call this in controller after you get reference to scene & stage.
     */
    public static void addCloseOnEsc(Stage stage, Scene scene) {
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE || event.getCode() == KeyCode.F4) {
                stage.close();
            }
        });
    }

    // (Bonus) For Platform.exit() if you want to close all app windows:
    public static void addExitOnEsc(Scene scene) {
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE || event.getCode() == KeyCode.F4) {
                javafx.application.Platform.exit();
            }
        });
    }
}
