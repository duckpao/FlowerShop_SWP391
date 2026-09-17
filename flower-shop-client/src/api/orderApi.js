import axiosClient from './axiosClient';

const orderApi = {
  /**
   * Tạo đơn hàng từ các cart items đã chọn
   * @param {string} userId
   * @param {{
   *   shopId: string,
   *   cartItemIds: string[],
   *   deliveryAddressId: string,
   *   couponId?: string
   * }} data
   */
  makeOrder: (userId, data) => axiosClient.post(`/orders/${userId}`, data),

  /**
   * Lấy lịch sử đơn hàng của user
   * @param {string} userId
   */
  getOrderHistory: (userId) => axiosClient.get(`/orders/${userId}`),
};

export default orderApi;

