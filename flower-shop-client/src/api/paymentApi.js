import axiosClient from './axiosClient';

const paymentApi = {
  /**
   * PHASE 5: Khởi tạo phiên thanh toán SePay Gateway (tính tiền server-side bảo mật)
   * @param {Object} data - { orderId, paymentType, paymentMethod, userId }
   */
  createPayment: (data) => {
    return axiosClient.post('/payments/sepay/create', data);
  },

  /** Khách bấm hủy/đóng cổng SePay -> backend hủy giao dịch và hủy luôn đơn tương ứng. */
  cancelCheckout: (invoiceNumber) =>
    axiosClient.post(`/payments/sepay/cancel/${encodeURIComponent(invoiceNumber)}`),

  /**
   * PHASE 11: Kiểm tra trạng thái thanh toán theo mã hóa đơn invoiceNumber (cho Polling kết quả).
   * Các trang success/pending gọi lặp lại hàm này cho tới khi webhook SePay cập nhật trạng thái PAID.
   */
  getPaymentStatusByInvoice: (invoiceNumber) => {
    return axiosClient.get(`/payments/status-by-invoice/${invoiceNumber}`);
  },

  /**
   * Kiểm tra trạng thái thanh toán theo orderId
   */
  checkPaymentStatus: (orderId) => {
    return axiosClient.get(`/payments/status-by-order/${orderId}`);
  },

  /**
   * PHASE 7: Tự động tạo form HTML ẩn và submit POST sang cổng SePay Checkout
   * @param {string} checkoutUrl - URL cổng SePay (Sandbox/Prod)
   * @param {Object} fields - Danh sách key-value đã được Spring Boot ký HMAC-SHA256
   */
  submitSePayCheckoutForm: (checkoutUrl, fields) => {
    // SePay yêu cầu POST form (không nhận JSON) nên phải tạo form ẩn rồi submit để trình duyệt điều hướng sang cổng
    const form = document.createElement('form');
    form.method = 'POST';
    form.action = checkoutUrl;
    form.style.display = 'none';

    Object.entries(fields).forEach(([key, value]) => {
      // Bỏ field rỗng: chữ ký HMAC do backend tính trên đúng tập field này, không được thêm/bớt
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
    paymentApi.redirectToSePay(payment);
  },

  /**
   * Thanh toán gộp nhiều đơn (mỗi shop một đơn) trong một giao dịch SePay.
   * @param {string[]} orderIds
   */
  startSePayGroupCheckout: async (orderIds, paymentMethod) => {
    const payment = await paymentApi.createGroupPayment({ orderIds, paymentMethod });
    paymentApi.redirectToSePay(payment);
  },

  /** Tạo 1 giao dịch SePay chung cho nhiều đơn (backend cộng tổng tiền các đơn). */
  createGroupPayment: (data) => axiosClient.post('/payments/sepay/create-group', data),

  /** Nhận response tạo giao dịch từ backend và chuyển trình duyệt sang cổng SePay. */
  redirectToSePay: (payment) => {
    const checkoutUrl = payment?.checkoutUrl || payment?.data?.checkoutUrl;
    const fields = payment?.fields || payment?.data?.fields;
    if (!checkoutUrl || !fields) throw new Error('Không nhận được thông tin cổng thanh toán từ máy chủ.');
    // Nhớ mã hóa đơn để các trang success/pending/cancel poll đúng giao dịch sau khi SePay redirect về
    if (payment?.invoiceNumber) sessionStorage.setItem('flower_pending_invoice', payment.invoiceNumber);
    paymentApi.submitSePayCheckoutForm(checkoutUrl, fields);
  },
};

export default paymentApi;
