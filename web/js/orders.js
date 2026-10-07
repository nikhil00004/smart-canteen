/* =========================================================
   orders.js
   Used by admin-orders.html: lists every order and lets the
   admin update order status and payment status.
   ========================================================= */

document.addEventListener("DOMContentLoaded", () => {
  if (document.getElementById("orders-body")) {
    loadOrders();
  }
});

async function loadOrders() {
  const tbody = document.getElementById("orders-body");
  tbody.innerHTML = `<tr><td colspan="9" style="text-align:center;">Loading orders...</td></tr>`;

  try {
    const orders = await OrderAPI.getAll();

    if (orders.length === 0) {
      tbody.innerHTML = `<tr><td colspan="9" style="text-align:center; color:#888;">No orders placed yet</td></tr>`;
      return;
    }

    tbody.innerHTML = orders.map(renderOrderRow).join("");
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="9" style="text-align:center; color:#d32f2f;">${err.message}</td></tr>`;
  }
}

function renderOrderRow(order) {
  const itemsSummary = order.items.map(i => `${escapeHtml(i.foodName)} x${i.quantity}`).join(", ");

  const paymentStatusOptions = order.paymentMethod === "ONLINE"
    ? ["PENDING_VERIFICATION", "PAID", "REJECTED"]
    : ["PENDING", "PAID"];

  return `
    <tr>
      <td>#${order.id}</td>
      <td>${escapeHtml(order.customerName)}<br><span style="color:#888; font-size:0.8rem;">${escapeHtml(order.mobileNumber)}</span></td>
      <td style="max-width:220px;">${escapeHtml(itemsSummary)}</td>
      <td>${formatRupees(order.totalAmount)}</td>
      <td>${order.paymentMethod}</td>
      <td>
        <select class="status-select" onchange="changePaymentStatus(${order.id}, this.value)">
          ${paymentStatusOptions.map(s => `<option value="${s}" ${s === order.paymentStatus ? "selected" : ""}>${s.replace("_", " ")}</option>`).join("")}
        </select>
      </td>
      <td>
        <select class="status-select" onchange="changeOrderStatus(${order.id}, this.value)">
          ${["PENDING", "PREPARING", "READY", "COMPLETED", "CANCELLED"].map(s => `<option value="${s}" ${s === order.orderStatus ? "selected" : ""}>${s}</option>`).join("")}
        </select>
      </td>
      <td>${formatDate(order.orderDate)}</td>
    </tr>
  `;
}

async function changeOrderStatus(id, status) {
  try {
    await OrderAPI.updateStatus(id, status);
    showToast("Order #" + id + " status updated to " + status, "success");
  } catch (err) {
    showToast(err.message, "error");
    loadOrders();
  }
}

async function changePaymentStatus(id, status) {
  try {
    await OrderAPI.updatePaymentStatus(id, status);
    showToast("Payment status updated to " + status, "success");
  } catch (err) {
    showToast(err.message, "error");
    loadOrders();
  }
}
