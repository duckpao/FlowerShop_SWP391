import { authService } from './authService'
const base = '/api/admin/customers'
const request = (...args) => (import.meta.env.DEV && window.location.pathname === '/dev-admin'
  ? authService.devAdminRequest(...args) : authService.authenticatedRequest(...args))
export const adminCustomerService = {
  list: params => request(`${base}?${new URLSearchParams(params)}`),
  detail: id => request(`${base}/${encodeURIComponent(id)}`),
  block: (id, blocked) => request(`${base}/${encodeURIComponent(id)}/blocked`, { method: 'PUT', body: { blocked } }),
}
