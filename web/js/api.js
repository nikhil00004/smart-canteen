/* =========================================================
   api.js
   Central place for every fetch() call to the Spring Boot backend.
   Because the frontend files live inside
   src/main/resources/static, they are served by the SAME
   Spring Boot server as the REST API. That means we can use a
   relative URL like "/api/..." instead of a full domain name.
   ========================================================= */

const API_BASE_URL = "/api";

/**
 * Generic helper that wraps fetch(), adds JSON headers when needed,
 * and turns a non-2xx response into a thrown Error with the backend's
 * error message (see GlobalExceptionHandler on the Java side).
 */
async function apiRequest(path, options = {}) {
  const response = await fetch(API_BASE_URL + path, options);

  // No content (e.g. DELETE) -> nothing to parse
  if (response.status === 204) {
    return null;
  }

  const data = await response.json().catch(() => null);

  if (!response.ok) {
    const message = (data && data.message) ? data.message : "Request failed (" + response.status + ")";
    throw new Error(message);
  }

  return data;
}

function apiGet(path) {
  return apiRequest(path, { method: "GET" });
}

function apiPost(path, body) {
  return apiRequest(path, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body)
  });
}

function apiPut(path, body) {
  return apiRequest(path, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body)
  });
}

function apiPatch(path, body) {
  return apiRequest(path, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body)
  });
}

function apiDelete(path) {
  return apiRequest(path, { method: "DELETE" });
}

// For multipart/form-data requests (image uploads) - do NOT set
// Content-Type manually, the browser sets the correct boundary itself.
function apiPostForm(path, formData) {
  return apiRequest(path, { method: "POST", body: formData });
}

function apiPutForm(path, formData) {
  return apiRequest(path, { method: "PUT", body: formData });
}

/* ---------------- Specific API functions ---------------- */

// Categories
const CategoryAPI = {
  getAll: () => apiGet("/categories"),
  create: (dto) => apiPost("/categories", dto),
  update: (id, dto) => apiPut("/categories/" + id, dto),
  remove: (id) => apiDelete("/categories/" + id)
};

// Menu items
const MenuAPI = {
  getAll: () => apiGet("/menu-items"),
  getByCategory: (categoryId) => apiGet("/menu-items/category/" + categoryId),
  create: (formData) => apiPostForm("/menu-items", formData),
  update: (id, formData) => apiPutForm("/menu-items/" + id, formData),
  updateAvailability: (id, available) => apiRequest("/menu-items/" + id + "/availability", {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ available })
  }),
  remove: (id) => apiDelete("/menu-items/" + id)
};

// Orders
const OrderAPI = {
  place: (orderRequest) => apiPost("/orders", orderRequest),
  getAll: () => apiGet("/orders"),
  getById: (id) => apiGet("/orders/" + id),
  updateStatus: (id, status) => apiPatch("/orders/" + id + "/status", { status }),
  updatePaymentStatus: (id, status) => apiPatch("/orders/" + id + "/payment-status", { status })
};

// Payment QR
const PaymentQRAPI = {
  getActive: () => apiGet("/payment-qr/active"),
  upload: (formData) => apiPostForm("/payment-qr", formData)
};

// Admin
const AdminAPI = {
  login: (username, password) => apiPost("/admin/login", { username, password }),
  dashboardStats: () => apiGet("/admin/dashboard-stats")
};

/* ---------------- Toast notifications ---------------- */
function showToast(message, type = "success") {
  let container = document.getElementById("toast-container");
  if (!container) {
    container = document.createElement("div");
    container.id = "toast-container";
    document.body.appendChild(container);
  }

  const toast = document.createElement("div");
  toast.className = "toast " + type;
  toast.textContent = message;
  container.appendChild(toast);

  setTimeout(() => toast.remove(), 3000);
}

/* ---------------- Currency helper ---------------- */
function formatRupees(amount) {
  return "₹" + Number(amount).toFixed(2).replace(/\.00$/, "");
}
