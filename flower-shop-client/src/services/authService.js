import toast from 'react-hot-toast'
import { authModel } from '../models/authModel'
import { API_BASE } from '../apiBase'

// Cầu nối HTTP dùng chung cho quản lý sản phẩm, favorite và ghi review:
// lấy CSRF cho lệnh ghi -> gắn Bearer nếu cần -> JSON.stringify (hoặc FormData) -> fetch -> đọc JSON/lỗi.
async function request(path, { method = 'GET', body, bearer = false } = {}) {
  const isFormData = body instanceof FormData
  const headers = {}
  if (method !== 'GET') {
    const csrfResponse = await fetch(`${API_BASE}/api/auth/csrf`, { credentials: 'include', cache: 'no-store' })
    if (!csrfResponse.ok) throw new Error('Không thể tạo phiên bảo vệ yêu cầu.')
    const csrf = await csrfResponse.json()
    headers[csrf.headerName] = csrf.token
    if (!isFormData) headers['Content-Type'] = 'application/json'
  }
  if (bearer && authModel.getToken()) headers.Authorization = `Bearer ${authModel.getToken()}`
  let response;
  try {
    response = await fetch(`${API_BASE}${path.startsWith('/api/') ? path : `/api/auth/${path}`}`, {
      method, headers, credentials: 'include', cache: 'no-store',
      ...(body !== undefined ? { body: isFormData ? body : JSON.stringify(body) } : {}),
    })
  } catch (err) {
    if (path !== 'refresh') toast.error('Mất kết nối mạng. Vui lòng kiểm tra lại đường truyền.');
    throw err;
  }
  const data = await response.json().catch(() => ({}))
  if (!response.ok) {
    const error = new Error(data.errors ? Object.values(data.errors).join(' ') : data.message || 'Yêu cầu thất bại. Vui lòng thử lại.')
    error.status = response.status
    if (path !== 'refresh' && error.status !== 401 && error.status !== 403) {
      toast.error(error.message)
    }
    throw error
  }
  return data
}

let refreshPromise = null
async function refresh() {
  // Serialize refresh across tabs when Web Locks is available to avoid token replay.
  if (!refreshPromise) {
    const run = async () => {
      const data = await request('refresh', { method: 'POST' })
      if (!data.accessToken) {
        authModel.clear()
        return null
      }
      authModel.setToken(data.accessToken)
      return data.user
    }
    refreshPromise = (navigator.locks ? navigator.locks.request('flowershop-refresh', run) : run())
      .catch(error => { authModel.clear(); throw error })
      .finally(() => { refreshPromise = null })
  }
  return refreshPromise
}

export const authService = {
  devManagerRequest(path, options = {}) {
    if (!import.meta.env.DEV || !/^\/api\/shop\/mine([/?]|$)/.test(path)) throw new Error('Chỉ hỗ trợ Manager thử nghiệm local.')
    return request(path, options)
  },
  devAdminRequest(path, options = {}) {
    if (!import.meta.env.DEV || !/^\/api\/admin\/(customers|shops)([/?]|$)/.test(path)) throw new Error('Chế độ thử nghiệm chỉ có trên local.')
    return request(path, options)
  },
  // Các service nghiệp vụ gọi đây để có token; nếu thiếu thì refresh, gặp 401 thì refresh và thử lại một lần.
  // Token chỉ xác định người gọi; quyền CUSTOMER/SHOP/ADMIN còn được BE kiểm tra trước controller.
  async authenticatedRequest(path, options = {}) {
    if (!authModel.getToken()) {
      const user = await refresh()
      if (!user) {
        const error = new Error('Vui lòng đăng nhập để tiếp tục.')
        error.status = 401
        throw error
      }
    }
    try { return await request(path, { ...options, bearer: true }) }
    catch (error) {
      if (error.status !== 401) throw error
      const user = await refresh()
      if (!user) throw error
      return request(path, { ...options, bearer: true })
    }
  },
  post: (path, body) => request(path, { method: 'POST', body }),
  async login(email, password) {
    const data = await request('login', { method: 'POST', body: { email, password } })
    authModel.setToken(data.accessToken)
    return data.user
  },
  async me() {
    if (!authModel.getToken()) return refresh()
    try { return await request('me', { bearer: true }) }
    catch (error) { if (error.status === 401) return refresh(); throw error }
  },
  async logout() {
    await request('logout', { method: 'POST' })
    authModel.clear()
  },
  clear: () => authModel.clear(),
}
