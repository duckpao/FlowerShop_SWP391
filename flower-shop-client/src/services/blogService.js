import { authService } from './authService'
import { API_BASE } from '../apiBase'

const managedBase = shopId => `/api/shop/mine/${encodeURIComponent(shopId)}/blogs`

async function publicRequest(path) {
  const response = await fetch(`${API_BASE}${path}`, { cache: 'no-store' })
  const data = await response.json().catch(() => ({}))
  if (!response.ok) throw new Error(data.message || 'Không tải được bài viết.')
  return data
}

export const blogService = {
  managed: (shopId, page = 0) => authService.authenticatedRequest(`${managedBase(shopId)}?page=${page}`),
  save: (shopId, id, blog, images = []) => {
    const body = new FormData()
    body.append('blog', new Blob([JSON.stringify(blog)], { type: 'application/json' }))
    images.forEach(file => body.append('images', file))
    return authService.authenticatedRequest(
      `${managedBase(shopId)}${id ? `/${encodeURIComponent(id)}` : ''}`,
      { method: id ? 'PUT' : 'POST', body },
    )
  },
  hide: (shopId, id) => authService.authenticatedRequest(
    `${managedBase(shopId)}/${encodeURIComponent(id)}`,
    { method: 'DELETE' },
  ),
  publicList: ({ shopId = '', q = '', page = 0 } = {}) => {
    const params = new URLSearchParams({ page })
    if (shopId) params.set('shopId', shopId)
    if (q) params.set('q', q)
    return publicRequest(`/api/public/blogs?${params}`)
  },
  publicDetail: id => publicRequest(`/api/public/blogs/${encodeURIComponent(id)}`),
}
