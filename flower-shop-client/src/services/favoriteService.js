import { authService } from './authService'

const base = '/api/customer/favorites'
// Giới hạn số trang khi dựng tập id đã lưu, để không gọi API không giới hạn.
const MAX_ID_PAGES = 10

export const favoriteService = {
// mine -> GET trang đã lưu; add -> PUT /{id}; remove -> DELETE /{id}. Cả ba dùng Bearer qua authService.
  mine: page => authService.authenticatedRequest(`${base}?page=${page}`),
  add: id => authService.authenticatedRequest(`${base}/${encodeURIComponent(id)}`, { method: 'PUT' }),
  remove: id => authService.authenticatedRequest(`${base}/${encodeURIComponent(id)}`, { method: 'DELETE' }),

  /** Tập id sản phẩm đã lưu, dùng để tô nút tim trên trang danh sách và chi tiết. */
  async ids() {
    const first = await favoriteService.mine(0)
    const collected = first.content.map(p => p.id)
    // Chỉ lấy tối đa 10 trang x 12 = 120 mục; nút tim ngoài tập này có thể chưa phản ánh đúng trạng thái đã lưu.
    const pages = Math.min(first.totalPages, MAX_ID_PAGES)
    for (let page = 1; page < pages; page += 1) {
      const next = await favoriteService.mine(page)
      collected.push(...next.content.map(p => p.id))
    }
    return new Set(collected)
  },
}
