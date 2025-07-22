package com.babsnet.posapp.repository;


import com.babsnet.posapp.model.*;
import com.babsnet.posapp.util.DatabaseHelper;
import com.babsnet.posapp.util.DateUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PurchaseDAO {

    public static int insertPurchase(String purchaseNumber, LocalDate purchaseDate, int supplierId, double total, User user) throws Exception {
        String sql = "INSERT INTO purchases (purchase_number, purchase_date, supplier_id, total, user_id) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, purchaseNumber);
            pstmt.setString(2, purchaseDate.toString());
            pstmt.setInt(3, supplierId);
            pstmt.setDouble(4, total);
            pstmt.setInt(5, user.getId());

            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1); // return purchase ID
                } else {
                    throw new Exception("Failed to get purchase ID.");
                }
            }
        }
    }

    public static void insertPurchaseDetails(int purchaseId, List<PurchaseItem> items) throws Exception {
        String sql = "INSERT INTO purchase_details (purchase_id, product_id, quantity, buy_price, subtotal) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (PurchaseItem item : items) {

                pstmt.setInt(1, purchaseId);
                pstmt.setInt(2, item.getProduct().getId());
                pstmt.setInt(3, item.getQuantity());
                pstmt.setDouble(4, item.getBuyPrice());
                pstmt.setDouble(5, item.getSubTotal());
                pstmt.addBatch();

                ProductRepository.addStock(conn,item.getProduct().getId(), item.getQuantity());
            }
            pstmt.executeBatch();
        }
    }


    public static List<Purchase> getAllPurchases() {
        List<Purchase> purchases = new ArrayList<>();
        String sql = "SELECT p.id, p.purchase_number, p.purchase_date, s.name AS supplier_name, p.total, p.status FROM purchases p LEFT JOIN suppliers s ON p.supplier_id = s.id ORDER BY p.id DESC";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Purchase purchase = new Purchase(
                        rs.getInt("id"),
                        rs.getString("purchase_number"),
                        DateUtil.parseDBDate(rs.getString("purchase_date")),
                        rs.getString("supplier_name"),
                        rs.getDouble("total"),
                        rs.getString("status")
                );
                purchases.add(purchase);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return purchases;
    }

    public static void cancelAndRollback(int purchaseId) {
        try (Connection conn = DatabaseHelper.getConnection()) {
            conn.setAutoCommit(false);


            String checkSql = "SELECT status FROM purchases WHERE id = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, purchaseId);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && "CANCELLED".equalsIgnoreCase(rs.getString("status"))) {
                    System.out.println("Purchase already cancelled. Skipping rollback.");
                    conn.rollback();
                    return;
                }
            }

            // Rollback stock
            String detailsSql = "SELECT product_id, quantity FROM purchase_details WHERE purchase_id = ?";
            try (PreparedStatement detailsStmt = conn.prepareStatement(detailsSql)) {
                detailsStmt.setInt(1, purchaseId);
                ResultSet rs = detailsStmt.executeQuery();

                while (rs.next()) {
                    int productId = rs.getInt("product_id");
                    int qty = rs.getInt("quantity");
                    ProductRepository.reduceStock(conn, productId, qty);
                }
            }


            String cancelSql = "UPDATE purchases SET status = 'CANCELLED' WHERE id = ?";
            try (PreparedStatement cancelStmt = conn.prepareStatement(cancelSql)) {
                cancelStmt.setInt(1, purchaseId);
                cancelStmt.executeUpdate();
            }

            conn.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public static void updatePurchase(int purchaseId, String purchaseNumber, LocalDate purchaseDate, String supplierName, List<PurchaseItem> items) throws Exception {
        try (Connection conn = DatabaseHelper.getConnection()) {
            conn.setAutoCommit(false);

            // Get supplier ID
            String supplierSql = "SELECT id FROM suppliers WHERE name = ?";
            int supplierId;
            try (PreparedStatement supplierStmt = conn.prepareStatement(supplierSql)) {
                supplierStmt.setString(1, supplierName);
                ResultSet rs = supplierStmt.executeQuery();
                if (rs.next()) {
                    supplierId = rs.getInt("id");
                } else {
                    throw new Exception("Supplier not found");
                }
            }

            // Get current purchase status
            String statusSql = "SELECT status FROM purchases WHERE id = ?";
            String currentStatus = "ACTIVE";
            try (PreparedStatement statusStmt = conn.prepareStatement(statusSql)) {
                statusStmt.setInt(1, purchaseId);
                ResultSet rs = statusStmt.executeQuery();
                if (rs.next()) {
                    currentStatus = rs.getString("status");
                }
            }

            // Update purchase header (always set to ACTIVE when updating)
            String updatePurchaseSql = "UPDATE purchases SET purchase_number = ?, purchase_date = ?, supplier_id = ?, status = 'ACTIVE' WHERE id = ?";
            try (PreparedStatement updateStmt = conn.prepareStatement(updatePurchaseSql)) {
                updateStmt.setString(1, purchaseNumber);
                updateStmt.setString(2, purchaseDate.toString());
                updateStmt.setInt(3, supplierId);
                updateStmt.setInt(4, purchaseId);
                updateStmt.executeUpdate();
            }

            // Rollback stock and delete old details in one flow
            String selectAndDeleteSql = "SELECT product_id, quantity FROM purchase_details WHERE purchase_id = ?";
            try (PreparedStatement selectStmt = conn.prepareStatement(selectAndDeleteSql)) {
                selectStmt.setInt(1, purchaseId);
                ResultSet rs = selectStmt.executeQuery();

                List<Integer> productIds = new ArrayList<>();
                List<Integer> quantities = new ArrayList<>();

                while (rs.next()) {
                    productIds.add(rs.getInt("product_id"));
                    quantities.add(rs.getInt("quantity"));
                }

                if (!"CANCELLED".equalsIgnoreCase(currentStatus)) {
                    // Jika status sebelumnya bukan CANCELLED, rollback stock dulu
                    for (int i = 0; i < productIds.size(); i++) {
                        ProductRepository.reduceStock(conn, productIds.get(i), quantities.get(i));
                    }
                }

            }

            String deleteSql = "DELETE FROM purchase_details WHERE purchase_id = ?";
            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                deleteStmt.setInt(1, purchaseId);
                deleteStmt.executeUpdate();
            }

            // Insert new details and add stock
            String insertSql = "INSERT INTO purchase_details (purchase_id, product_id, quantity, buy_price, subtotal) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                for (PurchaseItem item : items) {
                    insertStmt.setInt(1, purchaseId);
                    insertStmt.setInt(2, item.getProduct().getId());
                    insertStmt.setInt(3, item.getQuantity());
                    insertStmt.setDouble(4, item.getBuyPrice());
                    insertStmt.setDouble(5, item.getSubTotal());
                    insertStmt.addBatch();

                    ProductRepository.addStock(conn, item.getProduct().getId(), item.getQuantity());
                }
                insertStmt.executeBatch();
            }

            conn.commit();
        }
    }





    public static List<PurchaseItem> getPurchaseDetails(int purchaseId) {
        List<PurchaseItem> purchaseItems = new ArrayList<>();

        String sql = """
        SELECT pd.product_id, p.name AS product_name, p.barcode AS barcode,
               pd.quantity, pd.buy_price
        FROM purchase_details pd
        JOIN products p ON pd.product_id = p.id
        WHERE pd.purchase_id = ?
        """;

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, purchaseId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Product product = new Product();
                product.setId(rs.getInt("product_id"));
                product.setName(rs.getString("product_name"));
                product.setBarcode(rs.getString("barcode"));

                PurchaseItem item = new PurchaseItem();
                item.setProduct(product);
                item.setQuantity(rs.getInt("quantity"));
                item.setBuyPrice(rs.getDouble("buy_price"));
                item.setBarcode(rs.getString("barcode")); // optional
                item.setSubTotal(rs.getInt("quantity") * rs.getDouble("buy_price"));

                purchaseItems.add(item);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return purchaseItems;
    }


}
