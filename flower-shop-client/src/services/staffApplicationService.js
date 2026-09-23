import { authService } from './authService'
const managerRequest = (path, options) => import.meta.env.DEV && window.location.pathname === '/dev-manager' ? authService.devManagerRequest(path, options) : authService.authenticatedRequest(path, options)
export const staffApplicationService = {
  async detail(id) {
    const response = await fetch(`/api/public/shops/${encodeURIComponent(id)}`)
    if (!response.ok) throw new Error('Không tìm thấy shop đang hoạt động.')
    return response.json()
  },
  async search(q) {
    const response = await fetch(`/api/public/shops?q=${encodeURIComponent(q)}`)
    if (!response.ok) throw new Error('Không tải được danh sách shop.')
    return response.json()
  },
  apply: (id, body) => authService.authenticatedRequest(`/api/customer/shops/${encodeURIComponent(id)}/applications`, { method: 'POST', body }),
  mine: () => authService.authenticatedRequest('/api/account/staff-applications'),
  list: id => managerRequest(`/api/shop/mine/${encodeURIComponent(id)}/applications`),
  decide: (shop, id, approve) => managerRequest(`/api/shop/mine/${encodeURIComponent(shop)}/applications/${encodeURIComponent(id)}`, { method: 'PUT', body: { approve } }),
}
