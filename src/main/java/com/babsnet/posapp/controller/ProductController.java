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
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Objects;


public class ProductController implements FocusablePage {
    
    @FXML private TableView<Product> productTable;
    @FXML private TableColumn<Product, Number> colId;
    @FXML private TableColumn<Product, String> colName;
    @FXML private TableColumn<Product, Integer> colStock;
    @FXML private TableColumn<Product, Double> colPrice;
    @FXML private TableColumn<Product, Double> colLastBuyPrice;
    @FXML private TableColumn<Product, String> colType;
    @FXML private TableColumn<Product, String> colBarcode;
    @FXML private TableColumn<Product, String> colUnit;
    @FXML private TableColumn<Product, Boolean> colSelected;
    @FXML private TextField barcodeField, nameField, stockField, priceField, searchField, buyPriceField;
    @FXML private ComboBox<String> typeComboBox, unitComboBox;
    @FXML private VBox rootVBoxProduct;
    @FXML private Button generateBarcodeBtn;


    User currentUser = SessionManager.getInstance().getCurrentUser();
    private final EventHandler<KeyEvent> shortcutHandler = this::handleKeyEvents;
    private final ProductTypeRepository productTypeRepository = new ProductTypeRepository();
    private final UnitProductRepository unitProductRepository = new UnitProductRepository();

    private final ObservableList<Product> masterProducts = FXCollections.observableArrayList();
    private FilteredList<Product> filteredProducts;

    private final CheckBox headerSelectAll = new CheckBox();
    private final Map<Product, ChangeListener<Boolean>> selectionListeners = new WeakHashMap<>();

    // FILTER CONTROLS
    @FXML private ComboBox<String> typeFilterCombo;
    @FXML private ComboBox<String> unitFilterCombo;
    @FXML private TextField stockMinField, stockMaxField;
    @FXML private TextField priceMinField, priceMaxField;
    @FXML private CheckBox zeroStockOnlyCheck, selectedOnlyCheck;

    private static final String ALL = "ALL";


