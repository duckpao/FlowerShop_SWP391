import axiosClient from './axiosClient';

const orderService = {
  /** Phí vận chuyển GHN (bản GET, truyền shopId + addressId qua query). */
  shippingFee: (userId, shopId, addressId) =>
    axiosClient.get(`/orders/${userId}/shipping-fee`, { params: { shopId, addressId } }),

  /** Tạo nhiều đơn cùng lúc (giỏ có sản phẩm của nhiều shop -> mỗi shop 1 đơn). */
  makeOrders: (userId, data) => axiosClient.post(`/orders/${userId}/batch`, data),

  /** Tạo đơn đặt cọc từ yêu cầu hoa tùy chỉnh đã được shop báo giá ({ deliveryAddressId }). */
  makeCustomOrder: (userId, requestId, data) =>
    axiosClient.post(`/orders/${userId}/custom/${encodeURIComponent(requestId)}`, data),

  /**
   * Lấy lịch sử đơn hàng của user
   * @param {string} userId
   */
  getOrderHistory: (userId) => axiosClient.get(`/orders/${userId}`),
};

export default orderService;

