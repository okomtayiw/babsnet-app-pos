package com.babsnet.posapp.repository;

import com.babsnet.posapp.model.UnitProduct;
import com.babsnet.posapp.util.DatabaseHelper;
import com.babsnet.posapp.util.MessageDialogUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UnitProductRepository {

    public void loadUnitProduct(ComboBox<String> unitComboBox) {
        ObservableList<String> units = FXCollections.observableArrayList();
        String sql = "SELECT name FROM units";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                units.add(rs.getString("name"));
            }

            unitComboBox.setItems(units);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveUnit(String unitName) {
        if(unitName.isEmpty()) {
            MessageDialogUtil.showWarning("Name unit does not empty!");
            return;
        }
        String sql = "INSERT INTO units (name) VALUES (?)";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, unitName);
            pstmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Ambil semua units
    public ObservableList<UnitProduct> getAllUnits() {
        ObservableList<UnitProduct> list = FXCollections.observableArrayList();
        String sql = "SELECT * FROM units";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(new UnitProduct(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("abbreviation")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // Insert
    public boolean insertUnit(UnitProduct unit) {
        String sql = "INSERT INTO units (name, abbreviation) VALUES (?, ?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, unit.getName());
            stmt.setString(2, unit.getAbbreviation());
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Update
    public boolean updateUnit(UnitProduct unit) {
        String sql = "UPDATE units SET name=?, abbreviation=? WHERE id=?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, unit.getName());
            stmt.setString(2, unit.getAbbreviation());
            stmt.setInt(3, unit.getId());
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Cek apakah nama sudah dipakai oleh id lain
    public boolean unitNameExistsForOtherId(String name, int exceptId) {
        String sql = "SELECT COUNT(*) FROM units WHERE name = ? AND id <> ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setInt(2, exceptId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    // Delete
    public boolean deleteUnit(int id) {
        String sql = "DELETE FROM units WHERE id=?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Cek nama sudah exist
    public boolean unitNameExists(String name) {
        String sql = "SELECT COUNT(*) FROM units WHERE name = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // Cek apakah unit sedang digunakan di produk
    public boolean isUnitUsed(String unitName) {
        String sql = "SELECT COUNT(*) FROM products WHERE unit = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, unitName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}
