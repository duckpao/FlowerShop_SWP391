import { authService } from './authService'

// Chủ shop dùng /api/shop/mine, staff dùng /api/staff/shops (cùng StaffRefundController)
const base = (shopId, role) => `${role === 'SHOP' ? '/api/shop/mine' : '/api/staff/shops'}/${encodeURIComponent(shopId)}/refunds`

/**
 * Service cho chủ shop và Staff xử lý đơn hủy cần hoàn tiền.
 * Luồng gọi: StaffRefundsView -> staffRefundService -> StaffRefundController (ROLE_SHOP hoặc ROLE_SHOP_STAFF) -> StaffRefundService.
 */
export const staffRefundService = {
    // GET .../{shopId}/refunds: danh sách yêu cầu hoàn tiền của shop, mới nhất trước
    list: (shopId, role) => authService.authenticatedRequest(base(shopId, role)),

    // POST .../{id}/status: APPROVED (đã liên hệ) | PROCESSED (đã hoàn tiền) | REJECTED (từ chối)
    setStatus: (shopId, id, status, role) => authService.authenticatedRequest(`${base(shopId, role)}/${encodeURIComponent(id)}/status`, { method: 'POST', body: { status } }),
}
