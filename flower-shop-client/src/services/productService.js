import { authService } from './authService'
import { API_BASE } from '../apiBase'
const base = id => `/api/shop/mine/${encodeURIComponent(id)}/products`
export const productService = {
  list: (shop, page) => authService.authenticatedRequest(`${base(shop)}?page=${page}`),
  categories: shop => authService.authenticatedRequest(`${base(shop)}/categories`),
  save: (shop, id, body) => authService.authenticatedRequest(`${base(shop)}${id ? `/${encodeURIComponent(id)}` : ''}`, { method: id ? 'PUT' : 'POST', body }),
  hide: (shop, id) => authService.authenticatedRequest(`${base(shop)}/${encodeURIComponent(id)}`, { method: 'DELETE' }),
  images: (shop, productId) => authService.authenticatedRequest(`${base(shop)}/${encodeURIComponent(productId)}/images`),
  uploadImage: (shop, productId, file) => {
    const body = new FormData()
    body.append('file', file)
    return authService.authenticatedRequest(`${base(shop)}/${encodeURIComponent(productId)}/images`, { method: 'POST', body })
  },
  deleteImage: (shop, productId, imageId) => authService.authenticatedRequest(`${base(shop)}/${encodeURIComponent(productId)}/images/${encodeURIComponent(imageId)}`, { method: 'DELETE' }),
  videos: (shop, productId) => authService.authenticatedRequest(`${base(shop)}/${encodeURIComponent(productId)}/videos`),
  uploadVideo: (shop, productId, file, title, description) => {
    const body = new FormData()
    body.append('file', file)
    if (title) body.append('title', title)
    if (description) body.append('description', description)
    return authService.authenticatedRequest(`${base(shop)}/${encodeURIComponent(productId)}/videos`, { method: 'POST', body })
  },
  deleteVideo: (shop, productId, videoId) => authService.authenticatedRequest(`${base(shop)}/${encodeURIComponent(productId)}/videos/${encodeURIComponent(videoId)}`, { method: 'DELETE' }),
  async detail(id) {
    const response = await fetch(`${API_BASE}/api/public/products/${encodeURIComponent(id)}`)
    if (!response.ok) throw new Error('Không tải được thông tin sản phẩm.')
    return response.json()
  },
  async published(shop, page) {
    const response = await fetch(`${API_BASE}/api/public/shops/${encodeURIComponent(shop)}/products?page=${page}`)
    if (!response.ok) throw new Error('Không tải được sản phẩm của shop.')
    return response.json()
  },
  async catalog(page, categoryId, q) {
    const params = new URLSearchParams()
    if (page !== undefined && page !== null && page !== '') params.set('page', page)
    if (categoryId) params.set('categoryId', categoryId)
    if (q) params.set('q', q)
    const response = await fetch(`${API_BASE}/api/public/products?${params}`)
    if (!response.ok) throw new Error('Không tải được danh sách sản phẩm.')
    return response.json()
  },
  async catalogCategories() {
    const response = await fetch(`${API_BASE}/api/public/categories`)
    if (!response.ok) throw new Error('Không tải được danh mục.')
    return response.json()
  },
}
