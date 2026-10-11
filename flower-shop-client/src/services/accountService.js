import { authService } from './authService'

const request = (path, method = 'GET', body) => authService.authenticatedRequest(`/api/account/${path}`, { method, body })
async function updateProfile(path, method, body) {
  const profile = await request(path, method, body)
  window.dispatchEvent(new CustomEvent('flowershop-profile-updated', { detail: profile }))
  return profile
}
export const accountService = {
  profile: () => request('profile'),
  updateProfile: data => updateProfile('profile', 'PUT', data),
  uploadAvatar: file => {
    const body = new FormData()
    body.append('file', file)
    return updateProfile('profile/avatar', 'POST', body)
  },
  addresses: () => request('addresses'),
  saveAddress: (id, data) => request(id ? `addresses/${encodeURIComponent(id)}` : 'addresses', id ? 'PUT' : 'POST', data),
  deleteAddress: id => request(`addresses/${encodeURIComponent(id)}`, 'DELETE'),
  setDefault: id => request(`addresses/${encodeURIComponent(id)}/default`, 'PUT'),
}
