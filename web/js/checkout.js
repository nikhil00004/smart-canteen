/* =========================================================
   checkout.js
   Handles the checkout page: shows order summary, validates
   name/mobile, lets the customer choose Online/Offline payment,
   fetches the active QR code for online payment, and finally
   sends the order to POST /api/orders.
   ========================================================= */

let selectedPaymentMethod = null;
let onlinePaymentConfirmed = false;

document.addEventListener("DOMContentLoaded", () => {
  const cart = getCart();

  if (cart.length === 0) {
    window.location.href = "index.html";
    return;
  }

  renderOrderSummary(cart);
  setupPaymentOptions();
  setupForm();
});

function renderOrderSummary(cart) {
  const linesEl = document.getElementById("order-lines");
  linesEl.innerHTML = cart.map(line => `
    <div class="order-line">
      <span>${escapeHtml(line.name)} x ${line.quantity}</span>
      <span>${formatRupees(line.price * line.quantity)}</span>
    </div>
  `).join("");

  document.getElementById("order-total").textContent = formatRupees(getCartTotal());
  document.getElementById("qr-amount").textContent = formatRupees(getCartTotal());
}

function setupPaymentOptions() {
  const onlineOption = document.getElementById("payment-online");
  const offlineOption = document.getElementById("payment-offline");
  const qrBox = document.getElementById("qr-box");
  const offlineBox = document.getElementById("offline-box");
  const placeOrderBtn = document.getElementById("place-order-btn");

  onlineOption.addEventListener("click", async () => {
    selectedPaymentMethod = "ONLINE";
    onlinePaymentConfirmed = false;
    onlineOption.classList.add("selected");
    offlineOption.classList.remove("selected");
    offlineBox.classList.remove("show");
    qrBox.classList.add("show");
    placeOrderBtn.disabled = true;
    placeOrderBtn.textContent = "Place Order";
    await loadQrCode();
  });

  offlineOption.addEventListener("click", () => {
    selectedPaymentMethod = "OFFLINE";
    offlineOption.classList.add("selected");
    onlineOption.classList.remove("selected");
    qrBox.classList.remove("show");
    offlineBox.classList.add("show");
    placeOrderBtn.disabled = false;
    placeOrderBtn.textContent = "Place Order";
  });

  document.getElementById("payment-done-btn").addEventListener("click", () => {
    onlinePaymentConfirmed = true;
    placeOrderBtn.disabled = false;
    showToast("Thanks! You can now place your order.", "success");
  });
}

async function loadQrCode() {
  const qrImg = document.getElementById("qr-image");
  const qrLoading = document.getElementById("qr-loading");
  qrLoading.style.display = "block";
  qrImg.style.display = "none";

  try {
    const qr = await PaymentQRAPI.getActive();
    qrImg.src = qr.imagePath;
    qrImg.style.display = "block";
  } catch (err) {
    qrLoading.textContent = "QR code not available right now. Please choose Offline Payment or try again later.";
    return;
  }
  qrLoading.style.display = "none";
}

function setupForm() {
  const form = document.getElementById("checkout-form");
  form.addEventListener("submit", handleSubmit);
}

async function handleSubmit(e) {
  e.preventDefault();

  const nameInput = document.getElementById("customer-name");
  const mobileInput = document.getElementById("mobile-number");
  const nameError = document.getElementById("name-error");
  const mobileError = document.getElementById("mobile-error");

  nameError.textContent = "";
  mobileError.textContent = "";

  let valid = true;

  if (!nameInput.value.trim()) {
    nameError.textContent = "Please enter your name";
    valid = false;
  }

  const mobilePattern = /^[6-9]\d{9}$/;
  if (!mobilePattern.test(mobileInput.value.trim())) {
    mobileError.textContent = "Enter a valid 10-digit mobile number";
    valid = false;
  }

  if (!selectedPaymentMethod) {
    showToast("Please select a payment method", "error");
    valid = false;
  }

  if (selectedPaymentMethod === "ONLINE" && !onlinePaymentConfirmed) {
    showToast('Please click "I Have Completed Payment" after scanning the QR code', "error");
    valid = false;
  }

  if (!valid) return;

  const placeOrderBtn = document.getElementById("place-order-btn");
  placeOrderBtn.disabled = true;
  placeOrderBtn.textContent = "Placing Order...";

  const cart = getCart();
  const orderRequest = {
    customerName: nameInput.value.trim(),
    mobileNumber: mobileInput.value.trim(),
    paymentMethod: selectedPaymentMethod,
    items: cart.map(line => ({ menuItemId: line.menuItemId, quantity: line.quantity }))
  };

  try {
    const order = await OrderAPI.place(orderRequest);
    clearCart();
    localStorage.setItem("lastOrderId", order.id);
    setActiveOrder(order.id); // keep tracking this order on every page until it's completed
    window.location.href = "order-success.html?orderId=" + order.id;
  } catch (err) {
    showToast(err.message, "error");
    placeOrderBtn.disabled = false;
    placeOrderBtn.textContent = "Place Order";
  }
}

function escapeHtml(str) {
  const div = document.createElement("div");
  div.textContent = str;
  return div.innerHTML;
}
