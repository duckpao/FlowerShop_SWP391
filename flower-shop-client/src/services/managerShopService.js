import { authService } from './authService'
const call = (path, method = 'GET', body) => import.meta.env.DEV && window.location.pathname === '/dev-manager'
  ? authService.devManagerRequest(path, { method, body }) : authService.authenticatedRequest(path, { method, body })
const base = '/api/shop/mine'
export const managerShopService = {
  invitations: id => call(`${base}/${encodeURIComponent(id)}/invitations`),
  invite: (id, email) => call(`${base}/${encodeURIComponent(id)}/invitations`, 'POST', { email }),
  cancelInvite: (id, invitationId) => call(`${base}/${encodeURIComponent(id)}/invitations/${encodeURIComponent(invitationId)}`, 'DELETE'),
  list: role => call(role === 'SHOP' ? base : '/api/staff/shops'),
  save: (id, body) => call(`${base}/${encodeURIComponent(id)}`, 'PUT', body),
  uploadLogo: (id, file) => {
    const body = new FormData()
    body.append('file', file)
    return call(`${base}/${encodeURIComponent(id)}/logo`, 'POST', body)
  },
  removeLogo: id => call(`${base}/${encodeURIComponent(id)}/logo`, 'DELETE'),
  staff: id => call(`${base}/${encodeURIComponent(id)}/staff`),
  address: id => call(`${base}/${encodeURIComponent(id)}/address`),
  saveAddress: (id, body) => call(`${base}/${encodeURIComponent(id)}/address`, 'PUT', body),
  active: (id, userId, active) => call(`${base}/${encodeURIComponent(id)}/staff/${encodeURIComponent(userId)}`, 'PUT', { active }),
}
