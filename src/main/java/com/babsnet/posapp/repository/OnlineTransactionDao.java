package com.babsnet.posapp.repository;


import com.babsnet.posapp.model.*;
import com.babsnet.posapp.util.DatabaseHelper;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class OnlineTransactionDao {


    // ========= INSERT =========
    public long insert(OnlineTransaction tx, List<OnlineTransactionDetail> details) throws SQLException {
        String sqlHdr = """
            INSERT INTO online_transactions
              (transaction_number, trans_date, payment_method, status, user_id, total,
               provider, category, product_code, customer_id, ref_id, provider_tr_id, sn, rc, provider_message, provider_status,
               denom, buy_price, sell_price, fee, margin, inquiry_json, last_response_json)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;

        String sqlDtl = """
            INSERT INTO online_transaction_details
              (online_transaction_id, product_code, description, qty, unit_price, subtotal)
            VALUES (?,?,?,?,?,?)
            """;

        try (Connection c = DatabaseHelper.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(sqlHdr, Statement.RETURN_GENERATED_KEYS)) {

                // default transDate & total
                if (tx.transDate == null) tx.transDate = LocalDateTime.now();
                if (tx.total == null) tx.total = tx.sellPrice != null ? tx.sellPrice : java.math.BigDecimal.ZERO;
                if (tx.transactionNumber == null) tx.transactionNumber = genNumber();

                ps.setString(1,  tx.transactionNumber);
                ps.setTimestamp(2, Timestamp.valueOf(tx.transDate));
                ps.setString(3,  tx.paymentMethod);
                ps.setString(4,  tx.status.name());
                ps.setLong(5,    tx.userId);
                ps.setBigDecimal(6, tx.total);

                ps.setString(7,  tx.provider);
                ps.setString(8,  tx.category);
                ps.setString(9,  tx.productCode);
                ps.setString(10, tx.customerId);
                ps.setString(11, tx.refId);
                if (tx.providerTrId != null) ps.setLong(12, tx.providerTrId); else ps.setNull(12, Types.BIGINT);
                ps.setString(13, tx.sn);
                ps.setString(14, tx.rc);
                ps.setString(15, tx.providerMessage);
                if (tx.providerStatus != null) ps.setInt(16, tx.providerStatus); else ps.setNull(16, Types.TINYINT);

                if (tx.denom != null) ps.setLong(17, tx.denom); else ps.setNull(17, Types.BIGINT);
                if (tx.buyPrice != null) ps.setBigDecimal(18, tx.buyPrice); else ps.setNull(18, Types.DECIMAL);
                if (tx.sellPrice != null) ps.setBigDecimal(19, tx.sellPrice); else ps.setNull(19, Types.DECIMAL);
                if (tx.fee != null) ps.setBigDecimal(20, tx.fee); else ps.setNull(20, Types.DECIMAL);
                if (tx.margin != null) ps.setBigDecimal(21, tx.margin); else ps.setNull(21, Types.DECIMAL);
                if (tx.inquiryJson != null) ps.setString(22, tx.inquiryJson); else ps.setNull(22, Types.LONGVARCHAR);
                if (tx.lastResponseJson != null) ps.setString(23, tx.lastResponseJson); else ps.setNull(23, Types.LONGVARCHAR);

                ps.executeUpdate();
                long id;
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (!rs.next()) throw new SQLException("no generated key");
                    id = rs.getLong(1);
                }

                if (details != null && !details.isEmpty()) {
                    try (PreparedStatement pd = c.prepareStatement(sqlDtl)) {
                        for (OnlineTransactionDetail d : details) {
                            pd.setLong(1, id);
                            pd.setString(2, d.productCode);
                            pd.setString(3, d.description);
                            pd.setInt(4, d.qty);
                            pd.setBigDecimal(5, d.unitPrice);
                            pd.setBigDecimal(6, d.subtotal);
                            pd.addBatch();
                        }
                        pd.executeBatch();
                    }
                }
                c.commit();
                return id;
            } catch (Exception ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    // ========= GET =========
    public Optional<OnlineTransaction> findById(long id) throws SQLException {
        String qHdr = "SELECT * FROM online_transactions WHERE id=?";
        String qDtl = "SELECT * FROM online_transaction_details WHERE online_transaction_id=? ORDER BY id";
        try (Connection c = DatabaseHelper.getConnection();
             PreparedStatement ph = c.prepareStatement(qHdr);
             PreparedStatement pd = c.prepareStatement(qDtl)) {

            ph.setLong(1, id);
            try (ResultSet rs = ph.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                OnlineTransaction tx = mapHdr(rs);

                pd.setLong(1, id);
                try (ResultSet rd = pd.executeQuery()) {
                    while (rd.next()) tx.details.add(mapDtl(rd));
                }
                return Optional.of(tx);
            }
        }
    }

    public Optional<OnlineTransaction> findByRefId(String refId) throws SQLException {
        String q = "SELECT id FROM online_transactions WHERE ref_id=?";
        try (Connection c = DatabaseHelper.getConnection();
             PreparedStatement ps = c.prepareStatement(q)) {
            ps.setString(1, refId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return findById(rs.getLong(1));
            }
        }
    }

    // ========= UPDATE (setelah provider response/polling) =========
    public void updateAfterProviderResponse(
            long id,
            String rc,
            Integer providerStatus,
            String providerMessage,
            Long providerTrId,
            String sn,
            String lastResponseJson,
            OnlineTxnStatus newStatus
    ) throws SQLException {
        String sql = """
            UPDATE online_transactions
               SET rc=?, provider_status=?, provider_message=?, provider_tr_id=?,
                   sn=?, last_response_json=?, status=?, trans_date=trans_date
             WHERE id=?
            """;
        try (Connection c = DatabaseHelper.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, rc);
            if (providerStatus != null) ps.setInt(2, providerStatus); else ps.setNull(2, Types.TINYINT);
            ps.setString(3, providerMessage);
            if (providerTrId != null) ps.setLong(4, providerTrId); else ps.setNull(4, Types.BIGINT);
            ps.setString(5, sn);
            if (lastResponseJson != null) ps.setString(6, lastResponseJson); else ps.setNull(6, Types.LONGVARCHAR);
            ps.setString(7, newStatus.name());
            ps.setLong(8, id);
            ps.executeUpdate();
        }
    }


    public Integer deleteCascade(long id) throws SQLException {
        final String d1 = "DELETE FROM online_transaction_details WHERE online_transaction_id = ?";
        final String d2 = "DELETE FROM online_transactions WHERE id = ? AND UPPER(status) = 'FAILED'";

        int detailsAffected = 0;
        int headerAffected  = 0;

        try (Connection c = DatabaseHelper.getConnection()) {
            boolean origAuto = c.getAutoCommit();
            c.setAutoCommit(false);
            try (PreparedStatement p1 = c.prepareStatement(d1);
                 PreparedStatement p2 = c.prepareStatement(d2)) {

                p1.setLong(1, id);
                detailsAffected = p1.executeUpdate();

                p2.setLong(1, id);
                headerAffected = p2.executeUpdate();

                c.commit();
            } catch (Exception ex) {
                try { c.rollback(); } catch (SQLException ignore) {}
                if (ex instanceof SQLException se) throw se;
                throw new SQLException("Gagal delete cascade", ex);
            } finally {
                try { c.setAutoCommit(origAuto); } catch (SQLException ignore) {}
            }
        }

        return detailsAffected + headerAffected;
    }


    // ========= Mappers & utils =========
    private OnlineTransaction mapHdr(ResultSet rs) throws SQLException {
        OnlineTransaction t = new OnlineTransaction();
        t.id = rs.getLong("id");
        t.transactionNumber = rs.getString("transaction_number");
        t.transDate = rs.getTimestamp("trans_date").toLocalDateTime();
        t.paymentMethod = rs.getString("payment_method");
        t.status = OnlineTxnStatus.valueOf(rs.getString("status"));
        t.userId = rs.getLong("user_id");
        t.total = rs.getBigDecimal("total");

        t.provider = rs.getString("provider");
        t.category = rs.getString("category");
        t.productCode = rs.getString("product_code");
        t.customerId = rs.getString("customer_id");
        t.refId = rs.getString("ref_id");
        long tr = rs.getLong("provider_tr_id");
        t.providerTrId = rs.wasNull() ? null : tr;
        t.sn = rs.getString("sn");
        t.rc = rs.getString("rc");
        t.providerMessage = rs.getString("provider_message");
        int ps = rs.getInt("provider_status");
        t.providerStatus = rs.wasNull() ? null : ps;

        long denom = rs.getLong("denom");
        t.denom = rs.wasNull() ? null : denom;
        t.buyPrice = rs.getBigDecimal("buy_price");
        t.sellPrice = rs.getBigDecimal("sell_price");
        t.fee = rs.getBigDecimal("fee");
        t.margin = rs.getBigDecimal("margin");
        t.inquiryJson = rs.getString("inquiry_json");
        t.lastResponseJson = rs.getString("last_response_json");
        return t;
    }

    private OnlineTransactionDetail mapDtl(ResultSet rs) throws SQLException {
        OnlineTransactionDetail d = new OnlineTransactionDetail();
        d.id = rs.getLong("id");
        d.onlineTransactionId = rs.getLong("online_transaction_id");
        d.productCode = rs.getString("product_code");
        d.description = rs.getString("description");
        d.qty = rs.getInt("qty");
        d.unitPrice = rs.getBigDecimal("unit_price");
        d.subtotal = rs.getBigDecimal("subtotal");
        return d;
    }

    private String genNumber() {
        // generator sederhana: KIA-YYYYMMDD-HHMMSS-XXXX
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        String ts = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        int rnd = (int)(Math.random()*9000)+1000;
        return "KIA-" + ts + "-" + rnd;
    }


    public List<OnlineTxnJoined> listJoined(int limit) throws java.sql.SQLException {
        String sql = """
        SELECT
          ot.id, ot.transaction_number, ot.trans_date, ot.status, ot.customer_id, ot.category,
          ot.ref_id, ot.rc, ot.provider_message,
          otd.product_code, otd.description, otd.qty, otd.unit_price, otd.subtotal
        FROM online_transactions ot
        LEFT JOIN online_transaction_details otd ON otd.online_transaction_id = ot.id
        ORDER BY ot.trans_date DESC
        LIMIT ?
        """;
        List<OnlineTxnJoined> out = new ArrayList<>();
        try (java.sql.Connection c = DatabaseHelper.getConnection();
             java.sql.PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit <= 0 ? 200 : limit);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    com.babsnet.posapp.model.OnlineTxnJoined r = new com.babsnet.posapp.model.OnlineTxnJoined();
                    r.setId(rs.getLong("id"));
                    r.setTransactionNumber(rs.getString("transaction_number"));
                    java.sql.Timestamp ts = rs.getTimestamp("trans_date");
                    r.setTransDate(ts == null ? null : ts.toLocalDateTime());
                    r.setStatus(rs.getString("status"));
                    r.setCustomerId(rs.getString("customer_id"));
                    r.setCategory(rs.getString("category"));
                    r.setRefId(rs.getString("ref_id"));
                    r.setRc(rs.getString("rc"));
                    r.setProviderMessage(rs.getString("provider_message"));

                    r.setProductCode(rs.getString("product_code"));
                    r.setDescription(rs.getString("description"));
                    r.setQty(rs.getInt("qty"));
                    if (rs.wasNull()) r.setQty(0);
                    r.setUnitPrice(rs.getBigDecimal("unit_price"));
                    r.setSubtotal(rs.getBigDecimal("subtotal"));

                    out.add(r);
                }
            }
        }
        return out;
    }

    public List<ReportRow> listReportRowsByDateRange(LocalDateTime start, LocalDateTime endExclusive) throws SQLException {
        String sql = """
        SELECT 
            ot.id,
            ot.trans_date,
            ot.status,
            ot.customer_id,
            ot.ref_id,
            ot.rc,
            ot.provider_message,
            ot.category,
            ot.product_code,
            od.description,
            ot.denom,
            ot.buy_price,
            od.unit_price
        FROM online_transactions ot
        LEFT JOIN online_transaction_details od ON od.online_transaction_id = ot.id
        WHERE ot.trans_date >= ? AND ot.trans_date < ?
        ORDER BY ot.trans_date DESC, ot.id DESC
        """;

        List<ReportRow> rows = new ArrayList<>();
        try (Connection c = DatabaseHelper.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(start));
            ps.setTimestamp(2, Timestamp.valueOf(endExclusive));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Long id = rs.getLong("id");
                    Timestamp ts = rs.getTimestamp("trans_date");
                    LocalDateTime transDate = (ts != null) ? ts.toLocalDateTime() : null;

                    rows.add(new ReportRow(
                            id,
                            transDate,
                            rs.getString("status"),
                            rs.getString("customer_id"),
                            rs.getString("ref_id"),
                            rs.getString("rc"),
                            rs.getString("provider_message"),
                            rs.getString("category"),
                            rs.getString("product_code"),
                            rs.getString("description"),
                            rs.getBigDecimal("denom"),
                            rs.getBigDecimal("buy_price"),
                            rs.getBigDecimal("unit_price")
                    ));
                }
            }
        }
        return rows;
    }


}

