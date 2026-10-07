# Smart College Canteen Management System (Plain-Java Edition)

Same project, same features, different plumbing. This version uses **no
frameworks at all** — just the JDK's own tools — and a normal multi-page
HTML/CSS/JS frontend (not a single combined file).

| Layer      | Technology                                              |
|------------|------------------------------------------------------------|
| Frontend   | HTML + CSS + Vanilla JavaScript — separate files (multi-page site), no React/Bootstrap |
| Backend    | Plain Java — `com.sun.net.httpserver.HttpServer` (built into the JDK, no Spring) |
| Connector  | JDBC — `java.sql` (built into the JDK, no Hibernate/JPA) |
| Database   | MySQL |
| Build tool | **None** — just `javac` / `java` directly (a `build.bat`/`start.bat` pair is provided for convenience) |

---

## 1. Project Structure

```
smart-canteen-plain/
├── build.bat / start.bat       Windows compile & run scripts
├── build.sh  / start.sh        Mac/Linux equivalents
├── lib/                        put mysql-connector-j-*.jar here (see Step 1)
├── sql/schema-and-seed.sql     optional manual SQL (auto-created on first run anyway)
├── web/                         <-- THE ENTIRE FRONTEND
│   ├── index.html, cart.html, checkout.html, order-success.html
│   ├── admin-login.html, admin-dashboard.html, admin-orders.html,
│   │   admin-menu.html, admin-categories.html, admin-payment.html
│   ├── css/  (style.css, cart.css, checkout.css, admin.css)
│   └── js/   (api.js, cart.js, menu.js, checkout.js,
│              admin.js, orders.js, categories.js, payment.js)
├── uploads/                    created automatically - stores uploaded images
└── src/com/smartcanteen/
    ├── Main.java                starts the HTTP server, registers routes
    ├── db/
    │   ├── DB.java               JDBC connection (java.sql.DriverManager)
    │   └── SchemaInitializer.java  creates tables + seeds data on first run
    ├── util/
    │   ├── Json.java              hand-written JSON parser/writer (no library)
    │   ├── HttpUtil.java          request/response helpers
    │   ├── MultipartParser.java   hand-written file-upload parser
    │   └── FileStorage.java       saves uploaded images to disk
    └── handler/
        ├── StaticFileHandler.java   serves every file in web/ + uploaded images
        ├── CategoryHandler.java
        ├── MenuItemHandler.java
        ├── OrderHandler.java
        ├── PaymentQrHandler.java
        └── AdminHandler.java
```

There is **no `pom.xml`, no build tool** — you compile with `javac` and run
with `java`, exactly like the very first Java program you ever wrote. The
only external file you need is the MySQL JDBC driver jar (Step 1 below),
because `java.sql` gives you the *API* to talk to a database, but not the
actual driver for MySQL specifically.

The frontend is a normal multi-page site — clicking "Cart" really
navigates the browser to `cart.html`, just like the very first version of
this project. Each page's own `<script>` tags pull in `js/api.js` plus
whichever page-specific script it needs, exactly as before.

---

## 2. Setup Guide (Windows)

### Step 1 — Install required software + download the MySQL driver
1. **JDK 17+** — https://adoptium.net (verify with `java -version`)
2. **MySQL Server** — https://dev.mysql.com/downloads/installer/ (remember your root password)
3. **MySQL Workbench** — installed together with MySQL Server
4. **VS Code** — https://code.visualstudio.com (install the "Extension Pack for Java")
5. **MySQL Connector/J** (the JDBC driver jar) — download from
   https://dev.mysql.com/downloads/connector/j/ → choose "Platform Independent" →
   download the `.zip` → extract it → find the file named something like
   `mysql-connector-j-9.1.0.jar` → copy it into this project's `lib/` folder.

   If you download a different version, either rename the jar to
   `mysql-connector-j-9.1.0.jar`, or edit that filename in `build.bat` and
   `start.bat` to match what you downloaded.

### Step 2 — Create the database
Open **MySQL Workbench**, connect, and run:
```sql
CREATE DATABASE smart_canteen_db;
```
That's it. Tables and starting menu data are created automatically the
first time you run the server (Step 4). `sql/schema-and-seed.sql` is only
there if you want to inspect or manually reset the schema.

