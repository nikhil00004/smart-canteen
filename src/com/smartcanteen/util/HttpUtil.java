package com.smartcanteen.util;

import com.sun.net.httpserver.HttpExchange;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Small helper methods used by every handler:
 *   - reading the request body as text
 *   - sending a JSON response with the right status code + headers
 *   - splitting the URL path into segments (since HttpServer gives no
 *     built-in path-variable support like Spring's @PathVariable)
 *   - reading query string parameters
 */
public class HttpUtil {

    public static String readBody(HttpExchange exchange) throws IOException {
        InputStream in = exchange.getRequestBody();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int len;
        while ((len = in.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    public static byte[] readBodyBytes(HttpExchange exchange) throws IOException {
        InputStream in = exchange.getRequestBody();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int len;
        while ((len = in.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }
        return out.toByteArray();
    }

    public static void sendJson(HttpExchange exchange, int statusCode, Object bodyObject) throws IOException {
        String json = Json.write(bodyObject);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    public static void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", statusCode);
        body.put("message", message);
        sendJson(exchange, statusCode, body);
    }

    public static void sendNoContent(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
    }

    /**
     * Splits a path like "/api/menu-items/7/availability" into
     * ["api","menu-items","7","availability"], ignoring empty segments.
     */
    public static String[] pathSegments(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        String[] raw = path.split("/");
        List<String> segments = new ArrayList<>();
        for (String s : raw) {
            if (!s.isEmpty()) segments.add(s);
        }
        return segments.toArray(new String[0]);
    }

    public static Map<String, String> queryParams(HttpExchange exchange) {
        Map<String, String> params = new LinkedHashMap<>();
        String query = exchange.getRequestURI().getQuery();
        if (query == null) return params;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            String key = urlDecode(kv[0]);
            String value = kv.length > 1 ? urlDecode(kv[1]) : "";
            params.put(key, value);
        }
        return params;
    }

    private static String urlDecode(String s) {
        try {
            return java.net.URLDecoder.decode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }

    public static String method(HttpExchange exchange) {
        return exchange.getRequestMethod();
    }
}
