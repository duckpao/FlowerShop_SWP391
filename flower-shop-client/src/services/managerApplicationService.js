import { authService } from './authService'
const admin = (path, options) => import.meta.env.DEV && window.location.pathname === '/dev-admin'
  ? authService.devAdminRequest(path, options) : authService.authenticatedRequest(path, options)
export const managerApplicationService = {
  mine: () => authService.authenticatedRequest('/api/account/manager-applications'),
  submit: body => authService.authenticatedRequest('/api/customer/manager-applications', { method: 'POST', body }),
  list: (status, page) => admin(`/api/admin/shops/applications?status=${status}&page=${page}`),
  decide: (id, approve, note) => admin(`/api/admin/shops/applications/${encodeURIComponent(id)}`, { method: 'PUT', body: { approve, note } }),
}
