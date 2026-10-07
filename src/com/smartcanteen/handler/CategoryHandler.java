package com.smartcanteen.handler;

import com.smartcanteen.db.DB;
import com.smartcanteen.util.HttpUtil;
import com.smartcanteen.util.Json;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles every request under /api/categories
 *   GET    /api/categories        -> list all
 *   POST   /api/categories        -> create
 *   PUT    /api/categories/{id}   -> update
 *   DELETE /api/categories/{id}   -> delete
 */
public class CategoryHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) {
        try {
            String method = HttpUtil.method(exchange);
            String[] segments = HttpUtil.pathSegments(exchange); // ["api","categories", maybe "{id}"]

            if (method.equals("GET") && segments.length == 2) {
                listAll(exchange);
            } else if (method.equals("POST") && segments.length == 2) {
                create(exchange);
            } else if (method.equals("PUT") && segments.length == 3) {
                update(exchange, Integer.parseInt(segments[2]));
            } else if (method.equals("DELETE") && segments.length == 3) {
                delete(exchange, Integer.parseInt(segments[2]));
            } else {
                HttpUtil.sendError(exchange, 404, "No such endpoint");
            }
        } catch (IllegalArgumentException e) {
            safeSendError(exchange, 400, e.getMessage());
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

    private void listAll(HttpExchange exchange) throws Exception {
        List<Object> result = new ArrayList<>();
        try (Connection conn = DB.connect();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, name FROM category ORDER BY id")) {
            while (rs.next()) {
                result.add(rowToMap(rs));
            }
        }
        HttpUtil.sendJson(exchange, 200, result);
    }

    private void create(HttpExchange exchange) throws Exception {
        Map<String, Object> body = Json.parseObject(HttpUtil.readBody(exchange));
        String name = Json.asString(body.get("name"));
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Category name is required");
        }
        name = name.trim();

        try (Connection conn = DB.connect()) {
            try (PreparedStatement check = conn.prepareStatement("SELECT id FROM category WHERE LOWER(name) = LOWER(?)")) {
                check.setString(1, name);
                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next()) {
                        throw new IllegalArgumentException("Category already exists: " + name);
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO category (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, name);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    int id = keys.getInt(1);
                    HttpUtil.sendJson(exchange, 201, Json.map("id", id, "name", name));
                }
            }
        }
    }

    private void update(HttpExchange exchange, int id) throws Exception {
        Map<String, Object> body = Json.parseObject(HttpUtil.readBody(exchange));
        String name = Json.asString(body.get("name"));
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Category name is required");
        }
        name = name.trim();

        try (Connection conn = DB.connect();
             PreparedStatement ps = conn.prepareStatement("UPDATE category SET name = ? WHERE id = ?")) {
            ps.setString(1, name);
            ps.setInt(2, id);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                HttpUtil.sendError(exchange, 404, "Category not found with id: " + id);
                return;
            }
            HttpUtil.sendJson(exchange, 200, Json.map("id", id, "name", name));
        }
    }

    private void delete(HttpExchange exchange, int id) throws Exception {
        try (Connection conn = DB.connect()) {
            try (PreparedStatement check = conn.prepareStatement("SELECT COUNT(*) FROM menu_item WHERE category_id = ?")) {
                check.setInt(1, id);
                try (ResultSet rs = check.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) > 0) {
                        throw new IllegalArgumentException("Cannot delete category that still has menu items. Move or delete those items first.");
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM category WHERE id = ?")) {
                ps.setInt(1, id);
                int deleted = ps.executeUpdate();
                if (deleted == 0) {
                    HttpUtil.sendError(exchange, 404, "Category not found with id: " + id);
                    return;
                }
                HttpUtil.sendNoContent(exchange);
            }
        }
    }

    private Map<String, Object> rowToMap(ResultSet rs) throws SQLException {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", rs.getInt("id"));
        map.put("name", rs.getString("name"));
        return map;
    }
}
