async function get(path) {
  const response = await fetch(path, { cache: 'no-store' })
  const data = await response.json().catch(() => ({}))
  if (!response.ok) {
    const error = new Error(data.message || 'Không tải được dữ liệu sản phẩm.')
    error.status = response.status
    throw error
  }
  return data
}

export const catalogService = {
  browse: params => get(`/api/public/products?${new URLSearchParams(params)}`),
  detail: id => get(`/api/public/products/${encodeURIComponent(id)}`),
  categories: () => get('/api/public/categories'),
}
