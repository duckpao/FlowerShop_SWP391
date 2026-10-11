import axiosClient from './axiosClient';

const paymentApi = {
  /**
   * PHASE 5: Khởi tạo phiên thanh toán SePay Gateway (tính tiền server-side bảo mật)
   * @param {Object} data - { orderId, paymentType, paymentMethod, userId }
   */
  createPayment: (data) => {
    return axiosClient.post('/payments/sepay/create', data);
  },

  cancelCheckout: (invoiceNumber) =>
    axiosClient.post(`/payments/sepay/cancel/${encodeURIComponent(invoiceNumber)}`),

  /**
   * PHASE 11: Kiểm tra trạng thái thanh toán theo mã hóa đơn invoiceNumber (cho Polling kết quả)
   */
  getPaymentStatusByInvoice: (invoiceNumber) => {
    return axiosClient.get(`/payments/status-by-invoice/${invoiceNumber}`);
  },

  /**
   * PHASE 11: Kiểm tra trạng thái theo paymentId
   */
  getPaymentStatusById: (paymentId) => {
    return axiosClient.get(`/payments/${paymentId}`);
  },

  /**
   * Kiểm tra trạng thái thanh toán theo orderId
   */
  checkPaymentStatus: (orderId) => {
    return axiosClient.get(`/payments/status-by-order/${orderId}`);
  },

  /**
   * Tạo thông tin thanh toán VietQR nhúng trực tiếp (modal QR)
   */
  createQrPayment: (orderId) => {
    return axiosClient.post(`/payment/create-qr/${orderId}`);
  },

  /**
   * PHASE 7: Tự động tạo form HTML ẩn và submit POST sang cổng SePay Checkout
   * @param {string} checkoutUrl - URL cổng SePay (Sandbox/Prod)
   * @param {Object} fields - Danh sách key-value đã được Spring Boot ký HMAC-SHA256
   */
  submitSePayCheckoutForm: (checkoutUrl, fields) => {
    const form = document.createElement('form');
    form.method = 'POST';
    form.action = checkoutUrl;
    form.style.display = 'none';

    Object.entries(fields).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = key;
        input.value = value;
        form.appendChild(input);
      }
    });

    document.body.appendChild(form);
    form.submit();
  },

  /**
   * Tạo phiên thanh toán rồi chuyển khách sang cổng SePay.
   * @param {string} orderId
   * @param {'FULL'|'DEPOSIT'|'FINAL'} paymentType - đơn thường dùng FULL; đơn đặt cọc dùng DEPOSIT rồi FINAL
   * @param {string} [paymentMethod] - CARD | BANK_TRANSFER | NAPAS_BANK_TRANSFER (bỏ trống để khách chọn trên cổng)
   */
  startSePayCheckout: async (orderId, paymentType = 'FULL', paymentMethod) => {
    const payment = await paymentApi.createPayment({ orderId, paymentType, paymentMethod });
    const checkoutUrl = payment?.checkoutUrl || payment?.data?.checkoutUrl;
    const fields = payment?.fields || payment?.data?.fields;
    if (!checkoutUrl || !fields) throw new Error('Không nhận được thông tin cổng thanh toán từ máy chủ.');
    if (payment?.invoiceNumber) sessionStorage.setItem('flower_pending_invoice', payment.invoiceNumber);
    paymentApi.submitSePayCheckoutForm(checkoutUrl, fields);
  },
};

export default paymentApi;
