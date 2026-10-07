package com.smartcanteen;

import com.smartcanteen.db.SchemaInitializer;
import com.smartcanteen.handler.*;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class Main {

    public static void main(String[] args) throws Exception {
        // 1. Make sure the database tables exist and have starting data.
        SchemaInitializer.run();

        // 2. Start the HTTP server on port 8080.
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // Static frontend - serves web/index.html and any other file placed in web/
        server.createContext("/", new StaticFileHandler("web", "", "index.html"));

        // Uploaded images (food photos + QR codes)
        server.createContext("/uploads", new StaticFileHandler("uploads", "/uploads", ""));

        // REST API
        server.createContext("/api/categories", new CategoryHandler());
        server.createContext("/api/menu-items", new MenuItemHandler());
        server.createContext("/api/orders", new OrderHandler());
        server.createContext("/api/payment-qr", new PaymentQrHandler());
        server.createContext("/api/admin", new AdminHandler());

        // Handle multiple requests at once instead of one at a time
        server.setExecutor(Executors.newFixedThreadPool(10));

        server.start();
        System.out.println("=================================================");
        System.out.println(" WEll COME TO NIKHIL MACHIVALE PROJECT");
        System.out.println(" Smart Canteen server started!");
        System.out.println(" Customer site : http://localhost:" + port);
        System.out.println(" Admin panel   : http://localhost:" + port + "/admin-login.html");
        System.out.println("=================================================");
    }
}
