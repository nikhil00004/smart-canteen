/* =========================================================
   admin.js
   Shared admin functionality:
   - Login (admin-login.html)
   - Simple session check that protects the other admin pages
   - Sidebar toggle / logout (every admin page)
   - Dashboard stats (admin-dashboard.html)
   - Food menu management: list/add/edit/delete/availability (admin-menu.html)

   SESSION NOTE: authentication is intentionally simple for this college
   project. After a successful login we just store a flag in
   sessionStorage. Every other admin page checks that flag on load
   (see requireAdminLogin below) and redirects to the login page if it
   is missing. This is NOT secure enough for a real production system
   (a user could set the flag manually in devtools) but satisfies the
   "simple frontend session check" requirement for a college project.
   ========================================================= */

const ADMIN_SESSION_KEY = "smartCanteenAdminLoggedIn";

/* ---------------- Session helpers ---------------- */

function requireAdminLogin() {
  if (sessionStorage.getItem(ADMIN_SESSION_KEY) !== "true") {
    window.location.href = "admin-login.html";
  }
}

function adminLogout() {
  sessionStorage.removeItem(ADMIN_SESSION_KEY);
  window.location.href = "admin-login.html";
}

function setupSidebar() {
  const toggleBtn = document.getElementById("sidebar-toggle-btn");
  const sidebar = document.getElementById("sidebar");
  if (toggleBtn && sidebar) {
    toggleBtn.addEventListener("click", () => sidebar.classList.toggle("open"));
  }
  const logoutBtns = document.querySelectorAll(".logout-btn");
  logoutBtns.forEach(btn => btn.addEventListener("click", adminLogout));
}

/* ---------------- Login page ---------------- */

function setupLoginForm() {
  const form = document.getElementById("admin-login-form");
  if (!form) return;

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    const username = document.getElementById("admin-username").value.trim();
    const password = document.getElementById("admin-password").value;
    const errorEl = document.getElementById("login-error");
    errorEl.textContent = "";

    const btn = document.getElementById("login-btn");
    btn.disabled = true;
    btn.textContent = "Logging in...";

    try {
      const result = await AdminAPI.login(username, password);
      if (result.success) {
        sessionStorage.setItem(ADMIN_SESSION_KEY, "true");
        window.location.href = "admin-dashboard.html";
      } else {
        errorEl.textContent = result.message || "Invalid username or password";
      }
    } catch (err) {
      errorEl.textContent = err.message;
    } finally {
      btn.disabled = false;
      btn.textContent = "Login";
    }
  });
}

/* ---------------- Dashboard page ---------------- */

async function loadDashboardStats() {
  const grid = document.getElementById("stats-grid");
  if (!grid) return;

  try {
    const stats = await AdminAPI.dashboardStats();
    document.getElementById("stat-total").textContent = stats.totalOrders;
    document.getElementById("stat-pending").textContent = stats.pendingOrders;
    document.getElementById("stat-preparing").textContent = stats.preparingOrders;
    document.getElementById("stat-ready").textContent = stats.readyOrders;
    document.getElementById("stat-completed").textContent = stats.completedOrders;
  } catch (err) {
    showToast("Could not load dashboard stats: " + err.message, "error");
  }

  loadRecentOrders();
}

async function loadRecentOrders() {
  const tbody = document.getElementById("recent-orders-body");
  if (!tbody) return;

  try {
    const orders = await OrderAPI.getAll();
    const recent = orders.slice(0, 6);

    if (recent.length === 0) {
      tbody.innerHTML = `<tr><td colspan="6" style="text-align:center; color:#888;">No orders yet</td></tr>`;
      return;
    }

    tbody.innerHTML = recent.map(order => `
      <tr>
        <td>#${order.id}</td>
        <td>${escapeHtml(order.customerName)}</td>
        <td>${formatRupees(order.totalAmount)}</td>
        <td>${order.paymentMethod}</td>
        <td><span class="badge badge-${order.orderStatus.toLowerCase()}">${order.orderStatus}</span></td>
        <td>${formatDate(order.orderDate)}</td>
      </tr>
    `).join("");
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="6" style="text-align:center; color:#d32f2f;">${err.message}</td></tr>`;
  }
}

/* ---------------- Menu Management page (admin-menu.html) ---------------- */

let adminCategories = [];
let adminMenuItems = [];
let editingMenuItemId = null;

async function loadMenuManagementPage() {
  const tbody = document.getElementById("menu-items-body");
  if (!tbody) return;

  try {
    const [categories, items] = await Promise.all([CategoryAPI.getAll(), MenuAPI.getAll()]);
    adminCategories = categories;
    adminMenuItems = items;
    populateCategoryDropdown();
    renderMenuItemsTable();
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="6" style="text-align:center; color:#d32f2f;">${err.message}</td></tr>`;
  }
}

