/* =========================================================
   payment.js
   Used by admin-payment.html: shows the currently active QR code
   and lets the admin upload a new one (which replaces the old one).
   ========================================================= */

document.addEventListener("DOMContentLoaded", () => {
  if (document.getElementById("current-qr-box")) {
    loadCurrentQr();
    setupQrUploadForm();
  }
});

async function loadCurrentQr() {
  const box = document.getElementById("current-qr-box");

  try {
    const qr = await PaymentQRAPI.getActive();
    box.innerHTML = `
      <img class="qr-preview" src="${qr.imagePath}" alt="Active payment QR code">
      <p style="color:#6b7280; font-size:0.85rem;">Last updated: ${formatDate(qr.updatedAt)}</p>
    `;
  } catch (err) {
    box.innerHTML = `<div class="state-box"><div class="icon">📷</div><p>No QR code uploaded yet. Upload one below.</p></div>`;
  }
}

function setupQrUploadForm() {
  const form = document.getElementById("qr-upload-form");
  form.addEventListener("submit", async (e) => {
    e.preventDefault();

    const fileInput = document.getElementById("qr-file-input");
    if (fileInput.files.length === 0) {
      showToast("Please choose a QR code image", "error");
      return;
    }

    const formData = new FormData();
    formData.append("qrImage", fileInput.files[0]);

    const btn = document.getElementById("qr-upload-btn");
    btn.disabled = true;
    btn.textContent = "Uploading...";

    try {
      await PaymentQRAPI.upload(formData);
      showToast("QR code updated successfully", "success");
      fileInput.value = "";
      await loadCurrentQr();
    } catch (err) {
      showToast(err.message, "error");
    } finally {
      btn.disabled = false;
      btn.textContent = "Upload / Replace QR Code";
    }
  });
}

function formatDate(isoString) {
  const d = new Date(isoString);
  return d.toLocaleString("en-IN", { day: "2-digit", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit" });
}
