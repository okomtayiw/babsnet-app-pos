package com.babsnet.posapp.util;

import javafx.scene.image.Image;
import javafx.stage.Stage;
import java.io.InputStream;

public class IconUtil {
    private static Image appIcon = null;

    public static void setAppIcon(Stage stage) {
        if (appIcon == null) {
            try (InputStream iconStream = IconUtil.class.getResourceAsStream("/icon_babs.ico")) {
                if (iconStream != null) {
                    appIcon = new Image(iconStream);
                } else {
                    System.err.println("Resource icon_babs.ico tidak ditemukan di classpath.");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (appIcon != null) {
            stage.getIcons().add(appIcon);
        }
    }
}