### Step 3 — Configure the database connection
Open `src/com/smartcanteen/db/DB.java` and update these 2 lines near the
top to match your own MySQL username/password:
```java
public static final String DB_USER = "root";
public static final String DB_PASSWORD = "root";
```
(Change the port/host in `DB_URL` too, only if your MySQL isn't running on
the default `localhost:3306`.)

### Step 4 — Build and run
Open this project folder in VS Code, open a terminal (Terminal → New
Terminal), and run:
```
build.bat
```
This compiles every `.java` file into an `out` folder. If it says "Build
succeeded!", run:
```
start.bat
```
You should see:
```
Default admin account created -> username: admin / password: admin123
Initial categories and menu items seeded successfully.
Database ready...
=================================================
 Smart Canteen server started!
 Customer site : http://localhost:8080
=================================================
```

### Step 5 — Open the website
```
http://localhost:8080
```
Admin panel:
```
http://localhost:8080/admin-login.html
```
Default admin login: **admin / admin123**

### Step 6 — Verify the database
In MySQL Workbench:
- `SELECT * FROM category;` → 5 rows
- `SELECT * FROM menu_item;` → ~29 rows
- `SELECT * FROM admin;` → 1 row
- After placing a test order: `SELECT * FROM orders;` and `SELECT * FROM order_item;`

### Step 7 — Full end-to-end test
1. MySQL running, server running (`start.bat`).
2. Open `http://localhost:8080` — menu loads from the database.
3. Add items to cart → Cart → Proceed to Checkout.
4. Fill name + mobile, choose Offline Payment, place the order.
5. You land on the Order Success page with an Order ID; check `orders` / `order_item` tables.
6. Log in at `admin-login.html`, open Orders — your order appears; change its status.
7. Go to Menu Management → add a new food item (with or without an image).
8. Go back to `index.html` — the new item appears automatically under its category.

### Restarting after code changes
Whenever you edit a `.java` file, re-run `build.bat` then `start.bat`.
Editing anything in `web/` (HTML/CSS/JS) needs **no rebuild** — just
refresh the browser, since those are plain static files served as-is.

---

## 3. Database Design

Identical to the Spring Boot version — same 6 tables, same columns, same
relationships (`category` 1→many `menu_item`, `orders` 1→many
`order_item`). The only difference is these tables are created by plain
`CREATE TABLE IF NOT EXISTS` SQL in `SchemaInitializer.java` instead of by
Hibernate.

---

## 4. API Overview