function populateCategoryDropdown() {
  const select = document.getElementById("item-category");
  if (!select) return;
  select.innerHTML = adminCategories.map(c => `<option value="${c.id}">${escapeHtml(c.name)}</option>`).join("");
}

function renderMenuItemsTable() {
  const tbody = document.getElementById("menu-items-body");

  if (adminMenuItems.length === 0) {
    tbody.innerHTML = `<tr><td colspan="6" style="text-align:center; color:#888;">No food items yet. Click "Add Food Item" to create one.</td></tr>`;
    return;
  }

  tbody.innerHTML = adminMenuItems.map(item => `
    <tr>
      <td>${item.imagePath ? `<img class="thumb" src="${item.imagePath}">` : `<div class="thumb-placeholder">🍽️</div>`}</td>
      <td>${escapeHtml(item.name)}</td>
      <td>${escapeHtml(item.categoryName)}</td>
      <td>${formatRupees(item.price)}</td>
      <td>
        <label class="toggle-switch">
          <input type="checkbox" ${item.available ? "checked" : ""} onchange="toggleAvailability(${item.id}, this.checked)">
          ${item.available ? "Available" : "Unavailable"}
        </label>
      </td>
      <td class="action-icons">
        <button class="icon-btn" title="Edit" onclick="openEditItemModal(${item.id})">✏️</button>
        <button class="icon-btn delete-icon" title="Delete" onclick="deleteMenuItem(${item.id})">🗑️</button>
      </td>
    </tr>
  `).join("");
}

async function toggleAvailability(id, available) {
  try {
    await MenuAPI.updateAvailability(id, available);
    showToast("Availability updated", "success");
    await loadMenuManagementPage();
  } catch (err) {
    showToast(err.message, "error");
  }
}

function openAddItemModal() {
  editingMenuItemId = null;
  document.getElementById("item-modal-title").textContent = "Add Food Item";
  document.getElementById("item-form").reset();
  document.getElementById("item-available").checked = true;
  document.getElementById("item-modal").classList.add("show");
}

function openEditItemModal(id) {
  const item = adminMenuItems.find(i => i.id === id);
  if (!item) return;

  editingMenuItemId = id;
  document.getElementById("item-modal-title").textContent = "Edit Food Item";
  document.getElementById("item-name").value = item.name;
  document.getElementById("item-price").value = item.price;
  document.getElementById("item-category").value = item.categoryId;
  document.getElementById("item-available").checked = item.available;
  document.getElementById("item-image").value = "";
  document.getElementById("item-modal").classList.add("show");
}

function closeItemModal() {
  document.getElementById("item-modal").classList.remove("show");
}

async function submitItemForm(e) {
  e.preventDefault();

  const dto = {
    name: document.getElementById("item-name").value.trim(),
    price: parseFloat(document.getElementById("item-price").value),
    categoryId: parseInt(document.getElementById("item-category").value, 10),
    available: document.getElementById("item-available").checked
  };

  const imageInput = document.getElementById("item-image");
  const formData = new FormData();
  formData.append("item", JSON.stringify(dto));
  if (imageInput.files.length > 0) {
    formData.append("image", imageInput.files[0]);
  }

  const submitBtn = document.getElementById("item-submit-btn");
  submitBtn.disabled = true;
  submitBtn.textContent = "Saving...";

  try {
    if (editingMenuItemId) {
      await MenuAPI.update(editingMenuItemId, formData);
      showToast("Food item updated", "success");
    } else {
      await MenuAPI.create(formData);
      showToast("Food item added", "success");
    }
    closeItemModal();
    await loadMenuManagementPage();
  } catch (err) {
    showToast(err.message, "error");
  } finally {
    submitBtn.disabled = false;
    submitBtn.textContent = "Save";
  }
}

async function deleteMenuItem(id) {
  if (!confirm("Delete this food item? This cannot be undone.")) return;
  try {
    await MenuAPI.remove(id);
    showToast("Food item deleted", "success");
    await loadMenuManagementPage();
  } catch (err) {
    showToast(err.message, "error");
  }
}

/* ---------------- Shared helpers ---------------- */

function escapeHtml(str) {
  const div = document.createElement("div");
  div.textContent = str;
  return div.innerHTML;
}

function formatDate(isoString) {
  const d = new Date(isoString);
  return d.toLocaleString("en-IN", { day: "2-digit", month: "short", hour: "2-digit", minute: "2-digit" });
}

document.addEventListener("DOMContentLoaded", () => {
  setupSidebar();
  setupLoginForm();
});
