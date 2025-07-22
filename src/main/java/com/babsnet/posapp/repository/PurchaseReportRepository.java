package com.babsnet.posapp.repository;

import com.babsnet.posapp.model.PurchaseDetailReportRow;
import com.babsnet.posapp.util.DatabaseHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PurchaseReportRepository {

    public List<PurchaseDetailReportRow> findPurchaseDetailsReport(LocalDate start, LocalDate end) {
        List<PurchaseDetailReportRow> rows = new ArrayList<>();
        String sql = """
            SELECT
                  p.purchase_date,
                  p.purchase_number,
                  s.name AS supplier_name,
                  d.quantity AS qty,
                  d.buy_price,
                  d.subtotal,
                  pr.name AS product_name,
                  u.username AS user_name
            FROM purchases p
            JOIN purchase_details d ON p.id = d.purchase_id
            LEFT JOIN products pr ON d.product_id = pr.id
            LEFT JOIN suppliers s ON p.supplier_id = s.id
            LEFT JOIN users u ON p.user_id = u.id
            WHERE DATE(p.purchase_date) BETWEEN ? AND ?
            ORDER BY p.purchase_date DESC
            """;
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, start.toString());
            stmt.setString(2, end.toString());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                PurchaseDetailReportRow row = new PurchaseDetailReportRow();
                row.setPurchaseDate(rs.getString("purchase_date"));
                row.setPurchaseNumber(rs.getString("purchase_number"));
                row.setSupplierName(rs.getString("supplier_name"));
                row.setProductName(rs.getString("product_name"));
                row.setQty(rs.getInt("qty"));
                row.setBuyPrice(rs.getDouble("buy_price"));
                row.setSubtotal(rs.getDouble("subtotal"));
                row.setUserName(rs.getString("user_name"));
                rows.add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rows;
    }
}