    @FXML
    private void initialize() {
        barcodeField.textProperty().addListener((obs, oldVal, newVal) -> updateGenerateBarcodeBtnState());
        nameField.textProperty().addListener((obs, oldVal, newVal) -> updateGenerateBarcodeBtnState());
        stockField.textProperty().addListener((obs, oldVal, newVal) -> updateGenerateBarcodeBtnState());
        priceField.textProperty().addListener((obs, oldVal, newVal) -> updateGenerateBarcodeBtnState());
        buyPriceField.textProperty().addListener((obs, oldVal, newVal) -> updateGenerateBarcodeBtnState());

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
        colLastBuyPrice.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().getLastBuyPrice()));

        colLastBuyPrice.setCellFactory(tc -> new TableCell<Product, Double>() {
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
                buyPriceField.setText(FormatUtil.toIntegerString(newSelection.getLastBuyPrice()));
                priceField.setText(FormatUtil.toIntegerString(newSelection.getPrice()));
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

        initDataAndFiltering();
        setupSelectAllHeader();
        applyFilter();

        loadProducts();

        FilteredList<Product> filtered = new FilteredList<>(productTable.getItems(), p -> true);
        SortedList<Product> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(productTable.comparatorProperty());
        productTable.setItems(sorted);
        this.filteredProducts = filtered;

        productTypeRepository.loadProductTypes(typeComboBox);
        unitProductRepository.loadUnitProduct(unitComboBox);

        // === Inisialisasi opsi filter ===
        initFilterControls();

        // Trigger filter ulang saat ada perubahan input
        searchField.textProperty().addListener((o,ov,nv) -> applyFilter());
        typeFilterCombo.valueProperty().addListener((o,ov,nv) -> applyFilter());
        unitFilterCombo.valueProperty().addListener((o,ov,nv) -> applyFilter());

        stockMinField.textProperty().addListener((o,ov,nv) -> applyFilter());
        stockMaxField.textProperty().addListener((o,ov,nv) -> applyFilter());
        priceMinField.textProperty().addListener((o,ov,nv) -> applyFilter());
        priceMaxField.textProperty().addListener((o,ov,nv) -> applyFilter());

        zeroStockOnlyCheck.selectedProperty().addListener((o,ov,nv) -> applyFilter());
        selectedOnlyCheck.selectedProperty().addListener((o,ov,nv) -> applyFilter());

    }

    private void initFilterControls() {
        // Isi combobox filter Type & Unit (dengan opsi "ALL")
        productTypeRepository.loadProductTypes(typeFilterCombo);
        if (!typeFilterCombo.getItems().contains(ALL)) {
            typeFilterCombo.getItems().add(0, ALL);
        }
        typeFilterCombo.getSelectionModel().select(0);

        unitProductRepository.loadUnitProduct(unitFilterCombo);
        if (!unitFilterCombo.getItems().contains(ALL)) {
            unitFilterCombo.getItems().add(0, ALL);
        }
        unitFilterCombo.getSelectionModel().select(0);
    }

    // Reset tombol Clear Filters
    @FXML
    private void clearFilterControls() {
        searchField.clear();
        typeFilterCombo.getSelectionModel().select(ALL);
        unitFilterCombo.getSelectionModel().select(ALL);
        stockMinField.clear();
        stockMaxField.clear();
        priceMinField.clear();
        priceMaxField.clear();
        zeroStockOnlyCheck.setSelected(false);
        selectedOnlyCheck.setSelected(false);
        applyFilter();
    }

    private void updateGenerateBarcodeBtnState() {
        boolean allEmpty = barcodeField.getText().isEmpty()
                && nameField.getText().isEmpty()
                && stockField.getText().isEmpty()
                && buyPriceField.getText().isEmpty()
                && priceField.getText().isEmpty();

        generateBarcodeBtn.setDisable(!allEmpty);
    }

    private void initDataAndFiltering() {
        // load awal
        masterProducts.setAll(ProductRepository.loadProducts("%"));

        filteredProducts = new FilteredList<>(masterProducts, p -> true);
        SortedList<Product> sorted = new SortedList<>(filteredProducts);
        sorted.comparatorProperty().bind(productTable.comparatorProperty());
        productTable.setItems(sorted);

        searchField.textProperty().addListener((obs, oldV, newV) -> applyFilter());
    }

    private void applyFilter() {
        if (filteredProducts == null) {
            // fallback: kalau belum di-setup, anggap tidak ada filter
            return;
        }

        final String kw = (searchField.getText() == null) ? "" : searchField.getText().trim().toLowerCase();
        final String typeSel = safeStr(typeFilterCombo.getValue());
        final String unitSel = safeStr(unitFilterCombo.getValue());

        final Integer stockMin = parseIntOrNull(stockMinField.getText());
        final Integer stockMax = parseIntOrNull(stockMaxField.getText());
        final Double priceMin = parseDoubleOrNull(priceMinField.getText());
        final Double priceMax = parseDoubleOrNull(priceMaxField.getText());

        final boolean zeroOnly = zeroStockOnlyCheck.isSelected();
        final boolean selectedOnly = selectedOnlyCheck.isSelected();

        filteredProducts.setPredicate(p -> {
            if (p == null) return false;

            // 1) Selected only?
            if (selectedOnly && !p.isSelected()) return false;

            // 2) Keyword match (name/barcode/type/unit)
            if (!kw.isEmpty()) {
                boolean matchKw =
                        (p.getName() != null && p.getName().toLowerCase().contains(kw)) ||
                                (p.getBarcode() != null && p.getBarcode().toLowerCase().contains(kw)) ||
                                (p.getTypeName() != null && p.getTypeName().toLowerCase().contains(kw)) ||
                                (p.getUnitName() != null && p.getUnitName().toLowerCase().contains(kw));
                if (!matchKw) return false;
            }

            // 3) Type exact (kecuali ALL)
            if (!ALL.equalsIgnoreCase(typeSel)) {
                if (p.getTypeName() == null || !p.getTypeName().equalsIgnoreCase(typeSel)) return false;
            }

            // 4) Unit exact (kecuali ALL)
            if (!ALL.equalsIgnoreCase(unitSel)) {
                if (p.getUnitName() == null || !p.getUnitName().equalsIgnoreCase(unitSel)) return false;
            }

            // 5) Stock conditions
            if (zeroOnly && p.getStock() != 0) return false;
            if (stockMin != null && p.getStock() < stockMin) return false;
            if (stockMax != null && p.getStock() > stockMax) return false;

            // 6) Price range (pakai price jual)
            Double price = p.getPrice();
            if (price == null) price = 0d;
            if (priceMin != null && price < priceMin) return false;
            if (priceMax != null && price > priceMax) return false;

            return true;
        });

        try {
            var m = ProductController.class.getDeclaredMethod("refreshHeaderSelectAllState");
            m.setAccessible(true);
            m.invoke(this);
        } catch (Exception ignored) {}
    }

    private String safeStr(String s) { return s == null ? ALL : s; }
    private Integer parseIntOrNull(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return null; }
    }
    private Double parseDoubleOrNull(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return null; }
    }


    private void setupSelectAllHeader() {
        headerSelectAll.setTooltip(new Tooltip("Select/Deselect semua baris yang terlihat"));
        headerSelectAll.setFocusTraversable(false);

        // klik header -> toggle semua item yg terlihat (hasil filter)
        headerSelectAll.selectedProperty().addListener((obs, was, isSel) -> {
            if (headerSelectAll.isIndeterminate()) return;
            for (Product p : productTable.getItems()) {
                p.setSelected(isSel);
            }
            refreshHeaderSelectAllState();
        });

        colSelected.setGraphic(headerSelectAll);
        colSelected.setEditable(true);

        // saat item terlihat berubah (karena filter/sort/refresh)
        productTable.getItems().addListener((ListChangeListener<Product>) c -> {
            while (c.next()) {
                if (c.wasAdded() || c.wasRemoved()) {
                    attachRowSelectionListeners();
                    refreshHeaderSelectAllState();
                }
            }
        });
    }

    private void attachRowSelectionListeners() {
        for (Product p : productTable.getItems()) {
            selectionListeners.computeIfAbsent(p, key -> {
                var l = (javafx.beans.value.ChangeListener<Boolean>) (o, ov, nv) -> refreshHeaderSelectAllState();
                p.selectedProperty().addListener(l);
                return l;
            });
        }
    }

    private void refreshHeaderSelectAllState() {
        var items = productTable.getItems();
        int total = items.size();
        if (total == 0) {
            headerSelectAll.setIndeterminate(false);
            headerSelectAll.setSelected(false);
            return;
        }
        long sel = items.stream().filter(Product::isSelected).count();
        if (sel == 0) {
            headerSelectAll.setIndeterminate(false);
            headerSelectAll.setSelected(false);
        } else if (sel == total) {
            headerSelectAll.setIndeterminate(false);
            headerSelectAll.setSelected(true);
        } else {
            headerSelectAll.setIndeterminate(true); // partial
        }
    }

    private List<Product> getSelectedProductsInViewOrder() {
        if (productTable.getItems() == null) return List.of();
        return productTable.getItems()
                .stream()
                .filter(Objects::nonNull)
                .filter(Product::isSelected)
                .collect(Collectors.toList());
    }





    public void loadProducts() {
        masterProducts.setAll(ProductRepository.loadProducts("%"));
//        applyFilter();
//        ObservableList<Product> productList = FXCollections.observableArrayList(
//                ProductRepository.loadProducts("%")
//        );
//        productTable.setItems(productList);
    }

    public void loadProducts(String searchFilter) {
//        ObservableList<Product> productList = FXCollections.observableArrayList(
//                ProductRepository.loadProducts(searchFilter)
//        );
//        productTable.setItems(productList);
        masterProducts.setAll(ProductRepository.loadProducts(searchFilter));
//        applyFilter();
    }

