import { authService } from './authService'
const base = '/api/admin/shops'
const request = (...args) => (import.meta.env.DEV && window.location.pathname === '/dev-admin'
  ? authService.devAdminRequest(...args) : authService.authenticatedRequest(...args))
export const adminShopService = {
  list: params => request(`${base}?${new URLSearchParams(params)}`),
  detail: id => request(`${base}/${encodeURIComponent(id)}`),
  transition: (id, action) => request(`${base}/${encodeURIComponent(id)}/${action}`, { method: 'PUT' }),
}
