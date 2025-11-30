package com.babsnet.posapp.util;

import com.babsnet.posapp.model.OnlineTransaction;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Files;

public  class ThermalPrintPreviewDialog {

    private ThermalPrintPreviewDialog() {}

    public static void show(Window owner, OnlineTransaction tx) {
        Stage dlg = new Stage();
        if (owner != null) dlg.initOwner(owner);
        dlg.initModality(Modality.WINDOW_MODAL);
        dlg.setTitle("Preview Struk (Thermal)");

        // controls
        TextArea ta = new TextArea();
        ta.setEditable(false);
        ta.setWrapText(false);
        ta.setFont(Font.font("Monospaced", 12));

        ComboBox<Integer> cbCols = new ComboBox<>();
        cbCols.getItems().addAll(32); // 58mm & 80mm
        cbCols.getSelectionModel().selectFirst();

        Button btnPrint = new Button("Cetak (Thermal)");
        Button btnCopy  = new Button("Copy");
        Button btnSave  = new Button("Simpan .txt");
        Button btnClose = new Button("Tutup");

        // layout
        HBox top = new HBox(10, new Label("Kolom:"), cbCols);
        top.setAlignment(Pos.CENTER_LEFT);
        top.setPadding(new Insets(8));

        HBox bottom = new HBox(10, btnPrint, btnCopy, btnSave, btnClose);
        bottom.setAlignment(Pos.CENTER_RIGHT);
        bottom.setPadding(new Insets(8));

        BorderPane root = new BorderPane(ta, top, null, bottom, null);
        root.setPrefSize(520, 480);

        // behaviors
        Runnable refresh = () -> ta.setText(ThermalReceiptFormatter.format(tx, cbCols.getValue()));
        cbCols.valueProperty().addListener((o, old, v) -> refresh.run());
        refresh.run();

        btnPrint.setOnAction(e -> {
            ThermalPrinterUtil.printToThermalPrinter(ta.getText());
        });

        btnCopy.setOnAction(e -> {
            ClipboardContent cc = new ClipboardContent();
            cc.putString(ta.getText());
            Clipboard.getSystemClipboard().setContent(cc);
            MessageDialogUtil.showInfo("Teks struk disalin ke clipboard.");
        });

        btnSave.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Simpan Struk");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text", "*.txt"));
            fc.setInitialFileName("struk.txt");
            File f = fc.showSaveDialog(dlg);
            if (f != null) {
                try {
                    Files.writeString(f.toPath(), ta.getText());
                    MessageDialogUtil.showInfo("Berhasil disimpan: " + f.getAbsolutePath());
                } catch (Exception ex) {
                    MessageDialogUtil.showError("Gagal simpan: " + ex.getMessage());
                }
            }
        });

        btnClose.setOnAction(e -> dlg.close());

        dlg.setScene(new Scene(root));
        dlg.showAndWait();
    }
}

