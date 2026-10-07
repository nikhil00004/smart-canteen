package com.smartcanteen.handler;

import com.smartcanteen.db.DB;
import com.smartcanteen.util.FileStorage;
import com.smartcanteen.util.HttpUtil;
import com.smartcanteen.util.MultipartParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles every request under /api/payment-qr
 *   GET  /api/payment-qr/active  -> the QR code shown to customers at checkout
 *   POST /api/payment-qr         -> upload a new QR (multipart field "qrImage"), replaces the old one
 */
public class PaymentQrHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) {
        try {
            String method = HttpUtil.method(exchange);
            String[] s = HttpUtil.pathSegments(exchange); // ["api","payment-qr", maybe "active"]

            if (method.equals("GET") && s.length == 3 && s[2].equals("active")) {
                getActive(exchange);
            } else if (method.equals("POST") && s.length == 2) {
                upload(exchange);
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

    private void getActive(HttpExchange exchange) throws Exception {
        try (Connection conn = DB.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT id, image_path, updated_at FROM payment_qr WHERE active = TRUE ORDER BY id DESC LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                HttpUtil.sendError(exchange, 404, "No active payment QR code has been uploaded yet");
                return;
            }
            Map<String, Object> qr = new LinkedHashMap<>();
            qr.put("id", rs.getInt("id"));
            qr.put("imagePath", rs.getString("image_path"));
            qr.put("updatedAt", rs.getString("updated_at"));
            HttpUtil.sendJson(exchange, 200, qr);
        }
    }

    private void upload(HttpExchange exchange) throws Exception {
        byte[] body = HttpUtil.readBodyBytes(exchange);
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        List<MultipartParser.Part> parts = MultipartParser.parse(body, contentType);

        MultipartParser.Part filePart = MultipartParser.findByName(parts, "qrImage");
        if (filePart == null || filePart.filename == null || filePart.filename.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }

        String imagePath = FileStorage.saveFile(filePart.data, filePart.filename, "qr");
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        try (Connection conn = DB.connect()) {
            try (PreparedStatement deactivate = conn.prepareStatement("UPDATE payment_qr SET active = FALSE WHERE active = TRUE")) {
                deactivate.executeUpdate();
            }

            try (PreparedStatement insert = conn.prepareStatement(
                    "INSERT INTO payment_qr (image_path, updated_at, active) VALUES (?, ?, TRUE)",
                    Statement.RETURN_GENERATED_KEYS)) {
                insert.setString(1, imagePath);
                insert.setString(2, now);
                insert.executeUpdate();
                try (ResultSet keys = insert.getGeneratedKeys()) {
                    keys.next();
                    int id = keys.getInt(1);
                    Map<String, Object> qr = new LinkedHashMap<>();
                    qr.put("id", id);
                    qr.put("imagePath", imagePath);
                    qr.put("updatedAt", now);
                    HttpUtil.sendJson(exchange, 201, qr);
                }
            }
        }
    }
}
