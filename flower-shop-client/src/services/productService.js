import { authService } from './authService'
const base = id => `/api/shop/mine/${encodeURIComponent(id)}/products`
export const productService = {
  list: (shop, page) => authService.authenticatedRequest(`${base(shop)}?page=${page}`),
  categories: shop => authService.authenticatedRequest(`${base(shop)}/categories`),
  save: (shop, id, body) => authService.authenticatedRequest(`${base(shop)}${id ? `/${encodeURIComponent(id)}` : ''}`, { method: id ? 'PUT' : 'POST', body }),
  hide: (shop, id) => authService.authenticatedRequest(`${base(shop)}/${encodeURIComponent(id)}`, { method: 'DELETE' }),
  async published(shop, page) {
    const response = await fetch(`/api/public/shops/${encodeURIComponent(shop)}/products?page=${page}`)
    if (!response.ok) throw new Error('Không tải được sản phẩm của shop.')
    return response.json()
  },
}