Identical endpoints and JSON shapes to the Spring Boot version — the
frontend's `js/api.js` calls the exact same `/api/...` URLs:

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/api/categories` | List categories |
| POST | `/api/categories` | Create category |
| PUT | `/api/categories/{id}` | Update category |
| DELETE | `/api/categories/{id}` | Delete category |
| GET | `/api/menu-items` | List all menu items |
| GET | `/api/menu-items/category/{id}` | List items in a category |
| POST | `/api/menu-items` (multipart) | Create item (+ optional image) |
| PUT | `/api/menu-items/{id}` (multipart) | Update item (+ optional image) |
| PATCH | `/api/menu-items/{id}/availability` | Toggle available/unavailable |
| DELETE | `/api/menu-items/{id}` | Delete item |
| POST | `/api/orders` | Place an order |
| GET | `/api/orders` | List all orders |
| GET | `/api/orders/{id}` | Get one order |
| PATCH | `/api/orders/{id}/status` | Update order status |
| PATCH | `/api/orders/{id}/payment-status` | Update payment status |
| GET | `/api/payment-qr/active` | Get active QR code |
| POST | `/api/payment-qr` (multipart) | Upload/replace QR code |
| POST | `/api/admin/login` | Admin login |
| GET | `/api/admin/dashboard-stats` | Dashboard order counts |

---

## 5. FINAL CONNECTION EXPLANATION

**1. One process, two jobs — no framework doing it for you.**
`Main.java` calls `HttpServer.create(...)` (a class that has shipped
inside the JDK since Java 6) and registers a few **contexts** — URL
prefixes mapped to handler objects. `"/"` is mapped to
`StaticFileHandler`, which reads whichever file matches the request path
out of the `web/` folder (`index.html`, `cart.html`, `css/style.css`,
`js/menu.js`, ...) and streams its bytes back — this is the frontend.
`"/api/categories"`, `"/api/menu-items"`, etc. are mapped to handler
classes that read/write the database — this is the backend. Because it's
all one `HttpServer` on port 8080, the browser can call
`fetch("/api/...")` with a relative URL, exactly like the Spring Boot
version, and a click on `<a href="cart.html">` just navigates the browser
to a plain file the same server hands back.

**2. No `@RestController`, so routing is manual.** Spring Boot lets you
write `@GetMapping("/api/orders/{id}")` and it figures out routing for
you. Here, `OrderHandler.handle(exchange)` receives *every* request under
`/api/orders` and has to work out for itself, by splitting the URL path
into segments (`HttpUtil.pathSegments`), whether this is
`GET /api/orders`, `GET /api/orders/5`, or
`PATCH /api/orders/5/status`. This is the main structural difference from
the Spring version — one handler class per resource, with an if/else
chain instead of annotations.

**3. No JPA, so SQL is manual.** Instead of `menuItemRepository.findAll()`
building a `SELECT` for you, `MenuItemHandler` writes the SQL directly:
```java
Statement st = conn.createStatement();
ResultSet rs = st.executeQuery("SELECT ... FROM menu_item ...");
```
and loops over the `ResultSet` to build a `Map` per row, which `Json.java`
then turns into a JSON array. `DB.java` opens a fresh JDBC `Connection`
per request (`DriverManager.getConnection(...)`) using the MySQL driver
jar in `lib/`; the connection auto-closes via try-with-resources.

**4. No Jackson, so JSON is hand-rolled.** `Json.java` is a small
recursive-descent parser (`Json.parse`) and a matching writer
(`Json.write`) — together maybe 250 lines — that convert between JSON
text and Java `Map`/`List`/`String`/`Double`/`Boolean`. Every handler uses
it to read a request body (`Json.parseObject(...)`) and to build a
response (`HttpUtil.sendJson(exchange, 200, someMap)`).

**5. No Spring MultipartFile, so file uploads are hand-rolled too.**
`MultipartParser.java` manually splits the raw request bytes on the
`multipart/form-data` boundary to pull out the uploaded image's bytes and
filename. `FileStorage.java` then saves those bytes under
`uploads/food/` or `uploads/qr/` with a random filename, and returns a
`/uploads/...` URL. `StaticFileHandler` (also registered on `/uploads`)
serves that file back to the browser when the `<img>` tag loads it.

**6. The frontend is a normal multi-page site, just like the very first
version.** `index.html`, `cart.html`, `checkout.html`,
`order-success.html`, and the six `admin-*.html` pages each load
`js/api.js` plus their own page-specific script (`menu.js`, `cart.js`,
`checkout.js`, `admin.js`, `orders.js`, `categories.js`, `payment.js`).
Cart data lives in `localStorage`; admin login uses a `sessionStorage`
flag checked by `requireAdminLogin()` at the top of every protected admin
page. None of this changed from the Spring Boot version — only what
serves those files (a hand-written `StaticFileHandler` instead of
Spring's automatic static-resource handling) and what answers the
`/api/...` calls (plain JDBC handlers instead of Spring controllers)
changed.

**Full request/response flow, summarized:**
```
Browser (plain multi-page HTML/CSS/JS)
   │  fetch("/api/...")  OR  <a href="cart.html">
   ▼
HttpServer routes by URL prefix to a Handler class
   │
   ▼
Handler parses the path/JSON body by hand, opens a JDBC Connection
   │
   ▼
Plain SQL (PreparedStatement / Statement) runs against MySQL
   │  (ResultSet rows -> Map -> JSON, sent back the same way)
   ▼
Browser's fetch() promise resolves -> JS updates the current page
```

Same concept as the Spring Boot version end to end — menu stored in
MySQL and fetched live, cart in the browser until checkout, order
insert with server-side price lookup, QR-based manual online payment,
admin CRUD for menu/categories/orders/QR, and the persistent
order-tracker banner — just built entirely with what ships inside the
JDK, and served as separate HTML/CSS/JS files instead of one combined
file.
