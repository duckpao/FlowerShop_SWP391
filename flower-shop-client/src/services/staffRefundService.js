import { authService } from './authService'

const base = shopId => `/api/staff/shops/${encodeURIComponent(shopId)}/refunds`

/**
 * Service cho Staff xử lý đơn hủy cần hoàn tiền.
 * Luồng gọi: StaffRefundsView -> staffRefundService -> StaffRefundController (ROLE_SHOP_STAFF) -> StaffRefundService.
 */
export const staffRefundService = {
    // GET /api/staff/shops/{shopId}/refunds: danh sách yêu cầu hoàn tiền của shop, mới nhất trước
    list: shopId => authService.authenticatedRequest(base(shopId)),

    // POST .../{id}/status: APPROVED (đã liên hệ) | PROCESSED (đã hoàn tiền) | REJECTED (từ chối)
    setStatus: (shopId, id, status) => authService.authenticatedRequest(`${base(shopId)}/${encodeURIComponent(id)}/status`, { method: 'POST', body: { status } }),
}
