import { authService } from './authService'

// Chủ shop dùng /api/shop/mine, staff dùng /api/staff/shops (cùng StaffRefundController)
const base = (shopId, role) => `${role === 'SHOP' ? '/api/shop/mine' : '/api/staff/shops'}/${encodeURIComponent(shopId)}/refunds`

/**
 * Service cho chủ shop, Staff và Admin xử lý yêu cầu hoàn tiền.
 * Luồng gọi: StaffRefundsView / AdminRefundsView -> staffRefundService -> StaffRefundController hoặc AdminRefundController -> StaffRefundService.
 */
export const staffRefundService = {
    // GET .../{shopId}/refunds: danh sách yêu cầu hoàn tiền của shop, mới nhất trước
    list: (shopId, role) => authService.authenticatedRequest(base(shopId, role)),

    // POST .../{id}/status: APPROVED (đã liên hệ) | PROCESSED (đã hoàn tiền) | REJECTED (từ chối, kèm note phản hồi khách)
    setStatus: (shopId, id, status, role, note) => authService.authenticatedRequest(`${base(shopId, role)}/${encodeURIComponent(id)}/status`, { method: 'POST', body: { status, note } }),

    // GET /api/admin/refunds?status=&page=: mọi yêu cầu của hệ thống
    adminList: (status, page = 0) => authService.authenticatedRequest(`/api/admin/refunds?page=${page}${status ? `&status=${encodeURIComponent(status)}` : ''}`),
    adminSetStatus: (id, status, note) => authService.authenticatedRequest(`/api/admin/refunds/${encodeURIComponent(id)}/status`, { method: 'POST', body: { status, note } }),
}
