import axiosClient from './axiosClient';

const orderApi = {
  /** Phí vận chuyển GHN (bản GET, truyền shopId + addressId qua query). */
  shippingFee: (userId, shopId, addressId) =>
    axiosClient.get(`/orders/${userId}/shipping-fee`, { params: { shopId, addressId } }),

  /**
   * Đặt nhiều đơn một lúc (mỗi shop một đơn), dùng ở CheckoutPage.
   * Gọi: POST /api/orders/{userId}/batch -> OrderController.makeOrders -> OrderService.makeOrders.
   * @param {string} userId
   * @param {{
   *   deliveryAddressId: string,
   *   recipientName: string,
   *   phone: string,
   *   paymentMethod: 'COD' | 'ONLINE',
   *   shops: { shopId: string, cartItemIds: string[], couponId?: string }[]
   * }} data
   */
  makeOrders: (userId, data) => axiosClient.post(`/orders/${userId}/batch`, data),

  /**
   * Lấy lịch sử đơn hàng của user
   * @param {string} userId
   */
  getOrderHistory: (userId) => axiosClient.get(`/orders/${userId}`),
};

export default orderApi;

