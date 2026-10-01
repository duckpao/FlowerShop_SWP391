import { authService } from './authService'

const request = (path, method = 'GET', body) => authService.authenticatedRequest(`/api/account/${path}`, { method, body })
export const accountService = {
  profile: () => request('profile'),
  updateProfile: data => request('profile', 'PUT', data),
  addresses: () => request('addresses'),
  saveAddress: (id, data) => request(id ? `addresses/${encodeURIComponent(id)}` : 'addresses', id ? 'PUT' : 'POST', data),
  deleteAddress: id => request(`addresses/${encodeURIComponent(id)}`, 'DELETE'),
  setDefault: id => request(`addresses/${encodeURIComponent(id)}/default`, 'PUT'),
}
