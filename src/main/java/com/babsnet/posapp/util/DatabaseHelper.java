package com.babsnet.posapp.util;

import com.babsnet.posapp.repository.UserDAO;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseHelper {


    private static final String DB_URL = ConfigUtil.get("db.url");
    private static final String DB_USER = ConfigUtil.get("db.user");
    private static final String DB_PASS = ConfigUtil.get("db.pass");


    static {
        try {
            createTables();
            UserDAO.insertDefaultUser();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }
    public static void createTables() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            String createUserTable = """
        CREATE TABLE IF NOT EXISTS users (
            id INT AUTO_INCREMENT PRIMARY KEY,
            username VARCHAR(100) NOT NULL UNIQUE,
            password VARCHAR(255) NOT NULL,
            first_name VARCHAR(100),
            last_name VARCHAR(100),
            phone_number VARCHAR(30),
            email VARCHAR(100),
            role VARCHAR(50)
        )
        """;

            String createProductTypeTable = """
        CREATE TABLE IF NOT EXISTS product_types (
            id INT AUTO_INCREMENT PRIMARY KEY,
            type_name VARCHAR(100) NOT NULL UNIQUE
        )
        """;

            String createTableUnits = """
        CREATE TABLE IF NOT EXISTS units (
            id INT AUTO_INCREMENT PRIMARY KEY,
            name VARCHAR(50) NOT NULL UNIQUE,
            abbreviation VARCHAR(20)
        )
        """;

            String createProductsTable = """
        CREATE TABLE IF NOT EXISTS products (
            id INT AUTO_INCREMENT PRIMARY KEY,
            name VARCHAR(255) NOT NULL,
            stock INT NOT NULL,
            price DECIMAL(12,2) DEFAULT 0,
            discount DECIMAL(12,2) DEFAULT 0,
            barcode VARCHAR(100),
            unit_id INT,
            description TEXT,
            type_id INT,
            created_by VARCHAR(100),
            updated_by VARCHAR(100),
            created_date DATETIME,
            updated_date DATETIME,
            FOREIGN KEY (type_id) REFERENCES product_types(id),
            FOREIGN KEY (unit_id) REFERENCES units(id)
        )
        """;

            String createSupplier = """
        CREATE TABLE IF NOT EXISTS suppliers (
            id INT AUTO_INCREMENT PRIMARY KEY,
            name VARCHAR(255) NOT NULL UNIQUE,
            address VARCHAR(255),
            phone VARCHAR(50),
            email VARCHAR(100),
            notes TEXT
        )
        """;

            String createTransactionsTable = """
        CREATE TABLE IF NOT EXISTS transactions (
            id INT AUTO_INCREMENT PRIMARY KEY,
            transaction_number VARCHAR(100) NOT NULL UNIQUE,
            trans_date DATETIME NOT NULL,
            payment_method VARCHAR(50),
            status VARCHAR(20) DEFAULT 'ACTIVE',
            user_id INT,
            total DECIMAL(12,2) NOT NULL,
            FOREIGN KEY (user_id) REFERENCES users(id)
        )
        """;

            String createTransactionDetailsTable = """
        CREATE TABLE IF NOT EXISTS transaction_details (
            id INT AUTO_INCREMENT PRIMARY KEY,
            transaction_id INT,
            product_id INT,
            qty INT,
            price DECIMAL(12,2),
            subtotal DECIMAL(12,2),
            FOREIGN KEY (transaction_id) REFERENCES transactions(id),
            FOREIGN KEY (product_id) REFERENCES products(id)
        )
        """;

            String createTablePurchase = """
        CREATE TABLE IF NOT EXISTS purchases (
            id INT AUTO_INCREMENT PRIMARY KEY,
            purchase_number VARCHAR(100) NOT NULL UNIQUE,
            purchase_date DATETIME NOT NULL,
            supplier_id INT,
            total DECIMAL(12,2) NOT NULL,
            user_id INT,
            status VARCHAR(20) DEFAULT 'ACTIVE',
            FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
            FOREIGN KEY (user_id) REFERENCES users(id)
        )
        """;

            String createTablePurchaseDetails = """
        CREATE TABLE IF NOT EXISTS purchase_details (
            id INT AUTO_INCREMENT PRIMARY KEY,
            purchase_id INT,
            product_id INT,
            quantity INT,
            buy_price DECIMAL(12,2),
            subtotal DECIMAL(12,2),
            FOREIGN KEY (purchase_id) REFERENCES purchases(id),
            FOREIGN KEY (product_id) REFERENCES products(id)
        )
        """;

            String createTableAdjustStock = """
        CREATE TABLE IF NOT EXISTS stock_adjustments (
            id INT AUTO_INCREMENT PRIMARY KEY,
            product_id INT,
            adjust_date DATETIME,
            adjust_by INT,
            reason VARCHAR(255),
            description TEXT,
            quantity_change INT,
            FOREIGN KEY (product_id) REFERENCES products(id),
            FOREIGN KEY (adjust_by) REFERENCES users(id)
        )
        """;

            // ----------- EKSEKUSI HARUS URUT -------------
            stmt.execute(createUserTable);
            stmt.execute(createProductTypeTable);
            stmt.execute(createTableUnits);
            stmt.execute(createProductsTable);
            stmt.execute(createSupplier);
            stmt.execute(createTransactionsTable);
            stmt.execute(createTransactionDetailsTable);
            stmt.execute(createTablePurchase);
            stmt.execute(createTablePurchaseDetails);
            stmt.execute(createTableAdjustStock);
        }
    }

}
