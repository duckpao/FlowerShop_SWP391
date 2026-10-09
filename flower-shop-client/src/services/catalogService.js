import { API_BASE } from '../apiBase'

async function get(path) {
  const response = await fetch(`${API_BASE}${path}`, { cache: 'no-store' })
  const data = await response.json().catch(() => null)
  if (!response.ok) {
    const error = new Error(data?.message || 'Không tải được dữ liệu sản phẩm.')
    error.status = response.status
    throw error
  }
  if (data === null) throw new Error('API sản phẩm trả về dữ liệu không hợp lệ.')
  return data
}

export const catalogService = {
  browse: params => {
    const searchParams = new URLSearchParams()
    Object.entries(params || {}).forEach(([key, val]) => {
      if (val !== undefined && val !== null && val !== '') {
        searchParams.set(key, val)
      }
    })
    return get(`/api/public/products?${searchParams.toString()}`)
  },
  detail: id => get(`/api/public/products/${encodeURIComponent(id)}`),
  shops: () => get('/api/public/shops'),
  categories: async () => {
    const data = await get('/api/public/categories')
    if (!Array.isArray(data)) throw new Error('API danh mục phải trả về một danh sách.')
    return data
  },
}
