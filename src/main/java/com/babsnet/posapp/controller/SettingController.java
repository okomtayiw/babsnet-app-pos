package com.babsnet.posapp.controller;

import com.babsnet.posapp.util.ConfigUtil;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.print.Printer;
import javafx.print.PrinterJob;

import java.sql.Connection;
import java.sql.DriverManager;

public class SettingController {
    @FXML private TextField dbUrlField, dbUserField;
    @FXML private Label dbStatusLabel;
    @FXML private Button testDbButton;
    @FXML private ComboBox<String> printerComboBox;
    @FXML private Label printerStatusLabel;
    @FXML private Button testPrinterButton;

    private String dbUrl, dbUser, dbPass;

    @FXML
    public void initialize() {
        // Load DB config
        dbUrl  = ConfigUtil.get("db.url");
        dbUser = ConfigUtil.get("db.user");
        dbPass = ConfigUtil.get("db.pass");

        dbUrlField.setText(dbUrl);
        dbUserField.setText(dbUser);
        dbStatusLabel.setText("Unknown");

        // List all printers
        printerComboBox.setItems(FXCollections.observableArrayList(
                Printer.getAllPrinters().stream().map(Printer::getName).toList()
        ));
        if (!printerComboBox.getItems().isEmpty())
            printerComboBox.getSelectionModel().selectFirst();
        printerStatusLabel.setText("Unknown");
    }

    @FXML
    private void handleTestDb() {
        dbStatusLabel.setText("Testing...");
        try {
            Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
            dbStatusLabel.setText("Connected ✔");
            dbStatusLabel.setStyle("-fx-text-fill: green;");
            conn.close();
        } catch (Exception ex) {
            dbStatusLabel.setText("Failed: " + ex.getMessage());
            dbStatusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    private void handleTestPrinter() {
        String printerName = printerComboBox.getValue();
        if (printerName == null) {
            printerStatusLabel.setText("No printer selected!");
            printerStatusLabel.setStyle("-fx-text-fill: red;");
            return;
        }
        Printer printer = Printer.getAllPrinters().stream()
                .filter(p -> p.getName().equals(printerName))
                .findFirst().orElse(null);

        if (printer == null) {
            printerStatusLabel.setText("Not found");
            printerStatusLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        PrinterJob job = PrinterJob.createPrinterJob(printer);
        if (job != null) {
            boolean success = job.printPage(new Label("Test Print from Babsnet POS"));
            if (success) {
                job.endJob();
                printerStatusLabel.setText("Success ✔");
                printerStatusLabel.setStyle("-fx-text-fill: green;");
            } else {
                printerStatusLabel.setText("Failed to print");
                printerStatusLabel.setStyle("-fx-text-fill: red;");
            }
        } else {
            printerStatusLabel.setText("Failed to create print job");
            printerStatusLabel.setStyle("-fx-text-fill: red;");
        }
    }
}
