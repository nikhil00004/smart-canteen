package com.smartcanteen.handler;

import com.smartcanteen.db.DB;
import com.smartcanteen.util.HttpUtil;
import com.smartcanteen.util.Json;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles every request under /api/orders
 *   POST   /api/orders                       -> place a new order (customer checkout)
 *   GET    /api/orders                        -> list all orders (admin)
 *   GET    /api/orders/{id}                    -> get one order (order-success page)
 *   PATCH  /api/orders/{id}/status             -> { "status": "PREPARING" }
 *   PATCH  /api/orders/{id}/payment-status     -> { "status": "PAID" }
 */
public class OrderHandler implements HttpHandler {

    private static final List<String> ORDER_STATUSES = List.of("PENDING", "PREPARING", "READY", "COMPLETED", "CANCELLED");
    private static final List<String> PAYMENT_STATUSES = List.of("PENDING", "PENDING_VERIFICATION", "PAID", "REJECTED");

    @Override
    public void handle(HttpExchange exchange) {
        try {
            String method = HttpUtil.method(exchange);
            String[] s = HttpUtil.pathSegments(exchange); // ["api","orders", ...]

            if (method.equals("POST") && s.length == 2) {
                placeOrder(exchange);
            } else if (method.equals("GET") && s.length == 2) {
                listAll(exchange);
            } else if (method.equals("GET") && s.length == 3) {
                getOne(exchange, Integer.parseInt(s[2]));
            } else if (method.equals("PATCH") && s.length == 4 && s[3].equals("status")) {
                updateOrderStatus(exchange, Integer.parseInt(s[2]));
            } else if (method.equals("PATCH") && s.length == 4 && s[3].equals("payment-status")) {
                updatePaymentStatus(exchange, Integer.parseInt(s[2]));
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

    @SuppressWarnings("unchecked")
    private void placeOrder(HttpExchange exchange) throws Exception {
        Map<String, Object> body = Json.parseObject(HttpUtil.readBody(exchange));

        String customerName = Json.asString(body.get("customerName"));
        String mobileNumber = Json.asString(body.get("mobileNumber"));
        String paymentMethod = Json.asString(body.get("paymentMethod"));
        Object itemsRaw = body.get("items");

        if (customerName == null || customerName.trim().isEmpty()) throw new IllegalArgumentException("Name is required");
        if (mobileNumber == null || !mobileNumber.trim().matches("^[6-9]\\d{9}$")) throw new IllegalArgumentException("Enter a valid 10-digit mobile number");
        if (paymentMethod == null || (!paymentMethod.equals("ONLINE") && !paymentMethod.equals("OFFLINE"))) throw new IllegalArgumentException("paymentMethod must be ONLINE or OFFLINE");
        if (!(itemsRaw instanceof List) || ((List<Object>) itemsRaw).isEmpty()) throw new IllegalArgumentException("Cart cannot be empty");

        List<Object> items = (List<Object>) itemsRaw;
        String paymentStatus = paymentMethod.equals("ONLINE") ? "PENDING_VERIFICATION" : "PENDING";

        try (Connection conn = DB.connect()) {
            conn.setAutoCommit(false);
            try {
                double total = 0;
                // menuItemId -> [price, quantity]
                List<Object[]> lineItems = new ArrayList<>();

                for (Object rawLine : items) {
                    Map<String, Object> line = (Map<String, Object>) rawLine;
                    Integer menuItemId = Json.asInt(line.get("menuItemId"));
                    Integer quantity = Json.asInt(line.get("quantity"));
                    if (menuItemId == null) throw new IllegalArgumentException("menuItemId is required");
                    if (quantity == null || quantity < 1) throw new IllegalArgumentException("Quantity must be at least 1");

                    double price;
                    boolean available;
                    String itemName;
                    try (PreparedStatement ps = conn.prepareStatement("SELECT name, price, available FROM menu_item WHERE id = ?")) {
                        ps.setInt(1, menuItemId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) throw new IllegalArgumentException("Menu item not found with id: " + menuItemId);
                            itemName = rs.getString("name");
                            price = rs.getDouble("price");
                            available = rs.getBoolean("available");
                        }
                    }
                    if (!available) throw new IllegalArgumentException(itemName + " is currently unavailable");

                    total += price * quantity;
                    lineItems.add(new Object[]{menuItemId, quantity, price});
                }

                int orderId;
                String orderDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO orders (customer_name, mobile_number, total_amount, payment_method, payment_status, order_status, order_date) VALUES (?, ?, ?, ?, ?, 'PENDING', ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, customerName.trim());
                    ps.setString(2, mobileNumber.trim());
                    ps.setDouble(3, total);
                    ps.setString(4, paymentMethod);
                    ps.setString(5, paymentStatus);
                    ps.setString(6, orderDate);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        orderId = keys.getInt(1);
                    }
                }

                for (Object[] line : lineItems) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO order_item (quantity, price, order_id, menu_item_id) VALUES (?, ?, ?, ?)")) {
                        ps.setInt(1, (Integer) line[1]);
                        ps.setDouble(2, (Double) line[2]);
                        ps.setInt(3, orderId);
                        ps.setInt(4, (Integer) line[0]);
                        ps.executeUpdate();
                    }
                }

                conn.commit();
                HttpUtil.sendJson(exchange, 201, fetchOne(conn, orderId));

            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private void listAll(HttpExchange exchange) throws Exception {
        List<Object> result = new ArrayList<>();
        try (Connection conn = DB.connect();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT id FROM orders ORDER BY order_date DESC")) {
            List<Integer> ids = new ArrayList<>();
            while (rs.next()) ids.add(rs.getInt("id"));
            for (int id : ids) result.add(fetchOne(conn, id));
        }
        HttpUtil.sendJson(exchange, 200, result);
    }

    private void getOne(HttpExchange exchange, int id) throws Exception {
        try (Connection conn = DB.connect()) {
            Map<String, Object> order = fetchOne(conn, id);
            if (order == null) {
                HttpUtil.sendError(exchange, 404, "Order not found with id: " + id);
                return;
            }
            HttpUtil.sendJson(exchange, 200, order);
        }
    }

    private void updateOrderStatus(HttpExchange exchange, int id) throws Exception {
        Map<String, Object> body = Json.parseObject(HttpUtil.readBody(exchange));
        String status = Json.asString(body.get("status"));
        if (status == null || !ORDER_STATUSES.contains(status.toUpperCase())) {
            throw new IllegalArgumentException("Invalid order status: " + status);
        }
        try (Connection conn = DB.connect();
             PreparedStatement ps = conn.prepareStatement("UPDATE orders SET order_status = ? WHERE id = ?")) {
            ps.setString(1, status.toUpperCase());
            ps.setInt(2, id);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                HttpUtil.sendError(exchange, 404, "Order not found with id: " + id);
                return;
            }
            HttpUtil.sendJson(exchange, 200, fetchOne(conn, id));
        }
    }

