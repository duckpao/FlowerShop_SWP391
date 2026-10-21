import axiosClient from './axiosClient';

const orderApi = {
  /** Phí vận chuyển GHN (bản GET, truyền shopId + addressId qua query). */
  shippingFee: (userId, shopId, addressId) =>
    axiosClient.get(`/orders/${userId}/shipping-fee`, { params: { shopId, addressId } }),

  /**
   * Phí vận chuyển GHN từ shop tới một địa chỉ đã lưu (dùng ở CartPage).
   * Gọi: POST /api/orders/{userId}/shipping-fee -> OrderController.shippingFee
   *   -> OrderService.quoteShippingFee -> GhnService.calculateFee -> GHN /v2/shipping-order/fee.
   * @param {string} userId
   * @param {{ shopId: string, deliveryAddressId: string }} data
   */
  getShippingFee: (userId, data) => axiosClient.post(`/orders/${userId}/shipping-fee`, data),

  /**
   * Lấy lịch sử đơn hàng của user
   * @param {string} userId
   */
  getOrderHistory: (userId) => axiosClient.get(`/orders/${userId}`),
};

export default orderApi;

