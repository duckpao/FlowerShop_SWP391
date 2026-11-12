import { authService } from './authService'

const request = (path, options) => authService.authenticatedRequest(path, options)
const query = params => new URLSearchParams(Object.entries(params).filter(([, v]) => v !== '' && v != null))

/** SRS 4.4: Admin xử lý hoàn tiền, xem đơn hàng và kiểm duyệt đánh giá (AdminOversightController). */
export const adminOversightService = {
    refunds: (status, page) => request(`/api/admin/refunds?${query({ status, page })}`),
    setRefundStatus: (id, status, note) => request(`/api/admin/refunds/${encodeURIComponent(id)}/status`, { method: 'POST', body: { status, note } }),
    orders: (status, page) => request(`/api/admin/orders?${query({ status, page })}`),
    order: id => request(`/api/admin/orders/${encodeURIComponent(id)}`),
    reviews: page => request(`/api/admin/reviews?${query({ page })}`),
    deleteReview: id => request(`/api/admin/reviews/${encodeURIComponent(id)}`, { method: 'DELETE' }),
}
