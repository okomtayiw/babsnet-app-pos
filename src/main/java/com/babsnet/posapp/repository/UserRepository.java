package com.babsnet.posapp.repository;

import com.babsnet.posapp.model.User;
import com.babsnet.posapp.util.DatabaseHelper;
import com.babsnet.posapp.util.PasswordUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserRepository {

    public User validateUser(String username, String plainPassword) {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String hashFromDb = rs.getString("password");
                // Cocokkan password plain user dengan hash di DB pakai BCrypt
                if (PasswordUtil.checkPassword(plainPassword, hashFromDb)) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setUsername(rs.getString("username"));
                    user.setFirstName(rs.getString("first_name"));
                    user.setLastName(rs.getString("last_name"));
                    user.setPhoneNumber(rs.getString("phone_number"));
                    user.setEmail(rs.getString("email"));
                    user.setRole(rs.getString("role"));
                    // Tidak perlu set password ke object User demi keamanan!
                    return user;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM users ORDER BY id DESC")) {
            while (rs.next()) {
                User u = new User();
                u.setId(rs.getInt("id"));
                u.setUsername(rs.getString("username"));
                u.setPassword(rs.getString("password"));
                u.setFirstName(rs.getString("first_name"));
                u.setLastName(rs.getString("last_name"));
                u.setPhoneNumber(rs.getString("phone_number"));
                u.setEmail(rs.getString("email"));
                u.setRole(rs.getString("role"));
                list.add(u);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean insert(User user) {
        String sql = "INSERT INTO users (username, password, first_name, last_name, phone_number, email, role) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getFirstName());
            ps.setString(4, user.getLastName());
            ps.setString(5, user.getPhoneNumber());
            ps.setString(6, user.getEmail());
            ps.setString(7, user.getRole());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean update(User user) {
        String sql = "UPDATE users SET password=?, first_name=?, last_name=?, phone_number=?, email=?, role=? WHERE username=?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getPassword());
            ps.setString(2, user.getFirstName());
            ps.setString(3, user.getLastName());
            ps.setString(4, user.getPhoneNumber());
            ps.setString(5, user.getEmail());
            ps.setString(6, user.getRole());
            ps.setString(7, user.getUsername());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean delete(int userId) {
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE id=?")) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("phone_number"),
                        rs.getString("email"),
                        rs.getString("role")
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


}
