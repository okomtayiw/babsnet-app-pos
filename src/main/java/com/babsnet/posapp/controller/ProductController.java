package com.babsnet.posapp.controller;

import com.babsnet.posapp.model.Product;
import com.babsnet.posapp.model.User;
import com.babsnet.posapp.repository.ProductRepository;
import com.babsnet.posapp.repository.ProductTypeRepository;
import com.babsnet.posapp.repository.UnitProductRepository;
import com.babsnet.posapp.session.SessionManager;
import com.babsnet.posapp.util.*;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.List;

public class ProductController implements FocusablePage {
    
    @FXML private TableView<Product> productTable;
    @FXML private TableColumn<Product, Number> colId;
    @FXML private TableColumn<Product, String> colName;
    @FXML private TableColumn<Product, Integer> colStock;
    @FXML private TableColumn<Product, Double> colPrice;
    @FXML private TableColumn<Product, String> colType;
    @FXML private TableColumn<Product, String> colBarcode;
    @FXML private TableColumn<Product, String> colUnit;
    @FXML private TableColumn<Product, Boolean> colSelected;
    @FXML private TextField barcodeField, nameField, stockField, priceField, searchField;
    @FXML private ComboBox<String> typeComboBox, unitComboBox;
    @FXML private VBox rootVBoxProduct;
    @FXML private Button generateBarcodeBtn;


    User currentUser = SessionManager.getInstance().getCurrentUser();
    private final EventHandler<KeyEvent> shortcutHandler = this::handleKeyEvents;
    private final ProductTypeRepository productTypeRepository = new ProductTypeRepository();
    private final UnitProductRepository unitProductRepository = new UnitProductRepository();

    @FXML
    private void initialize() {
        barcodeField.textProperty().addListener((obs, oldVal, newVal) -> updateGenerateBarcodeBtnState());
        nameField.textProperty().addListener((obs, oldVal, newVal) -> updateGenerateBarcodeBtnState());
        stockField.textProperty().addListener((obs, oldVal, newVal) -> updateGenerateBarcodeBtnState());
        priceField.textProperty().addListener((obs, oldVal, newVal) -> updateGenerateBarcodeBtnState());

        updateGenerateBarcodeBtnState();
        rootVBoxProduct.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null) oldScene.removeEventFilter(KeyEvent.KEY_PRESSED, shortcutHandler);
            if (newScene != null) newScene.addEventFilter(KeyEvent.KEY_PRESSED, shortcutHandler);
        });
