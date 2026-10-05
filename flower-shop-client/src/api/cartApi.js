import axiosClient from './axiosClient';

const cartApi = {
  /**
   * Thêm sản phẩm vào giỏ hàng
   * @param {string} userId
   * @param {{ productId: string, quantity: number }} data
   */
  addToCart: (userId, data) => axiosClient.post(`/cart/${userId}/add`, data),

  /**
   * Lấy giỏ hàng của user
   * @param {string} userId
   */
  getCart: (userId) => axiosClient.get(`/cart/${userId}`),

  /**
   * Cập nhật số lượng 1 cart item
   * @param {string} userId
   * @param {string} itemId
   * @param {{ quantity: number }} data
   */
  updateCartItem: (userId, itemId, data) =>
    axiosClient.put(`/cart/${userId}/items/${itemId}`, data),

  /**
   * Xóa 1 sản phẩm khỏi giỏ hàng
   * @param {string} userId
   * @param {string} itemId
   */
  removeCartItem: (userId, itemId) =>
    axiosClient.delete(`/cart/${userId}/items/${itemId}`),
};

export default cartApi;

