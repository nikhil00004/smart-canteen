package com.smartcanteen.handler;

import com.smartcanteen.db.DB;
import com.smartcanteen.util.FileStorage;
import com.smartcanteen.util.HttpUtil;
import com.smartcanteen.util.Json;
import com.smartcanteen.util.MultipartParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles every request under /api/menu-items
 *   GET    /api/menu-items                      -> list all (customer menu page)
 *   GET    /api/menu-items/category/{id}         -> list items in one category
 *   POST   /api/menu-items                       -> create (multipart: "item" JSON part + optional "image" file part)
 *   PUT    /api/menu-items/{id}                  -> update (same multipart shape)
 *   PATCH  /api/menu-items/{id}/availability      -> { "available": true/false }
 *   DELETE /api/menu-items/{id}                  -> delete
 */
public class MenuItemHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) {
        try {
            String method = HttpUtil.method(exchange);
            String[] s = HttpUtil.pathSegments(exchange); // ["api","menu-items", ...]

            if (method.equals("GET") && s.length == 2) {
                listAll(exchange);
            } else if (method.equals("GET") && s.length == 4 && s[2].equals("category")) {
                listByCategory(exchange, Integer.parseInt(s[3]));
            } else if (method.equals("POST") && s.length == 2) {
                create(exchange);
            } else if (method.equals("PUT") && s.length == 3) {
                update(exchange, Integer.parseInt(s[2]));
            } else if (method.equals("PATCH") && s.length == 4 && s[3].equals("availability")) {
                updateAvailability(exchange, Integer.parseInt(s[2]));
            } else if (method.equals("DELETE") && s.length == 3) {
                delete(exchange, Integer.parseInt(s[2]));
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
        String sql = "SELECT m.id, m.name, m.price, m.image_path, m.available, m.category_id, c.name AS category_name " +
                "FROM menu_item m JOIN category c ON m.category_id = c.id ORDER BY m.id";
        try (Connection conn = DB.connect();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) result.add(rowToMap(rs));
        }
        HttpUtil.sendJson(exchange, 200, result);
    }

