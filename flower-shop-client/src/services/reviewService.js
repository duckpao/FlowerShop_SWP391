import { authService } from './authService'
import { API_BASE } from '../apiBase'

const mine = id => `/api/customer/products/${encodeURIComponent(id)}/reviews`

export const reviewService = {
  async list(id, page) {
    const response = await fetch(`${API_BASE}/api/public/products/${encodeURIComponent(id)}/reviews?page=${page}`, { cache: 'no-store' })
    if (!response.ok) throw new Error('Không tải được đánh giá.')
    return response.json()
  },

  /** Đánh giá của chính mình, hoặc null khi chưa đánh giá. */
  async own(id) {
    try { return await authService.authenticatedRequest(mine(id)) }
    catch (error) { if (error.status === 404) return null; throw error }
  },

  write: (id, body) => authService.authenticatedRequest(mine(id), { method: 'POST', body }),
  update: (id, body) => authService.authenticatedRequest(mine(id), { method: 'PUT', body }),
  remove: id => authService.authenticatedRequest(mine(id), { method: 'DELETE' }),
  reply: (shopId, reviewId, reply) => authService.authenticatedRequest(
    `/api/shop/mine/${encodeURIComponent(shopId)}/reviews/${encodeURIComponent(reviewId)}/reply`,
    { method: 'PUT', body: { reply } }),
}
