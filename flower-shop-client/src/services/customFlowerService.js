import { API_BASE } from '../apiBase'
import { authService } from './authService'

export const customFlowerService = {
  async suggestions({ budget, quantity = 1, shopId = '', q = '', page = 0 }) {
    const params = new URLSearchParams({ budget: String(budget), quantity: String(quantity), q, page: String(page) })
    if (shopId) params.set('shopId', shopId)
    const response = await fetch(`${API_BASE}/api/public/custom-orders/suggestions?${params}`, { cache: 'no-store' })
    const body = await response.json().catch(() => ({}))
    if (!response.ok) throw new Error(body.message || 'Không tải được mẫu hoa theo ngân sách.')
    return body
  },
  create: body => authService.authenticatedRequest('/api/customer/custom-orders', { method: 'POST', body }),
  mine: page => authService.authenticatedRequest(`/api/customer/custom-orders?page=${page}`),
  detail: id => authService.authenticatedRequest(`/api/customer/custom-orders/${encodeURIComponent(id)}`),
  shopRequests: (shopId, page, status = '') => authService.authenticatedRequest(`/api/shop/mine/${encodeURIComponent(shopId)}/custom-orders?${new URLSearchParams({ page: String(page), ...(status ? { status } : {}) })}`),
  decide: (shopId, id, body) => authService.authenticatedRequest(`/api/shop/mine/${encodeURIComponent(shopId)}/custom-orders/${encodeURIComponent(id)}`, { method: 'PUT', body }),
}
