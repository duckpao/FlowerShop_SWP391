import AdminManagerApplicationsView from './AdminManagerApplicationsView'
export default function AdminApprovalsView({ user, logout, busy, error }) {
  return <main className="account-page">
    <header className="account-header"><div><h1>Duyệt yêu cầu</h1><p>Admin · {user.email}</p></div>
      <a href="/">Trang chủ</a><a href="/account">Quản lý tài khoản và shop</a><button disabled={busy} onClick={logout}>Đăng xuất</button>
    </header>
    {error && <p className="message error" role="alert">{error}</p>}
    <p>Xem đơn Customer đăng ký mở shop. Duyệt đơn sẽ tạo shop và cấp quyền Manager; từ chối giữ nguyên quyền Customer.</p>
    <AdminManagerApplicationsView />
    <p>Đơn xin làm nhân viên do Manager của từng shop xử lý, không nằm trong danh sách duyệt của Admin.</p>
  </main>
}
