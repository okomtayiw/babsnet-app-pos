package com.babsnet.posapp.repository;

import com.babsnet.posapp.model.TransactionDetailReportRow;
import com.babsnet.posapp.util.DatabaseHelper;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TransactionReportRepository {

    public List<TransactionDetailReportRow> findTransactionDetailsReport(LocalDate start, LocalDate end) {
        List<TransactionDetailReportRow> report = new ArrayList<>();
        String sql = """
                SELECT
                    t.trans_date,
                    t.transaction_number,
                    p.name as product_name,
                    d.qty,
                    d.price,
                    d.subtotal,
                    (
                        SELECT pd.buy_price
                        FROM purchase_details pd
                        JOIN purchases pur ON pd.purchase_id = pur.id
                        WHERE pd.product_id = d.product_id
                        ORDER BY date(pur.purchase_date) DESC
                        LIMIT 1
                    ) AS buy_price,
                    t.payment_method,
                    u.username as user_name
                FROM transaction_details d
                JOIN transactions t ON d.transaction_id = t.id
                JOIN products p ON d.product_id = p.id
                LEFT JOIN users u ON t.user_id = u.id
                WHERE date(t.trans_date) >= ? AND date(t.trans_date) <= ?
                AND t.status = 'ACTIVE'
                ORDER BY t.trans_date, t.transaction_number, d.id
            """;
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, start.toString());
            ps.setString(2, end.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TransactionDetailReportRow row = new TransactionDetailReportRow(
                            rs.getString("trans_date"),
                            rs.getString("transaction_number"),
                            rs.getString("product_name"),
                            rs.getInt("qty"),
                            rs.getDouble("buy_price"),
                            rs.getDouble("price"),
                            rs.getDouble("subtotal"),
                            rs.getString("payment_method"),
                            rs.getString("user_name")
                    );
                    report.add(row);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return report;
    }
}