    private void listByCategory(HttpExchange exchange, int categoryId) throws Exception {
        List<Object> result = new ArrayList<>();
        String sql = "SELECT m.id, m.name, m.price, m.image_path, m.available, m.category_id, c.name AS category_name " +
                "FROM menu_item m JOIN category c ON m.category_id = c.id WHERE m.category_id = ? ORDER BY m.id";
        try (Connection conn = DB.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(rowToMap(rs));
            }
        }
        HttpUtil.sendJson(exchange, 200, result);
    }

    private void create(HttpExchange exchange) throws Exception {
        byte[] body = HttpUtil.readBodyBytes(exchange);
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        List<MultipartParser.Part> parts = MultipartParser.parse(body, contentType);

        MultipartParser.Part itemPart = MultipartParser.findByName(parts, "item");
        if (itemPart == null) throw new IllegalArgumentException("Missing 'item' field");
        Map<String, Object> dto = Json.parseObject(itemPart.asText());

        String name = requireString(dto, "name", "Food name is required");
        Double price = Json.asDouble(dto.get("price"));
        if (price == null || price <= 0) throw new IllegalArgumentException("Price must be greater than 0");
        Integer categoryId = Json.asInt(dto.get("categoryId"));
        if (categoryId == null) throw new IllegalArgumentException("Category is required");
        boolean available = dto.containsKey("available") ? Json.asBoolean(dto.get("available")) : true;

        String imagePath = null;
        MultipartParser.Part imagePart = MultipartParser.findByName(parts, "image");
        if (imagePart != null && imagePart.filename != null && !imagePart.filename.isEmpty()) {
            imagePath = FileStorage.saveFile(imagePart.data, imagePart.filename, "food");
        }

        try (Connection conn = DB.connect()) {
            ensureCategoryExists(conn, categoryId);

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO menu_item (name, price, image_path, available, category_id) VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, name);
                ps.setDouble(2, price);
                ps.setString(3, imagePath);
                ps.setBoolean(4, available);
                ps.setInt(5, categoryId);
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    int newId = keys.getInt(1);
                    HttpUtil.sendJson(exchange, 201, fetchOne(conn, newId));
                }
            }
        }
    }

    private void update(HttpExchange exchange, int id) throws Exception {
        byte[] body = HttpUtil.readBodyBytes(exchange);
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        List<MultipartParser.Part> parts = MultipartParser.parse(body, contentType);

        MultipartParser.Part itemPart = MultipartParser.findByName(parts, "item");
        if (itemPart == null) throw new IllegalArgumentException("Missing 'item' field");
        Map<String, Object> dto = Json.parseObject(itemPart.asText());

        String name = requireString(dto, "name", "Food name is required");
        Double price = Json.asDouble(dto.get("price"));
        if (price == null || price <= 0) throw new IllegalArgumentException("Price must be greater than 0");
        Integer categoryId = Json.asInt(dto.get("categoryId"));
        boolean available = dto.containsKey("available") ? Json.asBoolean(dto.get("available")) : true;

        String newImagePath = null;
        MultipartParser.Part imagePart = MultipartParser.findByName(parts, "image");
        if (imagePart != null && imagePart.filename != null && !imagePart.filename.isEmpty()) {
            newImagePath = FileStorage.saveFile(imagePart.data, imagePart.filename, "food");
        }

        try (Connection conn = DB.connect()) {
            if (categoryId != null) ensureCategoryExists(conn, categoryId);

            String sql = newImagePath != null
                    ? "UPDATE menu_item SET name=?, price=?, available=?, category_id=?, image_path=? WHERE id=?"
                    : "UPDATE menu_item SET name=?, price=?, available=?, category_id=? WHERE id=?";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, name);
                ps.setDouble(2, price);
                ps.setBoolean(3, available);
                ps.setInt(4, categoryId);
                if (newImagePath != null) {
                    ps.setString(5, newImagePath);
                    ps.setInt(6, id);
                } else {
                    ps.setInt(5, id);
                }
                int updated = ps.executeUpdate();
                if (updated == 0) {
                    HttpUtil.sendError(exchange, 404, "Menu item not found with id: " + id);
                    return;
                }
            }

            HttpUtil.sendJson(exchange, 200, fetchOne(conn, id));
        }
    }

    private void updateAvailability(HttpExchange exchange, int id) throws Exception {
        Map<String, Object> body = Json.parseObject(HttpUtil.readBody(exchange));
        if (!body.containsKey("available")) throw new IllegalArgumentException("available field is required");
        boolean available = Json.asBoolean(body.get("available"));

        try (Connection conn = DB.connect()) {
            try (PreparedStatement ps = conn.prepareStatement("UPDATE menu_item SET available = ? WHERE id = ?")) {
                ps.setBoolean(1, available);
                ps.setInt(2, id);
                int updated = ps.executeUpdate();
                if (updated == 0) {
                    HttpUtil.sendError(exchange, 404, "Menu item not found with id: " + id);
                    return;
                }
            }
            HttpUtil.sendJson(exchange, 200, fetchOne(conn, id));
        }
    }

    private void delete(HttpExchange exchange, int id) throws Exception {
        try (Connection conn = DB.connect();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM menu_item WHERE id = ?")) {
            ps.setInt(1, id);
            int deleted = ps.executeUpdate();
            if (deleted == 0) {
                HttpUtil.sendError(exchange, 404, "Menu item not found with id: " + id);
                return;
            }
            HttpUtil.sendNoContent(exchange);
        }
    }

    private void ensureCategoryExists(Connection conn, int categoryId) throws Exception {
        try (PreparedStatement check = conn.prepareStatement("SELECT id FROM category WHERE id = ?")) {
            check.setInt(1, categoryId);
            try (ResultSet rs = check.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Category not found with id: " + categoryId);
            }
        }
    }

    private Map<String, Object> fetchOne(Connection conn, int id) throws Exception {
        String sql = "SELECT m.id, m.name, m.price, m.image_path, m.available, m.category_id, c.name AS category_name " +
                "FROM menu_item m JOIN category c ON m.category_id = c.id WHERE m.id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rowToMap(rs);
            }
        }
    }

    private String requireString(Map<String, Object> dto, String key, String errorMessage) {
        String value = Json.asString(dto.get(key));
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(errorMessage);
        return value.trim();
    }

    private Map<String, Object> rowToMap(ResultSet rs) throws SQLException {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", rs.getInt("id"));
        map.put("name", rs.getString("name"));
        map.put("price", rs.getDouble("price"));
        map.put("imagePath", rs.getString("image_path"));
        map.put("available", rs.getBoolean("available"));
        map.put("categoryId", rs.getInt("category_id"));
        map.put("categoryName", rs.getString("category_name"));
        return map;
    }
}