//        stockField.setManaged(false);
        productTable.setEditable(true);
        colSelected.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
        colSelected.setCellFactory(tc -> new CheckBoxTableCell<>());
        colSelected.setEditable(true);
        colId.setCellValueFactory(cellData -> new SimpleIntegerProperty(productTable.getItems().indexOf(cellData.getValue()) + 1));
        colName.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().getName()));
        colStock.setCellValueFactory(data -> {
            int stock = data.getValue().getStock();
            return new SimpleIntegerProperty(stock).asObject();
        });
        colStock.setCellFactory(tc -> new TableCell<Product, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else if (item == 0) {
                    setText("0");
                    setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                } else {
                    setText(String.valueOf(item));
                    setStyle("");
                }

            }
        });

        colPrice.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().getPrice()));

        colPrice.setCellFactory(tc -> new TableCell<Product, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(FormatUtil.toRupiahNoDecimal(price));
                }
            }
        });
        colType.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().getTypeName()));
        colBarcode.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().getBarcode()));
        colBarcode.setCellFactory(tc -> new TableCell<Product, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setOnMouseClicked(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: blue; -fx-underline: true; -fx-cursor: hand;"); 

                    setOnMouseClicked(event -> {
                        if (event.getClickCount() == 1) {
                            showBarcodePopup(item);
                        }
                    });
                }
            }
        });

        colUnit.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().getUnitName()));


        productTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                nameField.setText(newSelection.getName());
                stockField.setText(String.valueOf(newSelection.getStock()));
                priceField.setText(String.valueOf(newSelection.getPrice()));
                barcodeField.setText(newSelection.getBarcode());
                typeComboBox.setValue(newSelection.getTypeName());
                unitComboBox.setValue(newSelection.getUnitName());
            }
        });
        productTable.focusedProperty().addListener((obs, oldVal, nowFocused) -> {
            if (nowFocused && productTable.getSelectionModel().isEmpty() && !productTable.getItems().isEmpty()) {
                productTable.getSelectionModel().select(0);
                productTable.scrollTo(0);
            }
        });

        loadProducts();
        productTypeRepository.loadProductTypes(typeComboBox);
        unitProductRepository.loadUnitProduct(unitComboBox);
    }

    private void updateGenerateBarcodeBtnState() {
        boolean allEmpty = barcodeField.getText().isEmpty()
                && nameField.getText().isEmpty()
                && stockField.getText().isEmpty()
                && priceField.getText().isEmpty();

        generateBarcodeBtn.setDisable(!allEmpty);
    }



    public void loadProducts() {
        ObservableList<Product> productList = FXCollections.observableArrayList(
                ProductRepository.loadProducts("%")
        );
        productTable.setItems(productList);
    }

    public void loadProducts(String searchFilter) {
        ObservableList<Product> productList = FXCollections.observableArrayList(
                ProductRepository.loadProducts(searchFilter)
        );
        productTable.setItems(productList);
    }

    @FXML
    public void addProduct() {
        String name = nameField.getText();
        int stock = FormatUtil.parseIntSafe(stockField.getText());
        double price = FormatUtil.parseDoubleSafe(priceField.getText());
        String selectedType = typeComboBox.getValue();
        String createdBy = String.valueOf(currentUser.getId());
        String createdDate = LocalDateTime.now().toString();
        int typeId = productTypeRepository.getTypeIdByName(selectedType);
        String barcode = barcodeField.getText();
        String selectedUnit = unitComboBox.getValue();
        int unitId = getUnitIdByName(selectedUnit);

        if(name.isEmpty() || barcode.isEmpty()) {
            MessageDialogUtil.showWarning("Barcode and name does not empty");
            return;
        }

        if (selectedType == null || selectedType.trim().isEmpty()) {
            MessageDialogUtil.showWarning("Product type belum dipilih!");
            typeComboBox.requestFocus();
            return;
        }

        if (selectedUnit == null || selectedUnit.trim().isEmpty()) {
            MessageDialogUtil.showWarning("Product unit belum dipilih!");
            typeComboBox.requestFocus();
            return;
        }

        if (ProductRepository.isBarcodeExist(barcode)) {
            MessageDialogUtil.showWarning("Barcode \"" + barcode + "\" sudah terdaftar pada produk lain!");
            barcodeField.requestFocus();
            barcodeField.selectAll();
            return;
        }
        ProductRepository.addProduct(name, stock, price, createdBy, createdDate, typeId, barcode, unitId);
        clearForm();
        loadProducts();
    }

    @FXML
    public void updateProduct() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        String name = nameField.getText();
        int stock = Integer.parseInt(stockField.getText());
        double price = Double.parseDouble(priceField.getText());
        String selectedType = typeComboBox.getValue();
        String selectedUnit = unitComboBox.getValue();
        String updatedBy = String.valueOf(currentUser.getId());
        String updatedDate = LocalDateTime.now().toString();
        int typeId = productTypeRepository.getTypeIdByName(selectedType);
        int unitId = getUnitIdByName(selectedUnit);
        String barcode = barcodeField.getText();

        ProductRepository.updateProduct(
                name, stock, price, updatedBy, updatedDate, typeId, unitId, selected, barcode
        );
        clearForm();
        loadProducts();
    }

    @FXML
    public void deleteSelectedProduct() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        if (ProductRepository.checkProduct(selected)){
            MessageDialogUtil.showError("Cannot delete product \"" + selected.getName() + "\" because it is used in a transaction.");
            return;
        }
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("Are you sure you want to delete product \"" + selected.getName() + "\"?");
        if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        ProductRepository.deleteProductById(selected);
        clearForm();
        loadProducts();
    }

    @FXML
    public void clearForm() {
        nameField.clear();
        stockField.clear();
        priceField.clear();
        barcodeField.clear();
        productTable.getSelectionModel().clearSelection();
    }

    @FXML
    public void resetSearch() {
        searchField.clear();
        loadProducts();
    }

    @FXML
    public void searchProduct() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            loadProducts();
        } else {
            loadProducts(keyword);
        }
    }



    private void handleKeyEvents(KeyEvent event) {
        KeyCode code = event.getCode();

        // === 1. Focus & Input Field Shortcut ===
        if (event.isControlDown()) {
            switch (code) {
                case F -> {
                    searchField.requestFocus();
                    searchField.selectAll();
                    event.consume();
                    return;
                }
                case B -> {
                    barcodeField.requestFocus();
                    barcodeField.selectAll();
                    event.consume();
                    return;
                }
                case N -> {
                    nameField.requestFocus();
                    nameField.selectAll();
                    event.consume();
                    return;
                }
                case P -> {
                    priceField.requestFocus();
                    priceField.selectAll();
                    event.consume();
                    return;
                }
                case S -> {
                    stockField.requestFocus();
                    stockField.selectAll();
                    event.consume();
                    return;
                }
                case T -> {
                    typeComboBox.requestFocus();
                    typeComboBox.show();
                    event.consume();
                    return;
                }
                case W -> {
                    unitComboBox.requestFocus();
                    unitComboBox.show();
                    event.consume();
                    return;
                }
                case Y -> {
                    showAddProductTypeDialog();
                    event.consume();
                    return;
                }
                case U -> {
                    showAddUnitDialog();
                    event.consume();
                    return;
                }
                case O -> { // Fokus ke tabel produk
                    if (!productTable.getItems().isEmpty()) {
                        productTable.requestFocus();
                        int selectedIdx = productTable.getSelectionModel().getSelectedIndex();
                        if (selectedIdx < 0) selectedIdx = 0;
                        productTable.getSelectionModel().select(selectedIdx);
                        productTable.scrollTo(selectedIdx);
                    }
                    event.consume();
                    return;
                }
                case L -> {
                    clearForm();
                    event.consume();
                    return;
                }
                case R -> {
                    loadProducts();
                    event.consume();
                    return;
                }
                case I -> {
                    showShortcutInfo();
                    event.consume();
                    return;
                }
                case INSERT -> {
                    addProduct();
                    event.consume();
                }
                case E -> {
                    updateProduct();
                    event.consume();
                }
                default -> {
                    // do nothing
                }
            }
        }

        // === 2. Table Navigation & Actions (aktif hanya jika table focus) ===
        if (productTable.isFocused()) {
            int idx = productTable.getSelectionModel().getSelectedIndex();
            int lastIdx = productTable.getItems().size() - 1;

            switch (code) {
                case HOME -> {
                    if (lastIdx >= 0) {
                        productTable.getSelectionModel().select(0);
                        productTable.scrollTo(0);
                    }
                    event.consume();
                }
                case END -> {
                    if (lastIdx >= 0) {
                        productTable.getSelectionModel().select(lastIdx);
                        productTable.scrollTo(lastIdx);
                    }
                    event.consume();
                }
                case UP -> {
                    if (idx > 0) {
                        productTable.getSelectionModel().select(idx - 1);
                        productTable.scrollTo(idx - 1);
                    }
                    event.consume();
                }
                case DOWN -> {
                    if (idx < lastIdx) {
                        productTable.getSelectionModel().select(idx + 1);
                        productTable.scrollTo(idx + 1);
                    }
                    event.consume();
                }
                case DELETE -> {
                    deleteSelectedProduct();
                    event.consume();
                }
                default -> {
                    // nothing
                }
            }
        }
    }

    @FXML
    private void showAddProductTypeDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/babsnet/posapp/ProductTypePage.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Kelola Jenis Produk");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL); // Modal, biar harus ditutup dulu
            stage.showAndWait();

            // Setelah window ditutup, refresh ComboBox Type!
            productTypeRepository.loadProductTypes(typeComboBox);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @FXML
    private void showAddUnitDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/babsnet/posapp/unit_product.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = new Stage();
            stage.setTitle("Manage Units");
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            unitProductRepository.loadUnitProduct(unitComboBox); // refresh data di combobox utama
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private int getUnitIdByName(String unitName) {
        String sql = "SELECT id FROM units WHERE name = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, unitName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }


    @FXML
    public void showShortcutInfo() {
        String info = """
            **Keyboard Shortcut Halaman Produk**
            
            - Ctrl + F  : Fokus kolom pencarian
            - Ctrl + B  : Fokus barcode
            - Ctrl + N  : Fokus nama produk
            - Ctrl + P  : Fokus harga produk
            - Ctrl + S  : Fokus stok
            - Insert    : Tambah produk baru
            - E         : Edit produk (update)
            - Delete    : Hapus produk
            - Home      : Pilih baris pertama
            - End       : Pilih baris terakhir
            - Up/Down   : Navigasi baris
            - Ctrl + L  : Bersihkan form
            - Ctrl + R  : Refresh data tabel
            - Ctrl + T  : Pilih type
            - Ctrl + Y  : Tambah type
            - Ctrl + W  : Pilih Unit
            - Ctrl + U  : Tambah Unit
            - Ctrl + I  : Tampilkan info shortcut keyboard
            """;
        TextArea area = new TextArea(info);
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefColumnCount(35);
        area.setPrefRowCount(16);
        Alert infoDialog = new Alert(Alert.AlertType.INFORMATION);
        infoDialog.setTitle("Daftar Shortcut Keyboard");
        infoDialog.setHeaderText("Keyboard Shortcut Halaman Produk");
        infoDialog.getDialogPane().setContent(area);
        infoDialog.showAndWait();
    }

    private void showBarcodePopup(String barcode) {
        try {
            BufferedImage barcodeImage = BarcodeUtil.generateBarcodeWithText(barcode, 350, 90);
            String base64 = BarcodeUtil.generateBarcodeBase64(barcode, 350, 90);
            byte[] imageBytes = java.util.Base64.getDecoder().decode(base64);
            javafx.scene.image.Image image = new javafx.scene.image.Image(new java.io.ByteArrayInputStream(imageBytes));
            javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(image);
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(350);

            // Copy barcode text button
            Button copyBtn = new Button("Copy Barcode");

            copyBtn.setOnAction(e -> {
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString(barcode);
                javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
            });



            Button saveBtn = new Button("Save Barcode");
            saveBtn.setOnAction(e -> {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Save Barcode Image");
                fileChooser.getExtensionFilters().add(
                        new FileChooser.ExtensionFilter("PNG Image", "*.png")
                );
                fileChooser.setInitialFileName(barcode + ".png");
                File file = fileChooser.showSaveDialog(imageView.getScene().getWindow());
                if (file != null) {
                    try {
                        ImageIO.write(barcodeImage, "png", file); // simpan dari BufferedImage ZXing
                        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Barcode saved to: " + file.getAbsolutePath());
                        alert.showAndWait();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to save barcode: " + ex.getMessage());
                        alert.showAndWait();
                    }
                }
            });


            Label barcodeLabel = new Label(barcode);
            VBox vbox = new VBox(15, imageView, barcodeLabel, copyBtn, saveBtn);
            vbox.setPadding(new javafx.geometry.Insets(10));
            vbox.setAlignment(javafx.geometry.Pos.CENTER);

            Stage popup = new Stage();
            popup.setScene(new Scene(vbox));
            popup.setTitle("Barcode Preview");
            popup.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            popup.showAndWait();
        } catch (Exception e) {
            MessageDialogUtil.showError("Gagal generate barcode.\n" + e.getMessage());
        }
    }


    @Override
    public void focusRootBox() {
        Platform.runLater(() -> rootVBoxProduct.requestFocus());
    }

    @FXML
    private void exportBarcodePdf() {
        List<Product> selectedProducts = productTable.getItems().stream()
                .filter(Product::isSelected)
                .toList();
        if (selectedProducts.isEmpty()) {
            MessageDialogUtil.showWarning("Pilih minimal satu produk terlebih dahulu!");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fileChooser.showSaveDialog(productTable.getScene().getWindow());
        if (file != null) {
            try {
                generateBarcodePdf(selectedProducts, file.getAbsolutePath());
                MessageDialogUtil.showInfo("Barcode berhasil diexport ke PDF!");
            } catch (Exception e) {
                MessageDialogUtil.showError("Gagal export PDF: " + e.getMessage());
            }
        }
    }

    private void generateBarcodePdf(List<Product> products, String filePath) throws Exception {
        float widthPt = 250f;
        float heightPt = 150f;

        Document document = new Document(new Rectangle(widthPt, heightPt));
        PdfWriter.getInstance(document, new java.io.FileOutputStream(filePath));
        document.open();

        for (Product product : products) {
            String barcodeText = product.getBarcode();
            if (barcodeText == null || barcodeText.trim().isEmpty()) {
                document.add(new Paragraph("No barcode."));
                continue;
            }

            try {
                // PERKECIL tinggi barcode (misal, 40 atau 30 px saja!)
                BufferedImage barcodeImg = BarcodeUtil.generateBarcodeBufferedImage(barcodeText, 180, 30);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(barcodeImg, "png", baos);
                Image image = Image.getInstance(baos.toByteArray());
                image.setAlignment(Image.ALIGN_CENTER);

                // scaleToFit supaya muat!
                image.scaleToFit(widthPt - 20, 40); // max height 40pt

                PdfPTable table = new PdfPTable(1);
                table.setWidthPercentage(100);
                table.getDefaultCell().setBorder(Rectangle.NO_BORDER);
                table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);

                // Cell 1: barcode image (kecil)
                PdfPCell barcodeCell = new PdfPCell(image, true);
                barcodeCell.setBorder(Rectangle.NO_BORDER);
                barcodeCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                barcodeCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                barcodeCell.setPaddingTop(10f);
                barcodeCell.setPaddingBottom(4f);
                table.addCell(barcodeCell);

                // Cell 2: kode barcode
                Font codeFont = new Font(Font.HELVETICA, 12, Font.BOLD);
                PdfPCell codeCell = new PdfPCell(new Phrase(barcodeText, codeFont));
                codeCell.setBorder(Rectangle.NO_BORDER);
                codeCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                codeCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                codeCell.setNoWrap(true);
                codeCell.setPaddingTop(2f);
                table.addCell(codeCell);

                document.add(table);

            } catch (Exception e) {
                document.add(new Paragraph("Barcode Error: " + e.getMessage()));
                e.printStackTrace();
            }
        }

        document.close();
    }

    @FXML
    private void handleGenerateBarcode() {
        String base = "P";
        String generated = BarcodeUtil.generateUniqueBarcode(base);
        barcodeField.setText(generated);
        barcodeField.requestFocus();
    }

}