    private void updatePaymentStatus(HttpExchange exchange, int id) throws Exception {
        Map<String, Object> body = Json.parseObject(HttpUtil.readBody(exchange));
        String status = Json.asString(body.get("status"));
        if (status == null || !PAYMENT_STATUSES.contains(status.toUpperCase())) {
            throw new IllegalArgumentException("Invalid payment status: " + status);
        }
        try (Connection conn = DB.connect();
             PreparedStatement ps = conn.prepareStatement("UPDATE orders SET payment_status = ? WHERE id = ?")) {
            ps.setString(1, status.toUpperCase());
            ps.setInt(2, id);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                HttpUtil.sendError(exchange, 404, "Order not found with id: " + id);
                return;
            }
            HttpUtil.sendJson(exchange, 200, fetchOne(conn, id));
        }
    }

    private Map<String, Object> fetchOne(Connection conn, int id) throws Exception {
        Map<String, Object> order;
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM orders WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                order = new LinkedHashMap<>();
                order.put("id", rs.getInt("id"));
                order.put("customerName", rs.getString("customer_name"));
                order.put("mobileNumber", rs.getString("mobile_number"));
                order.put("totalAmount", rs.getDouble("total_amount"));
                order.put("paymentMethod", rs.getString("payment_method"));
                order.put("paymentStatus", rs.getString("payment_status"));
                order.put("orderStatus", rs.getString("order_status"));
                order.put("orderDate", rs.getString("order_date"));
            }
        }

        List<Object> items = new ArrayList<>();
        String sql = "SELECT oi.quantity, oi.price, m.name AS food_name FROM order_item oi " +
                "JOIN menu_item m ON oi.menu_item_id = m.id WHERE oi.order_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("foodName", rs.getString("food_name"));
                    item.put("quantity", rs.getInt("quantity"));
                    item.put("price", rs.getDouble("price"));
                    items.add(item);
                }
            }
        }
        order.put("items", items);
        return order;
    }
}
