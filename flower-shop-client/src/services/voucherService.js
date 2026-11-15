import { authService } from './authService'
import axiosClient from './axiosClient'

const shopBase = shop => `/api/shop/mine/${encodeURIComponent(shop)}/vouchers`
const adminBase = '/api/admin/vouchers'

/** scope: { shopId } cho voucher của shop, rỗng cho voucher sàn (admin). */
const base = scope => (scope?.shopId ? shopBase(scope.shopId) : adminBase)

export const voucherService = {
  list: scope => authService.authenticatedRequest(base(scope)),
  save: (scope, id, body) => authService.authenticatedRequest(`${base(scope)}${id ? `/${encodeURIComponent(id)}` : ''}`, { method: id ? 'PUT' : 'POST', body }),
  remove: (scope, id) => authService.authenticatedRequest(`${base(scope)}/${encodeURIComponent(id)}`, { method: 'DELETE' }),
  /** Xem trước mã dùng được cho một shop khi thanh toán (chỉ đọc). */
  preview: (userId, shopId, cartItemIds, couponCode) =>
    axiosClient.post(`/orders/${encodeURIComponent(userId)}/vouchers/preview`, { shopId, cartItemIds, couponCode: couponCode || null }),
}
