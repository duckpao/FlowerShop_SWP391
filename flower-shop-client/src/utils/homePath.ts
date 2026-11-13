/**
 * Màn hình chính của từng vai trò, dùng cho logo: Admin về trang quản trị, chủ shop và nhân viên về trang quản lý shop,
 * khách hàng và khách vãng lai về trang chủ cửa hàng. Khớp với nơi hệ thống chuyển người dùng sau khi đăng nhập.
 */
export function homePathFor(role?: string | null): string {
  if (role === "ADMIN") return "/admin";
  if (role === "SHOP" || role === "SHOP_STAFF") return "/shop-admin";
  return "/";
}
