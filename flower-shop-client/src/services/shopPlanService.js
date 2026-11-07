import { authService } from './authService'
import { API_BASE } from '../apiBase'
const base = shop => `/api/shop/mine/${encodeURIComponent(shop)}/subscription`
export const shopPlanService = {
  adminPlans: () => authService.authenticatedRequest('/api/admin/shop-plans'),
  adminStatus: shop => authService.authenticatedRequest(`/api/admin/shops/${encodeURIComponent(shop)}/subscription`),
  adminGrant: (shop, body) => authService.authenticatedRequest(`/api/admin/shops/${encodeURIComponent(shop)}/subscription/activate`, { method: 'POST', body }),
  adminConfirm: (shop, invoice, note) => authService.authenticatedRequest(`/api/admin/shops/${encodeURIComponent(shop)}/subscription/payments/${encodeURIComponent(invoice)}/confirm`, { method: 'POST', body: { note } }),
  plans: () => authService.authenticatedRequest('/api/shop/plans'),
  status: shop => authService.authenticatedRequest(base(shop)),
  checkout: (shop, planId) => authService.authenticatedRequest(`${base(shop)}/checkout`, { method: 'POST', body: { planId } }),
  payment: (shop, invoice) => authService.authenticatedRequest(`${base(shop)}/payments/${encodeURIComponent(invoice)}`),
  feature: (shop, product, featured) => authService.authenticatedRequest(`${base(shop)}/featured/${encodeURIComponent(product)}`, { method: 'PUT', body: { featured } }),
  async featured() {
    const response = await fetch(`${API_BASE}/api/public/featured-products`, { cache: 'no-store' })
    if (!response.ok) throw new Error('Không tải được sản phẩm nổi bật.')
    return response.json()
  },
}
