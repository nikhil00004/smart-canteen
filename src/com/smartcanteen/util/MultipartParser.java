package com.smartcanteen.util;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * A small, dependency-free parser for multipart/form-data request bodies.
 * Used for food-image and QR-code uploads (admin-menu.html, admin-payment.html).
 *
 * com.sun.net.httpserver has no built-in multipart support (unlike Spring's
 * MultipartFile), so this class does the minimum needed: split the raw
 * bytes on the boundary and pull out each part's name, optional filename,
 * and content bytes.
 */
public class MultipartParser {

    public static class Part {
        public String name;
        public String filename; // null for plain text fields
        public byte[] data;

        public String asText() {
            return new String(data, StandardCharsets.UTF_8);
        }
    }

    /**
     * @param contentType the request's Content-Type header, e.g.
     *                    "multipart/form-data; boundary=----WebKitFormBoundaryXYZ"
     */
    public static List<Part> parse(byte[] body, String contentType) {
        String boundary = extractBoundary(contentType);
        if (boundary == null) {
            throw new IllegalArgumentException("No multipart boundary found in Content-Type header");
        }

        byte[] boundaryBytes = ("--" + boundary).getBytes(StandardCharsets.UTF_8);
        List<Part> parts = new ArrayList<>();

        List<int[]> boundaryPositions = findAll(body, boundaryBytes);

        for (int b = 0; b < boundaryPositions.size() - 1; b++) {
            int start = boundaryPositions.get(b)[0] + boundaryPositions.get(b)[1];
            int end = boundaryPositions.get(b + 1)[0];
            if (start >= end) continue;

            // Skip the CRLF right after the boundary line
            while (start < end && (body[start] == '\r' || body[start] == '\n')) start++;

            // Find the blank line that separates headers from the part body ("\r\n\r\n")
            int headerEnd = indexOf(body, "\r\n\r\n".getBytes(StandardCharsets.UTF_8), start, end);
            if (headerEnd == -1) continue;

            String headerText = new String(body, start, headerEnd - start, StandardCharsets.UTF_8);
            int contentStart = headerEnd + 4;
            int contentEnd = end;
            // trailing CRLF before next boundary belongs to the boundary marker, not the content
            if (contentEnd >= 2 && body[contentEnd - 2] == '\r' && body[contentEnd - 1] == '\n') {
                contentEnd -= 2;
            }

            Part part = new Part();
            for (String headerLine : headerText.split("\r\n")) {
                String lower = headerLine.toLowerCase();
                if (lower.startsWith("content-disposition")) {
                    part.name = extractHeaderValue(headerLine, "name");
                    part.filename = extractHeaderValue(headerLine, "filename");
                }
            }

            byte[] data = new byte[contentEnd - contentStart];
            System.arraycopy(body, contentStart, data, 0, data.length);
            part.data = data;

            if (part.name != null) {
                parts.add(part);
            }
        }

        return parts;
    }

    public static Part findByName(List<Part> parts, String name) {
        for (Part p : parts) {
            if (name.equals(p.name)) return p;
        }
        return null;
    }

    private static String extractBoundary(String contentType) {
        if (contentType == null) return null;
        for (String segment : contentType.split(";")) {
            segment = segment.trim();
            if (segment.startsWith("boundary=")) {
                String value = segment.substring("boundary=".length());
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                return value;
            }
        }
        return null;
    }

    private static String extractHeaderValue(String headerLine, String key) {
        String marker = key + "=\"";
        int idx = headerLine.indexOf(marker);
        if (idx == -1) return null;
        int start = idx + marker.length();
        int end = headerLine.indexOf('"', start);
        if (end == -1) return null;
        return headerLine.substring(start, end);
    }

    private static List<int[]> findAll(byte[] data, byte[] pattern) {
        List<int[]> positions = new ArrayList<>();
        int from = 0;
        while (true) {
            int idx = indexOf(data, pattern, from, data.length);
            if (idx == -1) break;
            positions.add(new int[]{idx, pattern.length});
            from = idx + pattern.length;
        }
        return positions;
    }

    private static int indexOf(byte[] data, byte[] pattern, int from, int to) {
        outer:
        for (int i = from; i <= to - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) continue outer;
            }
            return i;
        }
        return -1;
    }
}
