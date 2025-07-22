package com.babsnet.posapp.controller;

import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class DashboardController {
    @FXML
    private ImageView gifView;

    @FXML
    public void initialize() {
        try {
            Image gifImage = new Image(getClass().getResourceAsStream("/com/babsnet/posapp/images/store_images.png"));
            gifView.setImage(gifImage);
        } catch (Exception e) {
            System.out.println("Gagal load GIF: " + e.getMessage());
        }
    }
}
