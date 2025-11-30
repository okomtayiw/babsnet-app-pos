package com.babsnet.posapp.repository;

import com.babsnet.posapp.model.Product;
import com.babsnet.posapp.model.StockAdjustment;
import com.babsnet.posapp.model.User;
import com.babsnet.posapp.util.ConfigUtil;
import com.babsnet.posapp.util.DatabaseHelper;
import com.babsnet.posapp.util.MessageDialogUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ProductRepository {

    public static Product getProductByBarcode(String barcode) {
        String sql = """
        SELECT p.id, p.name, p.stock, p.price,p.last_buy_price, p.discount, p.barcode, pt.type_name
        FROM products p
        LEFT JOIN product_types pt ON p.type_id = pt.id
        WHERE p.barcode = ?
        """;

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, barcode);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                int stock = rs.getInt("stock");
                double price = rs.getDouble("price");
                double lastBuyPrice = rs.getDouble("last_buy_price");
                double discount = rs.getDouble("discount");
                String typeName = rs.getString("type_name");
                String barcodeValue = rs.getString("barcode");

                return new Product(id, name, stock, price,lastBuyPrice, discount, typeName, barcodeValue);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void deleteProductById(Product product) {

        String sql = "DELETE FROM products WHERE id = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, product.getId());
            pstmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
            MessageDialogUtil.showError("Failed to delete product.");
        }
    }




    public boolean checkStockAvailability(String barcode, int quantity) {
        String sql = "SELECT stock FROM products WHERE barcode = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, barcode);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                int availableStock = rs.getInt("stock");
                return availableStock < quantity;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public void restoreStockQuantity(String barcode, int quantityRestored) {
        safeExecuteUpdate("UPDATE products SET stock = stock + ? WHERE barcode = ?", quantityRestored, barcode);
    }



    public int saveTransactionWithDetails(double total, User user, List<Product> cart, String paymentMethod) throws Exception {
        String transactionNumber = generateTransactionNumber();
        String transactionSql = "INSERT INTO transactions (transaction_number, payment_method, trans_date, total, user_id) VALUES (?, ?, ?, ?, ?)";
        String detailSql = "INSERT INTO transaction_details (transaction_id, product_id, qty, price, subtotal) VALUES (?, ?, ?, ?, ?)";
        String stockUpdateSql = "UPDATE products SET stock = stock - ? WHERE barcode = ?";

        try (Connection conn = DatabaseHelper.getConnection()) {
            conn.setAutoCommit(false); // Start transaction

            // Insert into transactions
            int transactionId;
            try (PreparedStatement transStmt = conn.prepareStatement(transactionSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                transStmt.setString(1, transactionNumber);
                transStmt.setString(2, paymentMethod);
                transStmt.setString(3, LocalDateTime.now().toString());
                transStmt.setBigDecimal(4, BigDecimal.valueOf(total));
                transStmt.setInt(5, user.getId());
                transStmt.executeUpdate();

                try (ResultSet generatedKeys = transStmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        transactionId = generatedKeys.getInt(1);
                    } else {
                        throw new SQLException("Failed to retrieve transaction ID");
                    }
                }
            }

            // Insert into transaction_details & Update stock
            try (PreparedStatement detailStmt = conn.prepareStatement(detailSql);
                 PreparedStatement stockStmt = conn.prepareStatement(stockUpdateSql)) {

                for (Product product : cart) {
                    // Insert detail
                    detailStmt.setInt(1, transactionId);
                    detailStmt.setInt(2, product.getId());
                    detailStmt.setInt(3, product.getStock());
                    detailStmt.setBigDecimal(4, BigDecimal.valueOf(product.getPrice()));
                    detailStmt.setBigDecimal(5, BigDecimal.valueOf(product.getStock() * product.getPrice()));
                    detailStmt.addBatch();

                    // Update stock
                    stockStmt.setInt(1, product.getStock());
                    stockStmt.setString(2, product.getBarcode());
                    stockStmt.addBatch();
                }

                detailStmt.executeBatch();
                stockStmt.executeBatch();
            }

            conn.commit(); // Commit kalau berhasil semua
            return transactionId;

        } catch (Exception e) {
            e.printStackTrace();
            try {
                DatabaseHelper.getConnection().rollback();
            } catch (Exception rollbackEx) {
                rollbackEx.printStackTrace();
            }
            return 0;
        }
    }

    public String generateTransactionNumberOld() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "TRX-" + datePart + "-";

        String sql = "SELECT transaction_number FROM transactions WHERE transaction_number LIKE ? ORDER BY transaction_number DESC LIMIT 1";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, prefix + "%");
            ResultSet rs = pstmt.executeQuery();

            int nextNumber = 1;
            if (rs.next()) {
                String lastNumber = rs.getString("transaction_number");
                // Extract nomor terakhir (4 digit terakhir)
                String[] parts = lastNumber.split("-");
                nextNumber = Integer.parseInt(parts[2]) + 1;
            }

            return String.format("%s%04d", prefix, nextNumber);

        } catch (Exception e) {
            e.printStackTrace();
            return prefix + "0001";
        }
    }

    public static String generateTransactionNumber() throws Exception {
        String prefix = ConfigUtil.get("trx.prefix");
        String numberLength = ConfigUtil.get("trx.number_length");
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE); // yyyyMMdd
        Connection conn = DatabaseHelper.getConnection();
        // Hitung jumlah transaksi hari ini
        String sql = "SELECT COUNT(*) FROM transactions WHERE DATE(trans_date) = CURDATE()";
        int count = 0;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) count = rs.getInt(1);
        }
        int nomorUrut = count + 1;

        // Format nomor urut sesuai length dari config
        String kodeUrut = String.format("%0" + numberLength + "d", nomorUrut);

        return String.format("%s-%s-%s", prefix, today, kodeUrut);
    }



    private void safeExecuteUpdate(String sql, Object... params) {
        int retries = 5;
        for (int i = 0; i < retries; i++) {
            try (Connection conn = DatabaseHelper.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                for (int j = 0; j < params.length; j++) {
                    pstmt.setObject(j + 1, params[j]);
                }
                pstmt.executeUpdate();
                return;

            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("database is locked")) {
                    try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                } else {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }


    public static void addStock(Connection conn, int productId, int quantity) throws SQLException {
        String sql = "UPDATE products SET stock = stock + ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, quantity);
            pstmt.setInt(2, productId);
            pstmt.executeUpdate();
        }
    }

    public static void updateLastBuyPrice(Connection conn, int productId, double lastBuyPrice) throws SQLException {
        final String sql = "UPDATE products SET last_buy_price = ?, updated_date = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, lastBuyPrice);
            pstmt.setInt(2, productId);
            int updated = pstmt.executeUpdate();
            if (updated == 0) {
                throw new SQLException("Product not found, id=" + productId);
            }
        }
    }


    public static void reduceStock(Connection conn, int productId, int quantity) throws SQLException {
        String sql = "UPDATE products SET stock = stock - ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, quantity);
            pstmt.setInt(2, productId);
            pstmt.executeUpdate();
        }
    }


    public static List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
        String sql = """
        SELECT p.id, p.name, p.stock, p.price, p.discount, p.barcode,
               pt.type_name AS product_type, u.name AS unit_name
        FROM products p
        LEFT JOIN product_types pt ON p.type_id = pt.id
        LEFT JOIN units u ON p.unit_id = u.id
        """;

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Product product = new Product();
                product.setId(rs.getInt("id"));
                product.setName(rs.getString("name"));
                product.setStock(rs.getInt("stock"));
                product.setPrice(rs.getDouble("price"));
                product.setDiscount(rs.getDouble("discount"));
                product.setBarcode(rs.getString("barcode"));
                product.setTypeName(rs.getString("product_type"));
                product.setUnitName(rs.getString("unit_name"));

                products.add(product);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return products;
    }

    public static void adjustStock(int productId, int quantity, String reason, String description, User currentUser) {
        String updateStockSql = "UPDATE products SET stock = stock + ? WHERE id = ?";
        // Pakai jam server MariaDB
        String insertLogSql = """
        INSERT INTO stock_adjustments (product_id, adjust_date, adjust_by, reason, description, quantity_change)
        VALUES (?, NOW(), ?, ?, ?, ?)
    """;

        int adjustedQuantity = getAdjustedQuantity(quantity, reason);

        try (Connection conn = DatabaseHelper.getConnection()) {
            conn.setAutoCommit(false);

            // Update stock
            try (PreparedStatement stockStmt = conn.prepareStatement(updateStockSql)) {
                stockStmt.setInt(1, adjustedQuantity);
                stockStmt.setInt(2, productId);
                stockStmt.executeUpdate();
            }

            // Insert stock adjustment log
            try (PreparedStatement logStmt = conn.prepareStatement(insertLogSql)) {
                logStmt.setInt(1, productId);
                // kolom 2 diisi NOW() di SQL, jadi parameter mulai dari index 2
                logStmt.setInt(2, currentUser.getId());
                logStmt.setString(3, reason);
                logStmt.setString(4, description);
                logStmt.setInt(5, adjustedQuantity);
                logStmt.executeUpdate();
            }

            conn.commit();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static int getAdjustedQuantity(int quantity, String reason) {
        if (reason == null || reason.isEmpty()) throw new IllegalArgumentException("Reason cannot be null or empty");

        return switch (reason.toUpperCase()) {
            case "REJECTED", "EXPIRED", "DAMAGED", "LOST" -> -Math.abs(quantity);
            case "RETURN", "CORRECTION", "FOUND" -> Math.abs(quantity);
            default -> throw new IllegalArgumentException("Unknown stock adjustment reason: " + reason);
        };
    }


    public static List<StockAdjustment> getAllStockAdjustments() {
        List<StockAdjustment> adjustmentList = new ArrayList<>();

        String sql = """
        SELECT sa.id, p.name AS product_name,p.barcode,sa.product_id, sa.reason, sa.quantity_change, sa.description, sa.adjust_date
        FROM stock_adjustments sa
        LEFT JOIN products p ON sa.product_id = p.id
        ORDER BY sa.adjust_date DESC
    """;

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                StockAdjustment adj = new StockAdjustment(
                        rs.getInt("id"),
                        rs.getInt("product_id"),
                        rs.getString("barcode"),
                        rs.getString("product_name"),
                        rs.getString("reason"),
                        rs.getInt("quantity_change"),
                        rs.getString("description"),
                        rs.getString("adjust_date")
                );
                adjustmentList.add(adj);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return adjustmentList;
    }

    public static boolean deleteStockAdjustment(int adjustmentId) {
        String sqlDelete = "DELETE FROM stock_adjustments WHERE id = ?";
        try (Connection conn = DatabaseHelper.getConnection()) {
            conn.setAutoCommit(false);

            // Dapatkan adjustment dulu
            StockAdjustment adjustment = getStockAdjustmentById(adjustmentId);
            if (adjustment == null) return false;

            // Undo stock: Tambah atau kurangi stok kebalikan adjustment
            ProductRepository.addStock(conn, adjustment.getProductId(), -adjustment.getQuantity());

            // Hapus dari tabel adjustment
            try (PreparedStatement pstmt = conn.prepareStatement(sqlDelete)) {
                pstmt.setInt(1, adjustmentId);
                int affectedRows = pstmt.executeUpdate();
                conn.commit();
                return affectedRows > 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void updateStockAdjustment(
            int adjustmentId,
            int productId,
            int newQuantity,
            String reason,
            String description,
            User user) throws Exception {

        int adjustedQuantity = getAdjustedQuantity(newQuantity, reason);
        String getOldAdjustmentSql = "SELECT product_id, quantity_change FROM stock_adjustments WHERE id = ?";
        // FIX: pakai NOW() untuk MariaDB
        String updateStockAdjustmentSql = "UPDATE stock_adjustments SET product_id=?, reason=?, description=?, quantity_change=?, adjust_date=NOW() WHERE id=?";
        String updateProductStockSql = "UPDATE products SET stock = stock + ? WHERE id = ?";

        try (Connection conn = DatabaseHelper.getConnection()) {
            conn.setAutoCommit(false);

            int oldProductId = -1;
            int oldQuantity = 0;

            // 1. Ambil data adjustment lama
            try (PreparedStatement ps = conn.prepareStatement(getOldAdjustmentSql)) {
                ps.setInt(1, adjustmentId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        oldProductId = rs.getInt("product_id");
                        oldQuantity = rs.getInt("quantity_change");
                    } else {
                        throw new Exception("Adjustment not found!");
                    }
                }
            }

            // 2. Kalau product_id ganti, kembalikan stok product lama, lalu kurangi/tambah ke product baru
            if (oldProductId != productId) {
                // Kembalikan stok ke product lama
                try (PreparedStatement ps = conn.prepareStatement(updateProductStockSql)) {
                    ps.setInt(1, -oldQuantity); // rollback stock lama
                    ps.setInt(2, oldProductId);
                    ps.executeUpdate();
                }
                // Adjust stok ke product baru dengan newQuantity
                try (PreparedStatement ps = conn.prepareStatement(updateProductStockSql)) {
                    ps.setInt(1, adjustedQuantity); // stock baru sesuai newQuantity
                    ps.setInt(2, productId);
                    ps.executeUpdate();
                }
            } else {
                // 3. Kalau product sama, update stok sesuai selisih quantity_change
                int selisih = adjustedQuantity - oldQuantity;
                try (PreparedStatement ps = conn.prepareStatement(updateProductStockSql)) {
                    ps.setInt(1, selisih);
                    ps.setInt(2, productId);
                    ps.executeUpdate();
                }
            }

            // 4. Update data adjustment (pakai NOW() untuk adjust_date)
            try (PreparedStatement ps = conn.prepareStatement(updateStockAdjustmentSql)) {
                ps.setInt(1, productId);
                ps.setString(2, reason);
                ps.setString(3, description);
                ps.setInt(4, adjustedQuantity);
                ps.setInt(5, adjustmentId);
                ps.executeUpdate();
            }

            conn.commit();
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Update stock adjustment failed: " + e.getMessage());
        }
    }



    public static StockAdjustment getStockAdjustmentById(int adjustmentId) {
        String sql = """
        SELECT sa.id, p.name AS product_name,p.barcode, sa.product_id, sa.reason, sa.quantity_change, sa.description, sa.adjust_date
        FROM stock_adjustments sa
        LEFT JOIN products p ON sa.product_id = p.id
        WHERE sa.id = ?
        ORDER BY sa.adjust_date DESC
    """;
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, adjustmentId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                StockAdjustment adjustment =new StockAdjustment(
                        rs.getInt("id"),
                        rs.getInt("product_id"),
                        rs.getString("barcode"),
                        rs.getString("product_name"),
                        rs.getString("reason"),
                        rs.getInt("quantity_change"),
                        rs.getString("description"),
                        rs.getString("adjust_date")
                );
                return adjustment;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean isBarcodeExist(String barcode) {
        String sql = "SELECT COUNT(*) FROM products WHERE barcode = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, barcode);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static ObservableList<Product> getFilteredProducts(String searchFilter) {
        ObservableList<Product> productList = FXCollections.observableArrayList();

        String sql = """
            SELECT p.*, pt.type_name, u.name AS unit_name
            FROM products p
            LEFT JOIN product_types pt ON p.type_id = pt.id
            LEFT JOIN units u ON p.unit_id = u.id
            WHERE p.name LIKE ? OR p.barcode LIKE ?
        """;

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String filterParam = "%" + searchFilter + "%";
            pstmt.setString(1, filterParam);
            pstmt.setString(2, filterParam);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Product product = new Product(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("stock"),
                        rs.getDouble("price"),
                        rs.getDouble("last_buy_price"),
                        rs.getDouble("discount"),
                        rs.getString("type_name"),
                        rs.getString("barcode")
                );
                product.setUnitName(rs.getString("unit_name"));

                productList.add(product);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return productList;
    }


    //load product
    public static List<Product> loadProducts(String searchFilter) {
        List<Product> list = new ArrayList<>();
        String sql = """
            SELECT p.*, pt.type_name, u.name AS unit_name
            FROM products p
            LEFT JOIN product_types pt ON p.type_id = pt.id
            LEFT JOIN units u ON p.unit_id = u.id
            WHERE p.name LIKE ? OR p.barcode LIKE ?
            ORDER BY p.id DESC
        """;
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String filterParam = "%" + searchFilter + "%";
            pstmt.setString(1, filterParam);
            pstmt.setString(2, filterParam);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Product product = new Product(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("stock"),
                        rs.getDouble("price"),
                        rs.getDouble("last_buy_price"),
                        rs.getDouble("discount"),
                        rs.getString("type_name"),
                        rs.getString("barcode")
                );
                product.setUnitName(rs.getString("unit_name"));
                list.add(product);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }


    public static boolean checkProduct(Product selected) {
        String checkSql = "SELECT COUNT(*) FROM purchase_details WHERE product_id = ?";
        boolean status = false;
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {

            checkStmt.setInt(1, selected.getId());
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next() && rs.getInt(1) > 0) {
                status = true;
                return status;
            }

        } catch (Exception e) {
            e.printStackTrace();
            MessageDialogUtil.showError("Failed to check product usage.");
        }

        return status;
    }


    // update product
    public static void updateProduct(String name,
                                     int stock,
                                     double buyPrice,
                                     double price,
                                     String updatedBy,
                                     String updatedDate,
                                     int typeId,
                                     int unitId,
                                     Product product, String barcode) {
    String sql = """
        UPDATE products
        SET name = ?, stock = ?, last_buy_price = ?, price = ?, type_id = ?, unit_id = ?, barcode = ?, updated_by = ?, updated_date = ?
        WHERE id = ?
    """;

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            pstmt.setInt(2, stock);
            pstmt.setDouble(3, buyPrice);
            pstmt.setDouble(4, price);
            pstmt.setInt(5, typeId);
            pstmt.setInt(6, unitId);
            pstmt.setString(7, barcode);
            pstmt.setString(8, updatedBy);
            pstmt.setString(9, updatedDate);
            pstmt.setInt(10, product.getId());

            pstmt.executeUpdate();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addProduct(String name, int stock, double buyPrice,double price, String createdBy, String createdDate, int typeId, String barcode, int unitId) {
        String sql = "INSERT INTO products (name, stock, last_buy_price,price, type_id, unit_id, barcode, created_by, created_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            pstmt.setInt(2, stock);
            pstmt.setDouble(3, buyPrice);
            pstmt.setDouble(4, price);
            pstmt.setInt(5, typeId);
            pstmt.setInt(6, unitId);
            pstmt.setString(7, barcode);
            pstmt.setString(8, createdBy);
            pstmt.setString(9, createdDate);
            pstmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public List<Product> searchProducts(String keyword) {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE LOWER(barcode) LIKE ? OR LOWER(name) LIKE ?";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String searchKey = "%" + keyword.toLowerCase() + "%";
            ps.setString(1, searchKey);
            ps.setString(2, searchKey);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Product p = new Product();
                p.setId(rs.getInt("id"));
                p.setBarcode(rs.getString("barcode"));
                p.setName(rs.getString("name"));
                p.setLastBuyPrice(rs.getDouble("last_buy_price"));
                p.setPrice(rs.getDouble("price"));
                p.setStock(rs.getInt("stock"));
                products.add(p);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return products;
    }
}
