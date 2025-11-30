package com.babsnet.posapp.util;

import com.babsnet.posapp.model.Product;
import com.babsnet.posapp.repository.ProductRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.List;
import java.util.Optional;

public class ProductPickerDialog {

    private ProductPickerDialog() {}

    public static Optional<Product> showAndPick(Window owner) {
        // data
        List<Product> all = ProductRepository.loadProducts("%");
        ObservableList<Product> master = FXCollections.observableArrayList(all);
        FilteredList<Product> filtered = new FilteredList<>(master, p -> true);
        SortedList<Product> sorted = new SortedList<>(filtered);

        // UI
        TextField search = new TextField();
        search.setPromptText("Cari nama / barcode (Ctrl+F)");
        Button btnOk = new Button("Pilih");
        Button btnCancel = new Button("Batal");

        TableView<Product> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("Tidak ada data"));

        TableColumn<Product, String> cName = new TableColumn<>("Name");
        cName.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getName()));

        TableColumn<Product, String> cBarcode = new TableColumn<>("Barcode");
        cBarcode.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getBarcode()));

        TableColumn<Product, Number> cStock = new TableColumn<>("Stock");
        cStock.setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(c.getValue().getStock()));

        TableColumn<Product, String> cPrice = new TableColumn<>("Harga Beli");
        cPrice.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(
                FormatUtil.toRupiahNoDecimal(c.getValue().getLastBuyPrice())
        ));

        TableColumn<Product, String> cLastBuy = new TableColumn<>("Harga Jual");
        cLastBuy.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(
                (FormatUtil.toRupiahNoDecimal(c.getValue().getPrice()))
        ));

        TableColumn<Product, String> cType = new TableColumn<>("Type");
        cType.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getTypeName()));

        TableColumn<Product, String> cUnit = new TableColumn<>("Unit");
        cUnit.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getUnitName()));

        table.getColumns().addAll(cName, cBarcode, cStock, cPrice, cLastBuy, cType, cUnit);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);

        // filter keyword
        search.textProperty().addListener((o, ov, nv) -> {
            String kw = nv == null ? "" : nv.trim().toLowerCase();
            filtered.setPredicate(p -> {
                if (kw.isEmpty()) return true;
                return (p.getName() != null && p.getName().toLowerCase().contains(kw))
                        || (p.getBarcode() != null && p.getBarcode().toLowerCase().contains(kw))
                        || (p.getTypeName() != null && p.getTypeName().toLowerCase().contains(kw));
            });
            if (!table.getItems().isEmpty()) {
                table.getSelectionModel().select(0);
                table.scrollTo(0);
            }
        });

        // stage
        Stage stage = new Stage();
        stage.setTitle("Cari Produk");
        stage.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) stage.initOwner(owner);

        // keyboard
        stage.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.isControlDown() && e.getCode() == KeyCode.F) {
                search.requestFocus(); search.selectAll(); e.consume();
            } else if (e.getCode() == KeyCode.ENTER) {
                // enter -> pilih
                if (!table.getSelectionModel().isEmpty()) {
                    stage.setUserData(table.getSelectionModel().getSelectedItem());
                    stage.close();
                }
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                stage.setUserData(null);
                stage.close();
                e.consume();
            }
        });

        table.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2 && !table.getSelectionModel().isEmpty()) {
                stage.setUserData(table.getSelectionModel().getSelectedItem());
                stage.close();
            }
        });

        btnOk.setOnAction(ae -> {
            stage.setUserData(table.getSelectionModel().getSelectedItem());
            stage.close();
        });
        btnCancel.setOnAction(ae -> { stage.setUserData(null); stage.close(); });

        HBox top = new HBox(8, new Label("Cari:"), search);
        top.setPadding(new Insets(10));
        HBox bottom = new HBox(8, btnOk, btnCancel);
        bottom.setPadding(new Insets(10));
        bottom.setStyle("-fx-alignment: center-right;");

        BorderPane root = new BorderPane();
        root.setTop(top);
        root.setCenter(table);
        root.setBottom(bottom);
        Scene scene = new Scene(root, 900, 520);
        stage.setScene(scene);
        stage.showAndWait();

        return Optional.ofNullable((Product) stage.getUserData());
    }
}
