// Cổng HTTP công khai của Product List/Details/Category: fetch -> đọc JSON -> ném lỗi cho hook.
// URL ở đây là đường dẫn tương đối, đi tới origin đang mở FE. Vite config hiện không có proxy /api;
// khi chạy riêng cổng 5173, các request này không tự đi tới BE 8080 như authService dùng API_BASE.
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

// browse mã hóa bộ lọc vào query string; detail mã hóa id trên URL; categories/shops nạp các ô chọn.
export const catalogService = {
  browse: params => get(`/api/public/products?${new URLSearchParams(params)}`),
  detail: id => get(`/api/public/products/${encodeURIComponent(id)}`),
  categories: () => get('/api/public/categories'),
  shops: () => get('/api/public/shops'),
}
