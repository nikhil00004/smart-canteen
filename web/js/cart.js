/* =========================================================
   cart.js
   Handles the shopping cart using the browser's localStorage,
   so the cart survives a page refresh but is cleared once the
   order is placed. Shared by index.html, cart.html and checkout.html.

   Cart shape stored in localStorage under key "smartCanteenCart":
   [
     { menuItemId: 3, name: "Poha", price: 30, quantity: 2, imagePath: null },
     ...
   ]
   ========================================================= */

const CART_STORAGE_KEY = "smartCanteenCart";

function getCart() {
  const raw = localStorage.getItem(CART_STORAGE_KEY);
  return raw ? JSON.parse(raw) : [];
}

function saveCart(cart) {
  localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(cart));
  updateCartCountBadge();
}

function clearCart() {
  localStorage.removeItem(CART_STORAGE_KEY);
  updateCartCountBadge();
}

function addToCart(menuItem) {
  const cart = getCart();
  const existing = cart.find(line => line.menuItemId === menuItem.id);

  if (existing) {
    existing.quantity += 1;
  } else {
    cart.push({
      menuItemId: menuItem.id,
      name: menuItem.name,
      price: menuItem.price,
      quantity: 1,
      imagePath: menuItem.imagePath
    });
  }

  saveCart(cart);
  showToast(menuItem.name + " added to cart", "success");
}

function getCartQuantity(menuItemId) {
  const line = getCart().find(l => l.menuItemId === menuItemId);
  return line ? line.quantity : 0;
}

function increaseQuantity(menuItemId) {
  const cart = getCart();
  const line = cart.find(l => l.menuItemId === menuItemId);
  if (line) {
    line.quantity += 1;
    saveCart(cart);
  }
  return cart;
}

function decreaseQuantity(menuItemId) {
  let cart = getCart();
  const line = cart.find(l => l.menuItemId === menuItemId);
  if (line) {
    line.quantity -= 1;
    if (line.quantity <= 0) {
      cart = cart.filter(l => l.menuItemId !== menuItemId);
    }
    saveCart(cart);
  }
  return cart;
}

function removeFromCart(menuItemId) {
  const cart = getCart().filter(l => l.menuItemId !== menuItemId);
  saveCart(cart);
  return cart;
}

function getCartTotal() {
  return getCart().reduce((sum, line) => sum + (line.price * line.quantity), 0);
}

function getCartItemCount() {
  return getCart().reduce((sum, line) => sum + line.quantity, 0);
}

// Updates the little badge on the cart icon in the navbar, on every page.
function updateCartCountBadge() {
  const badge = document.querySelectorAll(".cart-count");
  const count = getCartItemCount();
  badge.forEach(el => { el.textContent = count; });
}

/* =========================================================
   Active order tracking
   Keeps showing the customer's most recent order (as a small
   sticky banner on every page) until it reaches COMPLETED or
   CANCELLED, so they don't lose track of it after leaving the
   order-success page.
   ========================================================= */

const ACTIVE_ORDER_KEY = "smartCanteenActiveOrderId";

function setActiveOrder(orderId) {
  localStorage.setItem(ACTIVE_ORDER_KEY, orderId);
}

function getActiveOrderId() {
  return localStorage.getItem(ACTIVE_ORDER_KEY);
}

function clearActiveOrder() {
  localStorage.removeItem(ACTIVE_ORDER_KEY);
}

// Called on every customer page (except order-success, which has its own
// full detail view) to show a small "Order #12 - PREPARING" strip.
async function renderOrderTrackingBanner() {
  const orderId = getActiveOrderId();
  if (!orderId) return;

  // don't show the mini banner on the full order-success page itself
  if (window.location.pathname.endsWith("order-success.html")) return;

  try {
    const order = await OrderAPI.getById(orderId);

    if (order.orderStatus === "COMPLETED" || order.orderStatus === "CANCELLED") {
      clearActiveOrder();
      return;
    }

    let banner = document.getElementById("order-tracker-banner");
    if (!banner) {
      banner = document.createElement("a");
      banner.id = "order-tracker-banner";
      banner.className = "order-tracker-banner";
      document.body.prepend(banner);
    }

    banner.href = "order-success.html?orderId=" + order.id;
    banner.innerHTML = `📦 Order #${order.id} — <strong>${order.orderStatus}</strong> &nbsp; (tap for details)`;
  } catch (err) {
    // order not found or server error - just don't show the banner
  }
}

document.addEventListener("DOMContentLoaded", () => {
  updateCartCountBadge();
  renderOrderTrackingBanner();
  // Refresh the status every 15 seconds while the customer stays on a page
  setInterval(renderOrderTrackingBanner, 15000);
});
