package com.smartcanteen.handler;

import com.smartcanteen.db.DB;
import com.smartcanteen.util.HttpUtil;
import com.smartcanteen.util.Json;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handles every request under /api/admin
 *   POST /api/admin/login             -> { username, password } -> { success, message }
 *   GET  /api/admin/dashboard-stats   -> order counts for the dashboard cards
 *
 * Authentication is intentionally simple (plain username/password check
 * against the "admin" table) - no JWT/OTP, as required for this project.
 */
public class AdminHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) {
        try {
            String method = HttpUtil.method(exchange);
            String[] s = HttpUtil.pathSegments(exchange); // ["api","admin", "login" | "dashboard-stats"]

            if (method.equals("POST") && s.length == 3 && s[2].equals("login")) {
                login(exchange);
            } else if (method.equals("GET") && s.length == 3 && s[2].equals("dashboard-stats")) {
                dashboardStats(exchange);
            } else {
                HttpUtil.sendError(exchange, 404, "No such endpoint");
            }
        } catch (Exception e) {
            safeSendError(exchange, 500, "Something went wrong: " + e.getMessage());
        }
    }

    private void safeSendError(HttpExchange exchange, int code, String message) {
        try {
            HttpUtil.sendError(exchange, code, message);
        } catch (Exception ignored) {
        }
    }

    private void login(HttpExchange exchange) throws Exception {
        Map<String, Object> body = Json.parseObject(HttpUtil.readBody(exchange));
        String username = Json.asString(body.get("username"));
        String password = Json.asString(body.get("password"));

        if (username == null || password == null) {
            HttpUtil.sendError(exchange, 400, "Username and password are required");
            return;
        }

        boolean valid = false;
        try (Connection conn = DB.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT password FROM admin WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getString("password").equals(password)) {
                    valid = true;
                }
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();
        if (valid) {
            response.put("success", true);
            response.put("message", "Login successful");
            HttpUtil.sendJson(exchange, 200, response);
        } else {
            response.put("success", false);
            response.put("message", "Invalid username or password");
            HttpUtil.sendJson(exchange, 401, response);
        }
    }

    private void dashboardStats(HttpExchange exchange) throws Exception {
        Map<String, Object> stats = new LinkedHashMap<>();
        try (Connection conn = DB.connect()) {
            stats.put("totalOrders", countAll(conn));
            stats.put("pendingOrders", countByStatus(conn, "PENDING"));
            stats.put("preparingOrders", countByStatus(conn, "PREPARING"));
            stats.put("readyOrders", countByStatus(conn, "READY"));
            stats.put("completedOrders", countByStatus(conn, "COMPLETED"));
        }
        HttpUtil.sendJson(exchange, 200, stats);
    }

    private int countAll(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM orders")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private int countByStatus(Connection conn, String status) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM orders WHERE order_status = ?")) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
