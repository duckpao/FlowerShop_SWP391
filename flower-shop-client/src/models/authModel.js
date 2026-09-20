// Access tokens live only in memory, never localStorage/sessionStorage.
let accessToken = null
export const authModel = {
  getToken: () => accessToken,
  setToken: token => { accessToken = token },
  clear: () => { accessToken = null },
}
export const roleLabels = {
  ADMIN: 'Quản trị viên', CUSTOMER: 'Khách hàng', SHOP: 'Quản lý cửa hàng (Vendor)', SHOP_STAFF: 'Nhân viên cửa hàng',
}
