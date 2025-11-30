package com.babsnet.posapp.repository;

import com.babsnet.posapp.model.Transaction;
import com.babsnet.posapp.model.TransactionDetail;
import com.babsnet.posapp.util.DatabaseHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TransactionRepository {

    public static void deleteTransactionDetail(int detailId) {
        String selectDetailSql = "SELECT product_id, qty, transaction_id FROM transaction_details WHERE id = ?";
        String selectStockSql = "SELECT stock FROM products WHERE id = ?";
        String updateStockSql = "UPDATE products SET stock = ? WHERE id = ?";
        String deleteDetailSql = "DELETE FROM transaction_details WHERE id = ?";
        Connection conn = null;
        int transactionId = 0;
        try {
            conn = DatabaseHelper.getConnection();
            conn.setAutoCommit(false);

            int productId = 0;
            int qty = 0;
            int currentStock = 0;

            // 1. Ambil product_id, qty, dan transaction_id dari detail
            try (PreparedStatement ps = conn.prepareStatement(selectDetailSql)) {
                ps.setInt(1, detailId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        productId = rs.getInt("product_id");
                        qty = rs.getInt("qty");
                        transactionId = rs.getInt("transaction_id");
                    }
                }
            }

            // 2. Ambil stock lama dari products
            if (productId > 0) {
                try (PreparedStatement ps = conn.prepareStatement(selectStockSql)) {
                    ps.setInt(1, productId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            currentStock = rs.getInt("stock");
                        }
                    }
                }
            }

            int newStock = currentStock + qty;

            // 3. Update stock produk
            if (productId > 0) {
                try (PreparedStatement ps = conn.prepareStatement(updateStockSql)) {
                    ps.setInt(1, newStock);
                    ps.setInt(2, productId);
                    ps.executeUpdate();
                }
            }

            // 4. Hapus detail transaksi
            try (PreparedStatement ps = conn.prepareStatement(deleteDetailSql)) {
                ps.setInt(1, detailId);
                ps.executeUpdate();
            }

            // 5. Update total (pakai koneksi yg sama)
            updateTransactionTotal(conn, transactionId);

            conn.commit();

        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (Exception ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (Exception e) { e.printStackTrace(); }
            }
        }
    }


    public static void updateTransactionTotal(Connection conn, int transactionId) throws SQLException {
        String sql =
                "UPDATE transactions t " +
                        "SET t.total = (" +
                        "  SELECT COALESCE(SUM(COALESCE(td.subtotal, td.qty*td.price)), 0) " +
                        "  FROM transaction_details td WHERE td.transaction_id = t.id" +
                        ") WHERE t.id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, transactionId);
            ps.executeUpdate();
        }
    }


    public Transaction getTransactionById(int transactionId) {
        String sql = "SELECT t.id, t.trans_date, t.total, t.status, t.transaction_number, u.username " +
                "FROM transactions t " +
                "LEFT JOIN users u ON t.user_id = u.id " +
                "WHERE t.id = ?";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, transactionId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Transaction transaction = new Transaction();
                transaction.setId(rs.getInt("id"));
                transaction.setTransDate(rs.getString("trans_date"));
                transaction.setTotal(rs.getDouble("total"));
                transaction.setUserName(rs.getString("username"));
                transaction.setStatus(rs.getString("status"));
                transaction.setTransactionNumber(rs.getString("transaction_number"));
                transaction.setDetails(getTransactionDetails(transactionId, conn));
                return transaction;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private List<TransactionDetail> getTransactionDetails(int transactionId, Connection conn) {
        List<TransactionDetail> details = new ArrayList<>();
        String sql = "SELECT p.name AS product_name, td.qty, td.price " +
                "FROM transaction_details td " +
                "LEFT JOIN products p ON td.product_id = p.id " +
                "WHERE td.transaction_id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, transactionId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                TransactionDetail detail = new TransactionDetail();
                detail.setProductName(rs.getString("product_name"));
                int qty = rs.getInt("qty");
                double price = rs.getDouble("price");
                double subtotal = qty * price;

                detail.setQty(qty);
                detail.setPrice(price);
                detail.setSubtotal(subtotal);

                details.add(detail);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return details;
    }


    // 1. Get all transactions (with status)
    public static List<Transaction> getAllTransactions() {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM transactions ORDER BY trans_date DESC";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Transaction(
                        rs.getInt("id"),
                        rs.getString("transaction_number"),
                        rs.getString("trans_date"),
                        rs.getString("payment_method"),
                        rs.getDouble("total"),
                        rs.getString("status")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // 2. Get transaction details by transaction id
    public static List<TransactionDetail> getTransactionDetails(int transactionId) {
        List<TransactionDetail> details = new ArrayList<>();
        String sql = """
            SELECT td.*, p.name AS product_name
            FROM transaction_details td
            LEFT JOIN products p ON td.product_id = p.id
            WHERE td.transaction_id = ?
            """;
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, transactionId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                details.add(new TransactionDetail(
                        rs.getInt("id"),
                        rs.getInt("transaction_id"),
                        rs.getInt("product_id"),
                        rs.getString("product_name"),
                        rs.getInt("qty"),
                        rs.getDouble("price"),
                        rs.getDouble("subtotal")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return details;
    }

    // 3. Cancel transaction (rollback stock + update status)
    public static void cancelTransaction(int transactionId) throws Exception {
        try (Connection conn = DatabaseHelper.getConnection()) {
            conn.setAutoCommit(false);

            // Rollback stock
            String detailSql = "SELECT product_id, qty FROM transaction_details WHERE transaction_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(detailSql)) {
                ps.setInt(1, transactionId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    int pid = rs.getInt("product_id");
                    int qty = rs.getInt("qty");
                    String updateStockSql = "UPDATE products SET stock = stock + ? WHERE id = ?";
                    try (PreparedStatement up = conn.prepareStatement(updateStockSql)) {
                        up.setInt(1, qty);
                        up.setInt(2, pid);
                        up.executeUpdate();
                    }
                }
            }

            // Update status
            String updateSql = "UPDATE transactions SET status = 'CANCELLED' WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setInt(1, transactionId);
                ps.executeUpdate();
            }
            conn.commit();
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Cancel transaction failed: " + e.getMessage());
        }
    }

    public static void updateTransactionDetail(int detailId, int qty, double price, double subtotal) throws Exception {
        String selectDetailSql = "SELECT product_id, qty, transaction_id FROM transaction_details WHERE id = ?";
        String selectStockSql = "SELECT stock FROM products WHERE id = ?";
        String updateStockSql = "UPDATE products SET stock = ? WHERE id = ?";
        String updateDetailSql = "UPDATE transaction_details SET qty=?, price=?, subtotal=? WHERE id=?";
        Connection conn = null;
        try {
            conn = DatabaseHelper.getConnection();
            conn.setAutoCommit(false);

            int productId = 0;
            int oldQty = 0;
            int transactionId = 0;
            int currentStock = 0;

            // 1. Ambil data lama (product_id, qty, transaction_id)
            try (PreparedStatement ps = conn.prepareStatement(selectDetailSql)) {
                ps.setInt(1, detailId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        productId = rs.getInt("product_id");
                        oldQty = rs.getInt("qty");
                        transactionId = rs.getInt("transaction_id");
                    }
                }
            }

            // 2. Ambil stock lama dari products
            if (productId > 0) {
                try (PreparedStatement ps = conn.prepareStatement(selectStockSql)) {
                    ps.setInt(1, productId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            currentStock = rs.getInt("stock");
                        }
                    }
                }
            }

            // 3. Hitung selisih qty (new - old)
            int diffQty = qty - oldQty;
            int newStock = currentStock - diffQty; // Jika diffQty positif, stock dikurangi; jika negatif, stock bertambah

            // 4. Update stock produk
            if (productId > 0) {
                try (PreparedStatement ps = conn.prepareStatement(updateStockSql)) {
                    ps.setInt(1, newStock);
                    ps.setInt(2, productId);
                    ps.executeUpdate();
                }
            }

            // 5. Update detail transaksi
            try (PreparedStatement ps = conn.prepareStatement(updateDetailSql)) {
                ps.setInt(1, qty);
                ps.setDouble(2, price);
                ps.setDouble(3, subtotal);
                ps.setInt(4, detailId);
                ps.executeUpdate();
            }

            // 6. Update total transaksi dengan conn yang sama
            updateTransactionTotal(conn, transactionId);

            conn.commit();

        } catch (Exception e) {
            if (conn != null) try { conn.rollback(); } catch (Exception ex) { ex.printStackTrace(); }
            throw e;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }



}
