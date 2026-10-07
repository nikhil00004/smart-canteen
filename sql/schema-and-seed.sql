-- =========================================================
-- Smart College Canteen Management System (plain-Java edition)
-- Optional manual SQL script.
--
-- NOTE: You do NOT need to run this file. SchemaInitializer.java
-- creates every table (CREATE TABLE IF NOT EXISTS) and seeds the
-- default admin account + starting menu automatically the first
-- time you start the server, as long as the database is empty.
--
-- Use this only if you want to inspect the schema, or reset the
-- database back to the starting data by hand.
-- =========================================================

CREATE DATABASE IF NOT EXISTS smart_canteen_db;
USE smart_canteen_db;

CREATE TABLE IF NOT EXISTS admin (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS category (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS menu_item (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    image_path VARCHAR(255),
    available BOOLEAN NOT NULL DEFAULT TRUE,
    category_id INT NOT NULL,
    FOREIGN KEY (category_id) REFERENCES category(id)
);

CREATE TABLE IF NOT EXISTS orders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    customer_name VARCHAR(100) NOT NULL,
    mobile_number VARCHAR(15) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    payment_status VARCHAR(30) NOT NULL,
    order_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    order_date DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS order_item (
    id INT AUTO_INCREMENT PRIMARY KEY,
    quantity INT NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    order_id INT NOT NULL,
    menu_item_id INT NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (menu_item_id) REFERENCES menu_item(id)
);

CREATE TABLE IF NOT EXISTS payment_qr (
    id INT AUTO_INCREMENT PRIMARY KEY,
    image_path VARCHAR(255) NOT NULL,
    updated_at DATETIME NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

-- ---------- Default Admin ----------
INSERT INTO admin (username, password) VALUES ('admin', 'admin123');

-- ---------- Categories ----------
INSERT INTO category (name) VALUES ('Breakfast & Snacks');
INSERT INTO category (name) VALUES ('Sandwiches & Quick Bites');
INSERT INTO category (name) VALUES ('Main Course');
INSERT INTO category (name) VALUES ('Beverages');
INSERT INTO category (name) VALUES ('Desserts');

-- ---------- Menu Items ----------
INSERT INTO menu_item (name, price, available, category_id) VALUES ('Wada Pav', 20.00, true, 1);
INSERT INTO menu_item (name, price, available, category_id) VALUES ('Samosa (1 pc)', 20.00, true, 1);
INSERT INTO menu_item (name, price, available, category_id) VALUES ('Samosa Chat', 35.00, true, 1);
INSERT INTO menu_item (name, price, available, category_id) VALUES ('Misal Pav', 60.00, true, 1);
INSERT INTO menu_item (name, price, available, category_id) VALUES ('Poha', 30.00, true, 1);
INSERT INTO menu_item (name, price, available, category_id) VALUES ('Upma', 30.00, true, 1);
INSERT INTO menu_item (name, price, available, category_id) VALUES ('Idli Sambhar (2 pcs)', 40.00, true, 1);

INSERT INTO menu_item (name, price, available, category_id) VALUES ('Plain Veg Sandwich', 40.00, true, 2);
INSERT INTO menu_item (name, price, available, category_id) VALUES ('Veg Cheese Sandwich', 60.00, true, 2);
INSERT INTO menu_item (name, price, available, category_id) VALUES ('Bombay Grilled Sandwich', 70.00, true, 2);
INSERT INTO menu_item (name, price, available, category_id) VALUES ('Veg Burger', 60.00, true, 2);
INSERT INTO menu_item (name, price, available, category_id) VALUES ('Paneer Kathi Roll', 80.00, true, 2);
INSERT INTO menu_item (name, price, available, category_id) VALUES ('French Fries', 50.00, true, 2);

INSERT INTO menu_item (name, price, available, category_id) VALUES ('Veg Thali (Roti, Sabzi, Dal, Rice)', 120.00, true, 3);
INSERT INTO menu_item (name, price, available, category_id) VALUES ('Veg Biryani', 100.00, true, 3);
INSERT INTO menu_item (name, price, available, category_id) VALUES ('Dal Khichdi Tadka', 80.00, true, 3);
INSERT INTO menu_item (name, price, available, category_id) VALUES ('Paneer Butter Masala', 130.00, true, 3);
INSERT INTO menu_item (name, price, available, category_id) VALUES ('Chapati', 10.00, true, 3);
INSERT INTO menu_item (name, price, available, category_id) VALUES ('Butter Roti', 15.00, true, 3);

INSERT INTO menu_item (name, price, available, category_id) VALUES ('Cutting Tea', 15.00, true, 4);
INSERT INTO menu_item (name, price, available, category_id) VALUES ('Masala Tea', 15.00, true, 4);
INSERT INTO menu_item (name, price, available, category_id) VALUES ('Coffee', 20.00, true, 4);
INSERT INTO menu_item (name, price, available, category_id) VALUES ('Cold Coffee with Ice Cream', 50.00, true, 4);
INSERT INTO menu_item (name, price, available, category_id) VALUES ('Fresh Lime Soda', 30.00, true, 4);
INSERT INTO menu_item (name, price, available, category_id) VALUES ('Sweet Lassi', 35.00, true, 4);

INSERT INTO menu_item (name, price, available, category_id) VALUES ('Gulab Jamun (2 pcs)', 30.00, true, 5);
INSERT INTO menu_item (name, price, available, category_id) VALUES ('Ice Cream (Vanilla / Chocolate)', 40.00, true, 5);
