package com.smartcanteen.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Serves plain files from disk (used for both the "web" frontend folder
 * and the "uploads" folder). Spring Boot does this automatically; with
 * plain com.sun.net.httpserver we have to write it ourselves.
 */
public class StaticFileHandler implements HttpHandler {

    private final String rootDir;
    private final String urlPrefix;
    private final String defaultFile;

    public StaticFileHandler(String rootDir, String urlPrefix, String defaultFile) {
        this.rootDir = rootDir;
        this.urlPrefix = urlPrefix;
        this.defaultFile = defaultFile;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();

        // strip the URL prefix (e.g. "/uploads") to get the file's relative path
        String relativePath = path.startsWith(urlPrefix) ? path.substring(urlPrefix.length()) : path;
        if (relativePath.isEmpty() || relativePath.equals("/")) {
            relativePath = "/" + defaultFile;
        }

        File file = new File(rootDir, relativePath).getCanonicalFile();
        File rootCanonical = new File(rootDir).getCanonicalFile();

        // security check: never serve a file outside the intended root folder
        if (!file.getPath().startsWith(rootCanonical.getPath()) || !file.isFile()) {
            String notFound = "404 Not Found";
            exchange.sendResponseHeaders(404, notFound.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(notFound.getBytes());
            }
            return;
        }

        exchange.getResponseHeaders().set("Content-Type", contentTypeFor(file.getName()));
        exchange.sendResponseHeaders(200, file.length());
        try (OutputStream os = exchange.getResponseBody(); FileInputStream fis = new FileInputStream(file)) {
            fis.transferTo(os);
        }
    }

    private String contentTypeFor(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".html")) return "text/html; charset=UTF-8";
        if (lower.endsWith(".css")) return "text/css";
        if (lower.endsWith(".js")) return "application/javascript";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }
}