//    @FXML
//    private void selectAllProducts() {
//        for (Product p : productTable.getItems()) p.setSelected(true);
//        refreshHeaderSelectAllState();
//    }
//
//    @FXML
//    private void unselectAllProducts() {
//        for (Product p : productTable.getItems()) p.setSelected(false);
//        refreshHeaderSelectAllState();
//    }


    @FXML
    public void addProduct() {
        String name = nameField.getText();
        int stock = FormatUtil.parseIntSafe(stockField.getText());
        double price = FormatUtil.parseDoubleSafe(priceField.getText());
        double buyPrice = FormatUtil.parseDoubleSafe(buyPriceField.getText());
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
        ProductRepository.addProduct(name, stock, buyPrice, price, createdBy, createdDate, typeId, barcode, unitId);
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
        double buyPrice = Double.parseDouble(buyPriceField.getText());
        String selectedType = typeComboBox.getValue();
        String selectedUnit = unitComboBox.getValue();
        String updatedBy = String.valueOf(currentUser.getId());
        String updatedDate = LocalDateTime.now().toString();
        if(selectedType == null || selectedType.isEmpty()) {
            MessageDialogUtil.showError("Tidak dapat update product karena typenya kosong");
            return;
        }
        int typeId = productTypeRepository.getTypeIdByName(selectedType);
        int unitId = getUnitIdByName(selectedUnit);
        String barcode = barcodeField.getText();

        ProductRepository.updateProduct(
                name, stock, buyPrice, price, updatedBy, updatedDate, typeId, unitId, selected, barcode
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
        buyPriceField.clear();
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
                case X -> {
                    buyPriceField.requestFocus();
                    buyPriceField.selectAll();
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
            
            - Ctrl + F      : Fokus kolom pencarian
            - Ctrl + B      : Fokus barcode
            - Ctrl + N      : Fokus nama produk
            - Ctrl + X      : Fokus harga beli produk
            - Ctrl + P      : Fokus harga jual produk
            - Ctrl + S      : Fokus stok
            - Ctrl + Insert : Simpan data produk
            - E             : Edit produk (update)
            - Delete        : Hapus produk
            - Home          : Pilih baris pertama
            - End           : Pilih baris terakhir
            - Up/Down       : Navigasi baris
            - Ctrl + L      : Bersihkan form
            - Ctrl + R      : Refresh data tabel
            - Ctrl + T      : Pilih type
            - Ctrl + Y      : Tambah type
            - Ctrl + W      : Pilih Unit
            - Ctrl + U      : Tambah Unit
            - Ctrl + I      : Tampilkan info shortcut keyboard
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

    @FXML
    private void exportProductsCsv() {
        List<Product> rows = getSelectedProductsInViewOrder();
        if (rows.isEmpty()) {
            MessageDialogUtil.showWarning("Belum ada produk yang dicentang. Silakan centang kolom Select dulu.");
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Save Products CSV (Selected)");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV file", "*.csv"));
        fc.setInitialFileName("products_selected_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".csv");
        File file = fc.showSaveDialog(productTable.getScene().getWindow());
        if (file == null) return;

        final char DELIM = ',';

        try (var out = Files.newBufferedWriter(Path.of(file.getAbsolutePath()), StandardCharsets.UTF_8)) {
            // BOM supaya Excel baca UTF-8 dengan benar
            out.write('\uFEFF');

            // header
            out.write(String.join(String.valueOf(DELIM),
                    "No","Name","Stock","Harga Beli","Harga Jual","Type","Barcode","Unit"));
            out.write("\r\n");

            int no = 1;
            for (Product p : rows) {
                String name   = csvEscape(p.getName(), DELIM);
                String stock  = String.valueOf(p.getStock());
                String last   = FormatUtil.toIntegerString(p.getLastBuyPrice()); // Harga Beli
                String price  = FormatUtil.toIntegerString(p.getPrice());        // Harga Jual
                String type   = csvEscape(p.getTypeName(), DELIM);
                String barcode = (p.getBarcode() == null || p.getBarcode().isEmpty())
                        ? ""
                        : "=\"" + p.getBarcode() + "\""; // jaga leading zero di Excel
                String unit   = csvEscape(p.getUnitName(), DELIM);

                out.write(no++ + String.valueOf(DELIM)
                        + name + DELIM
                        + stock + DELIM
                        + last + DELIM   // <= sesuai header: Harga Beli
                        + price + DELIM  // <= sesuai header: Harga Jual
                        + type + DELIM
                        + barcode + DELIM
                        + unit);
                out.write("\r\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
            MessageDialogUtil.showError("Gagal export CSV: " + e.getMessage());
            return;
        }

        MessageDialogUtil.showInfo("Export CSV (selected) selesai.");
    }

    private String csvEscape(String s, char delim) {
        if (s == null) return "";
        boolean needQuote = s.indexOf(delim) >= 0 || s.contains("\"") || s.contains("\n") || s.contains("\r");
        String val = s.replace("\"", "\"\"");
        return needQuote ? "\"" + val + "\"" : val;
    }

    @FXML
    private void exportProductsPdf() {
        List<Product> rows = getSelectedProductsInViewOrder();
        if (rows.isEmpty()) {
            MessageDialogUtil.showWarning("Belum ada produk yang dicentang. Silakan centang kolom Select dulu.");
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Save Products PDF (Selected)");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF file", "*.pdf"));
        fc.setInitialFileName("products_selected_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf");
        File file = fc.showSaveDialog(productTable.getScene().getWindow());
        if (file == null) return;

        try {
            Document doc = new Document(PageSize.A4.rotate(), 24f, 24f, 24f, 24f);
            PdfWriter.getInstance(doc, new java.io.FileOutputStream(file));
            doc.open();

            var titleFont  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            var headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            var cellFont   = FontFactory.getFont(FontFactory.HELVETICA, 10);

            doc.add(new Paragraph("Product List (Selected Only)", titleFont));
            doc.add(new Paragraph("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), cellFont));
            doc.add(new Paragraph(" ")); // spacer

            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{5f, 28f, 9f, 12f, 12f, 14f, 15f, 10f});

            java.util.function.Function<String, PdfPCell> H = text -> {
                PdfPCell c = new PdfPCell(new Phrase(text, headerFont));
                c.setHorizontalAlignment(Element.ALIGN_CENTER);
                c.setPadding(6f);
                return c;
            };

            table.addCell(H.apply("No"));
            table.addCell(H.apply("Name"));
            table.addCell(H.apply("Stock"));
            table.addCell(H.apply("Harga Beli"));
            table.addCell(H.apply("Harga Jual"));
            table.addCell(H.apply("Type"));
            table.addCell(H.apply("Barcode"));
            table.addCell(H.apply("Unit"));

            int no = 1;
            for (Product p : rows) {
                table.addCell(makeCell(String.valueOf(no++), cellFont, Element.ALIGN_CENTER));
                table.addCell(makeCell(nullSafe(p.getName()), cellFont, Element.ALIGN_LEFT));
                table.addCell(makeCell(String.valueOf(p.getStock()), cellFont, Element.ALIGN_RIGHT));
                table.addCell(makeCell(FormatUtil.toRupiahNoDecimal(p.getLastBuyPrice()), cellFont, Element.ALIGN_RIGHT));
                table.addCell(makeCell(FormatUtil.toRupiahNoDecimal(p.getPrice()), cellFont, Element.ALIGN_RIGHT));
                table.addCell(makeCell(nullSafe(p.getTypeName()), cellFont, Element.ALIGN_LEFT));
                table.addCell(makeCell(nullSafe(p.getBarcode()), cellFont, Element.ALIGN_LEFT));
                table.addCell(makeCell(nullSafe(p.getUnitName()), cellFont, Element.ALIGN_LEFT));
            }

            doc.add(table);
            doc.close();
            MessageDialogUtil.showInfo("Export PDF (selected) selesai.");
        } catch (Exception e) {
            e.printStackTrace();
            MessageDialogUtil.showError("Gagal export PDF: " + e.getMessage());
        }
    }


    private PdfPCell makeCell(String text, Font font, int hAlign) {
        PdfPCell c = new PdfPCell(new Phrase(text == null ? "" : text, font));
        c.setHorizontalAlignment(hAlign);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(5f);
        return c;
    }

    private String nullSafe(String s) { return s == null ? "" : s; }

}
