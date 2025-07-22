package com.babsnet.posapp.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.function.Consumer;

public class SceneLoader {

    public static void loadScene(String fxmlPath, Consumer<Object> controllerConsumer) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneLoader.class.getResource(fxmlPath));
            Parent root = loader.load();

            // Ambil controller & pass ke caller
            Object controller = loader.getController();
            controllerConsumer.accept(controller);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Edit Purchase");
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
