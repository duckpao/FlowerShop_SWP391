import { API_BASE } from '../apiBase'

// Nhiều request ghi chạy song song (vd mỗi shop một ô chọn mã giảm giá) dùng chung một lần xin token:
// xin riêng từng lần khi chưa có phiên sẽ tạo nhiều phiên, token của lần trước bị lệch và server trả 403.
let inflight = null

export function fetchCsrf() {
  if (!inflight) {
    inflight = fetch(`${API_BASE}/api/auth/csrf`, { credentials: 'include', cache: 'no-store' })
      .then(response => {
        if (!response.ok) throw new Error('Không thể tạo phiên bảo vệ yêu cầu.')
        return response.json()
      })
      .finally(() => { inflight = null })
  }
  return inflight
}
