import { authService } from './authService'

const qs = params => new URLSearchParams(Object.entries(params).filter(([, v]) => v !== '' && v != null)).toString()

/** Admin kiểm duyệt đánh giá. Luồng: AdminReviewsView -> adminReviewService -> AdminReviewController. */
export const adminReviewService = {
    list: params => authService.authenticatedRequest(`/api/admin/reviews?${qs(params)}`),
    remove: (id, reason) => authService.authenticatedRequest(`/api/admin/reviews/${encodeURIComponent(id)}`, { method: 'DELETE', body: { reason } }),
}
