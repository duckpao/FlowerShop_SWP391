import { authService } from './authService'
import { API_BASE } from '../apiBase'
// URL gốc của quản lý sản phẩm Shop; mỗi API được ManagerProductController nhận theo shopId.
const base = id => `/api/shop/mine/${encodeURIComponent(id)}/products`
export const productService = {
  // list/categories: GET; save: POST tạo hoặc PUT sửa; hide: DELETE nhưng nghiệp vụ là ẩn mềm.
  list: (shop, page) => authService.authenticatedRequest(`${base(shop)}?page=${page}`),
  categories: shop => authService.authenticatedRequest(`${base(shop)}/categories`),
  save: (shop, id, body) => authService.authenticatedRequest(`${base(shop)}${id ? `/${encodeURIComponent(id)}` : ''}`, { method: id ? 'PUT' : 'POST', body }),
  hide: (shop, id) => authService.authenticatedRequest(`${base(shop)}/${encodeURIComponent(id)}`, { method: 'DELETE' }),
  // Nhánh API media đã có: FormData gửi file -> BE upload Cloudinary -> database lưu URL.
  // ProductsView hiện dùng nhập URL rồi save, chưa gọi nhóm hàm upload/list/delete media này.
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
// detail/catalog/catalogCategories ở file này không phải nhánh ProductCatalogView/ProductDetailView đang gọi; xem catalogService.
  async detail(id) {
    const response = await fetch(`${API_BASE}/api/public/products/${encodeURIComponent(id)}`)
    if (!response.ok) throw new Error('Không tải được thông tin sản phẩm.')
    return response.json()
  },
  // ProductsView manage=false -> GET /api/public/shops/{shopId}/products -> PublicProductController.list.
  async published(shop, page) {
    const response = await fetch(`${API_BASE}/api/public/shops/${encodeURIComponent(shop)}/products?page=${page}`)
    if (!response.ok) throw new Error('Không tải được sản phẩm của shop.')
    return response.json()
  },
  async catalog(page, categoryId, q) {
    const params = new URLSearchParams({ page })
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
