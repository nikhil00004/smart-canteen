/* =========================================================
   menu.js
   Loads the food menu on index.html dynamically from the backend
   (GET /api/categories and GET /api/menu-items) and renders it
   grouped by category. Also wires up "Add to Cart" / quantity buttons.
   ========================================================= */

let allCategories = [];
let allMenuItems = [];

document.addEventListener("DOMContentLoaded", () => {
  loadMenu();
  setupNavToggle();
});

async function loadMenu() {
  const container = document.getElementById("menu-container");
  container.innerHTML = renderLoadingState();

  try {
    const [categories, items] = await Promise.all([
      CategoryAPI.getAll(),
      MenuAPI.getAll()
    ]);

    allCategories = categories;
    allMenuItems = items;

    if (categories.length === 0) {
      container.innerHTML = renderEmptyState("No menu categories yet. Please check back soon!");
      return;
    }

    renderCategoryTabs(categories);
    renderMenu(allCategories, allMenuItems);
  } catch (err) {
    container.innerHTML = renderEmptyState("Could not load the menu. " + err.message);
  }
}

function renderCategoryTabs(categories) {
  const tabsEl = document.getElementById("category-tabs");
  tabsEl.innerHTML = "";

  const allTab = document.createElement("button");
  allTab.className = "category-tab active";
  allTab.textContent = "All";
  allTab.onclick = () => selectTab(allTab, () => renderMenu(allCategories, allMenuItems));
  tabsEl.appendChild(allTab);

  categories.forEach(cat => {
    const tab = document.createElement("button");
    tab.className = "category-tab";
    tab.textContent = cat.name;
    tab.onclick = () => selectTab(tab, () => renderMenu([cat], allMenuItems));
    tabsEl.appendChild(tab);
  });
}

function selectTab(clickedTab, renderFn) {
  document.querySelectorAll(".category-tab").forEach(t => t.classList.remove("active"));
  clickedTab.classList.add("active");
  renderFn();
}

function renderMenu(categories, items) {
  const container = document.getElementById("menu-container");
  container.innerHTML = "";

  let hasAnyItem = false;

  categories.forEach(category => {
    const categoryItems = items.filter(item => item.categoryId === category.id);
    if (categoryItems.length === 0) return;
    hasAnyItem = true;

    const block = document.createElement("div");
    block.className = "category-block";
    block.innerHTML = `
      <h2 class="category-heading">${escapeHtml(category.name)}</h2>
      <div class="food-grid" id="grid-${category.id}"></div>
    `;
    container.appendChild(block);

    const grid = block.querySelector(".food-grid");
    categoryItems.forEach(item => grid.appendChild(renderFoodCard(item)));
  });

  if (!hasAnyItem) {
    container.innerHTML = renderEmptyState("No food items found in this category yet.");
  }
}

function renderFoodCard(item) {
  const card = document.createElement("div");
  card.className = "food-card";

  const imageHtml = item.imagePath
    ? `<img class="food-img" src="${item.imagePath}" alt="${escapeHtml(item.name)}">`
    : `<div class="food-img-placeholder">🍽️</div>`;

  const qty = getCartQuantity(item.id);

  card.innerHTML = `
    ${imageHtml}
    <div class="food-info">
      <div class="food-name">${escapeHtml(item.name)}</div>
      <div class="food-price">${formatRupees(item.price)}</div>
      ${!item.available ? '<span class="food-unavailable-badge">Unavailable</span>' : ""}
      <div class="card-action"></div>
    </div>
  `;

  const actionEl = card.querySelector(".card-action");

  if (!item.available) {
    const btn = document.createElement("button");
    btn.className = "add-to-cart-btn";
    btn.disabled = true;
    btn.textContent = "Not Available";
    actionEl.appendChild(btn);
  } else if (qty > 0) {
    actionEl.appendChild(buildQtyControl(item));
  } else {
    const btn = document.createElement("button");
    btn.className = "add-to-cart-btn";
    btn.textContent = "Add to Cart";
    btn.onclick = () => {
      addToCart(item);
      const newControl = buildQtyControl(item);
      actionEl.innerHTML = "";
      actionEl.appendChild(newControl);
    };
    actionEl.appendChild(btn);
  }

  return card;
}

function buildQtyControl(item) {
  const wrap = document.createElement("div");
  wrap.className = "qty-control";

  const minusBtn = document.createElement("button");
  minusBtn.textContent = "-";

  const qtySpan = document.createElement("span");
  qtySpan.textContent = getCartQuantity(item.id);

  const plusBtn = document.createElement("button");
  plusBtn.textContent = "+";

  minusBtn.onclick = () => {
    decreaseQuantity(item.id);
    const newQty = getCartQuantity(item.id);
    if (newQty === 0) {
      const card = wrap.closest(".food-card");
      const actionEl = card.querySelector(".card-action");
      const btn = document.createElement("button");
      btn.className = "add-to-cart-btn";
      btn.textContent = "Add to Cart";
      btn.onclick = () => {
        addToCart(item);
        actionEl.innerHTML = "";
        actionEl.appendChild(buildQtyControl(item));
      };
      actionEl.innerHTML = "";
      actionEl.appendChild(btn);
    } else {
      qtySpan.textContent = newQty;
    }
  };

  plusBtn.onclick = () => {
    increaseQuantity(item.id);
    qtySpan.textContent = getCartQuantity(item.id);
  };

  wrap.appendChild(minusBtn);
  wrap.appendChild(qtySpan);
  wrap.appendChild(plusBtn);
  return wrap;
}

function renderLoadingState() {
  return `<div class="state-box"><div class="spinner"></div><p>Loading delicious food...</p></div>`;
}

function renderEmptyState(message) {
  return `<div class="state-box"><div class="icon">🍽️</div><p>${escapeHtml(message)}</p></div>`;
}

function escapeHtml(str) {
  const div = document.createElement("div");
  div.textContent = str;
  return div.innerHTML;
}

function setupNavToggle() {
  const hamburger = document.getElementById("hamburger-btn");
  const navLinks = document.getElementById("nav-links");
  if (hamburger && navLinks) {
    hamburger.addEventListener("click", () => navLinks.classList.toggle("open"));
  }
}

function scrollToMenu() {
  document.getElementById("menu-section").scrollIntoView({ behavior: "smooth" });
}
