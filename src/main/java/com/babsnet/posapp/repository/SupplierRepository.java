package com.babsnet.posapp.repository;

import com.babsnet.posapp.model.Supplier;
import com.babsnet.posapp.util.DatabaseHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SupplierRepository {
    public static List<Supplier> getAllSuppliers() {
        List<Supplier> list = new ArrayList<>();
        String sql = "SELECT * FROM suppliers ORDER BY id ASC";
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Supplier s = new Supplier();
                s.setId(rs.getInt("id"));
                s.setName(rs.getString("name"));
                s.setAddress(rs.getString("address"));
                s.setPhone(rs.getString("phone"));
                s.setEmail(rs.getString("email"));
                s.setNotes(rs.getString("notes"));
                list.add(s);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static Supplier getSupplierById(int id) {
        String sql = "SELECT * FROM suppliers WHERE id = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Supplier s = new Supplier();
                    s.setId(rs.getInt("id"));
                    s.setName(rs.getString("name"));
                    s.setAddress(rs.getString("address"));
                    s.setPhone(rs.getString("phone"));
                    s.setEmail(rs.getString("email"));
                    s.setNotes(rs.getString("notes"));
                    return s;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean insertSupplier(Supplier s) {
        String sql = "INSERT INTO suppliers (name, address, phone, email, notes) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, s.getName());
            pstmt.setString(2, s.getAddress());
            pstmt.setString(3, s.getPhone());
            pstmt.setString(4, s.getEmail());
            pstmt.setString(5, s.getNotes());
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean updateSupplier(Supplier s) {
        String sql = "UPDATE suppliers SET name = ?, address = ?, phone = ?, email = ?, notes = ? WHERE id = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, s.getName());
            pstmt.setString(2, s.getAddress());
            pstmt.setString(3, s.getPhone());
            pstmt.setString(4, s.getEmail());
            pstmt.setString(5, s.getNotes());
            pstmt.setInt(6, s.getId());
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean deleteSupplier(int id) {
        String sql = "DELETE FROM suppliers WHERE id = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
