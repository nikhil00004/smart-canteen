package com.smartcanteen.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DB {

    public static final String DB_URL = System.getenv().getOrDefault(
            "DB_URL",
            "jdbc:mysql://localhost:3306/smart_canteen_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
    );
    public static final String DB_USER = System.getenv().getOrDefault("DB_USER", "root");
    public static final String DB_PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "Nikhil@0608");

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                    "MySQL JDBC driver not found on classpath. " +
                    "Download mysql-connector-j and put the jar in the lib/ folder. See README.md.", e);
        }
    }

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
}