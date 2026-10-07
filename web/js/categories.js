/* =========================================================
   categories.js
   Used by admin-categories.html: list, add, edit, delete categories.
   ========================================================= */

let editingCategoryId = null;

document.addEventListener("DOMContentLoaded", () => {
  if (document.getElementById("categories-body")) {
    loadCategories();
  }
});

async function loadCategories() {
  const tbody = document.getElementById("categories-body");
  tbody.innerHTML = `<tr><td colspan="3" style="text-align:center;">Loading categories...</td></tr>`;

  try {
    const categories = await CategoryAPI.getAll();

    if (categories.length === 0) {
      tbody.innerHTML = `<tr><td colspan="3" style="text-align:center; color:#888;">No categories yet. Click "Add Category" to create one.</td></tr>`;
      return;
    }

    tbody.innerHTML = categories.map(cat => `
      <tr>
        <td>#${cat.id}</td>
        <td>${escapeHtml(cat.name)}</td>
        <td class="action-icons">
          <button class="icon-btn" title="Edit" onclick="openEditCategoryModal(${cat.id}, '${escapeJs(cat.name)}')">✏️</button>
          <button class="icon-btn delete-icon" title="Delete" onclick="deleteCategory(${cat.id})">🗑️</button>
        </td>
      </tr>
    `).join("");
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="3" style="text-align:center; color:#d32f2f;">${err.message}</td></tr>`;
  }
}

function openAddCategoryModal() {
  editingCategoryId = null;
  document.getElementById("category-modal-title").textContent = "Add Category";
  document.getElementById("category-name").value = "";
  document.getElementById("category-modal").classList.add("show");
}

function openEditCategoryModal(id, name) {
  editingCategoryId = id;
  document.getElementById("category-modal-title").textContent = "Edit Category";
  document.getElementById("category-name").value = name;
  document.getElementById("category-modal").classList.add("show");
}

function closeCategoryModal() {
  document.getElementById("category-modal").classList.remove("show");
}

async function submitCategoryForm(e) {
  e.preventDefault();
  const name = document.getElementById("category-name").value.trim();
  if (!name) return;

  const submitBtn = document.getElementById("category-submit-btn");
  submitBtn.disabled = true;
  submitBtn.textContent = "Saving...";

  try {
    if (editingCategoryId) {
      await CategoryAPI.update(editingCategoryId, { name });
      showToast("Category updated", "success");
    } else {
      await CategoryAPI.create({ name });
      showToast("Category added", "success");
    }
    closeCategoryModal();
    await loadCategories();
  } catch (err) {
    showToast(err.message, "error");
  } finally {
    submitBtn.disabled = false;
    submitBtn.textContent = "Save";
  }
}

async function deleteCategory(id) {
  if (!confirm("Delete this category? It must have no food items in it.")) return;
  try {
    await CategoryAPI.remove(id);
    showToast("Category deleted", "success");
    await loadCategories();
  } catch (err) {
    showToast(err.message, "error");
  }
}

function escapeHtml(str) {
  const div = document.createElement("div");
  div.textContent = str;
  return div.innerHTML;
}

function escapeJs(str) {
  return String(str).replace(/'/g, "\\'");
}
