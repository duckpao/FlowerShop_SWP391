const KEY = 'access_token'
export const authModel = {
  getToken: () => localStorage.getItem(KEY),
  setToken: token => localStorage.setItem(KEY, token),
  clear: () => localStorage.removeItem(KEY),
}
export const roleLabels = {
  ADMIN: 'Quản trị viên', CUSTOMER: 'Khách hàng', SHOP: 'Quản lý cửa hàng (Vendor)', SHOP_STAFF: 'Nhân viên cửa hàng',
}
