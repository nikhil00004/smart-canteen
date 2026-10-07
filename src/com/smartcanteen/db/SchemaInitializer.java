package com.smartcanteen.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runs once when the server starts:
 *   1. Creates all tables with CREATE TABLE IF NOT EXISTS (plain SQL,
 *      since there is no Hibernate here to do it automatically).
 *   2. Seeds the default admin account and the starting menu, but only
 *      if the database is currently empty (so it never duplicates data
 *      on the next restart).
 */
public class SchemaInitializer {

    public static void run() {
        try (Connection conn = DB.connect(); Statement st = conn.createStatement()) {

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS admin (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(50) NOT NULL UNIQUE,
                    password VARCHAR(100) NOT NULL
                )
            """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS category (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(100) NOT NULL UNIQUE
                )
            """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS menu_item (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(150) NOT NULL,
                    price DECIMAL(10,2) NOT NULL,
                    image_path VARCHAR(255),
                    available BOOLEAN NOT NULL DEFAULT TRUE,
                    category_id INT NOT NULL,
                    FOREIGN KEY (category_id) REFERENCES category(id)
                )
            """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS orders (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    customer_name VARCHAR(100) NOT NULL,
                    mobile_number VARCHAR(15) NOT NULL,
                    total_amount DECIMAL(10,2) NOT NULL,
                    payment_method VARCHAR(20) NOT NULL,
                    payment_status VARCHAR(30) NOT NULL,
                    order_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                    order_date DATETIME NOT NULL
                )
            """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS order_item (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    quantity INT NOT NULL,
                    price DECIMAL(10,2) NOT NULL,
                    order_id INT NOT NULL,
                    menu_item_id INT NOT NULL,
                    FOREIGN KEY (order_id) REFERENCES orders(id),
                    FOREIGN KEY (menu_item_id) REFERENCES menu_item(id)
                )
            """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS payment_qr (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    image_path VARCHAR(255) NOT NULL,
                    updated_at DATETIME NOT NULL,
                    active BOOLEAN NOT NULL DEFAULT TRUE
                )
            """);

            seedAdmin(conn);
            seedMenu(conn);

            System.out.println("Database ready (tables created if needed, data seeded if empty).");

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database: " + e.getMessage(), e);
        }
    }

    private static void seedAdmin(Connection conn) throws Exception {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM admin")) {
            rs.next();
            if (rs.getInt(1) == 0) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO admin (username, password) VALUES (?, ?)")) {
                    ps.setString(1, "admin");
                    ps.setString(2, "admin123");
                    ps.executeUpdate();
                }
                System.out.println("Default admin account created -> username: admin / password: admin123");
            }
        }
    }

    private static void seedMenu(Connection conn) throws Exception {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM category")) {
            rs.next();
            if (rs.getInt(1) > 0) return; // already seeded
        }

        Map<String, Map<String, Double>> data = new LinkedHashMap<>();

        Map<String, Double> breakfast = new LinkedHashMap<>();
        breakfast.put("Wada Pav", 20.0);
        breakfast.put("Samosa (1 pc)", 20.0);
        breakfast.put("Samosa Chat", 35.0);
        breakfast.put("Misal Pav", 60.0);
        breakfast.put("Poha", 30.0);
        breakfast.put("Upma", 30.0);
        breakfast.put("Idli Sambhar (2 pcs)", 40.0);
        data.put("Breakfast & Snacks", breakfast);

        Map<String, Double> sandwiches = new LinkedHashMap<>();
        sandwiches.put("Plain Veg Sandwich", 40.0);
        sandwiches.put("Veg Cheese Sandwich", 60.0);
        sandwiches.put("Bombay Grilled Sandwich", 70.0);
        sandwiches.put("Veg Burger", 60.0);
        sandwiches.put("Paneer Kathi Roll", 80.0);
        sandwiches.put("French Fries", 50.0);
        data.put("Sandwiches & Quick Bites", sandwiches);

        Map<String, Double> mainCourse = new LinkedHashMap<>();
        mainCourse.put("Veg Thali (Roti, Sabzi, Dal, Rice)", 120.0);
        mainCourse.put("Veg Biryani", 100.0);
        mainCourse.put("Dal Khichdi Tadka", 80.0);
        mainCourse.put("Paneer Butter Masala", 130.0);
        mainCourse.put("Chapati", 10.0);
        mainCourse.put("Butter Roti", 15.0);
        data.put("Main Course", mainCourse);

        Map<String, Double> beverages = new LinkedHashMap<>();
        beverages.put("Cutting Tea", 15.0);
        beverages.put("Masala Tea", 15.0);
        beverages.put("Coffee", 20.0);
        beverages.put("Cold Coffee with Ice Cream", 50.0);
        beverages.put("Fresh Lime Soda", 30.0);
        beverages.put("Sweet Lassi", 35.0);
        data.put("Beverages", beverages);

        Map<String, Double> desserts = new LinkedHashMap<>();
        desserts.put("Gulab Jamun (2 pcs)", 30.0);
        desserts.put("Ice Cream (Vanilla / Chocolate)", 40.0);
        data.put("Desserts", desserts);

        for (Map.Entry<String, Map<String, Double>> categoryEntry : data.entrySet()) {
            int categoryId;
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO category (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, categoryEntry.getKey());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    categoryId = keys.getInt(1);
                }
            }

            for (Map.Entry<String, Double> itemEntry : categoryEntry.getValue().entrySet()) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO menu_item (name, price, available, category_id) VALUES (?, ?, TRUE, ?)")) {
                    ps.setString(1, itemEntry.getKey());
                    ps.setDouble(2, itemEntry.getValue());
                    ps.setInt(3, categoryId);
                    ps.executeUpdate();
                }
            }
        }

        System.out.println("Initial categories and menu items seeded successfully.");
    }
}
