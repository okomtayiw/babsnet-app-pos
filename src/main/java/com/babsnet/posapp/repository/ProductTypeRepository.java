package com.babsnet.posapp.repository;

import com.babsnet.posapp.model.ProductType;
import com.babsnet.posapp.util.DatabaseHelper;
import com.babsnet.posapp.util.MessageDialogUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ProductTypeRepository {

    public static List<ProductType> getAll() {
        List<ProductType> list = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM product_types")) {
            while (rs.next()) {
                list.add(new ProductType(
                        rs.getInt("id"),
                        rs.getString("type_name")
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public static boolean insert(ProductType type) {
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO product_types (type_name) VALUES (?)")) {
            ps.setString(1, type.getTypeName());
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    public static boolean isTypeNameExist(String typeName) {
        String sql = "SELECT COUNT(*) FROM product_types WHERE type_name = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, typeName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isProductTypeUsed(int typeId) {
        String sql = "SELECT COUNT(*) FROM products WHERE type_id = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, typeId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }



    public static boolean update(ProductType type) {
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE product_types SET type_name=? WHERE id=?")) {
            ps.setString(1, type.getTypeName());
            ps.setInt(2, type.getId());
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    public static boolean typeNameExistsForOtherId(String typeName, int exceptId) {
        String sql = "SELECT COUNT(*) FROM product_types WHERE type_name = ? AND id <> ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, typeName);
            pstmt.setInt(2, exceptId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    public static boolean delete(int id) {
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM product_types WHERE id=?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    public int getTypeIdByName(String typeName) {
        String sql = "SELECT id FROM product_types WHERE type_name = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, typeName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }


    public void loadProductTypes(ComboBox<String> typeComboBox) {
        ObservableList<String> productTypes = FXCollections.observableArrayList();
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT type_name FROM product_types");
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                productTypes.add(rs.getString("type_name"));
            }

            typeComboBox.setItems(productTypes);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
