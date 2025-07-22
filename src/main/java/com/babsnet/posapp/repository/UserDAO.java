package com.babsnet.posapp.repository;

import com.babsnet.posapp.util.DatabaseHelper;
import com.babsnet.posapp.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class UserDAO {

    public static void insertDefaultUser() {
        String defaultUsername = "admin";
        String defaultPassword = "12345678"; // default password
        String hashedPassword = PasswordUtil.hashPassword(defaultPassword);

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO users (username, password, first_name, last_name, phone_number, email, role) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                             "ON DUPLICATE KEY UPDATE username = username")) {

            pstmt.setString(1, defaultUsername);
            pstmt.setString(2, hashedPassword);
            pstmt.setString(3, "Admin");
            pstmt.setString(4, "");
            pstmt.setString(5, "");
            pstmt.setString(6, "");
            pstmt.setString(7, "ADMIN");

            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}

