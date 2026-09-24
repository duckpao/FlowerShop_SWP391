import AdminShopsView from "./AdminShopsView";
import AdminManagerApplicationsView from "./AdminManagerApplicationsView";
import AdminCustomersView from "./AdminCustomersView";
import AdminProfileView from "./AdminProfileView";
import "../styles/admin.css";

const options = [
  {
    href: "/admin/users",
    title: "Quản lý người dùng",
    icon: "♙",
    description:
      "Quản lý tài khoản khách hàng: tìm kiếm, xem chi tiết, khóa hoặc mở khóa.",
  },
  {
    href: "/admin/shops",
    title: "Quản lý shop",
    icon: "❀",
    description:
      "Xem cửa hàng, tìm kiếm, duyệt hoạt động và khóa hoặc mở khóa shop.",
  },
  {
    href: "/admin/approvals",
    title: "Quản lý đơn",
    icon: "☷",
    description:
      "Xem đơn đăng ký mở shop, kiểm tra thông tin và duyệt hoặc từ chối yêu cầu.",
  },
];

export default function AdminDashboardView({
  user,
  logout,
  busy,
  error,
  path,
}) {
  const current = options.find((item) => item.href === path);
  const profile = path === "/admin/profile";
  return (
    <div className="admin-shell">
      <aside className="admin-sidebar">
        <a className="admin-brand" href="/admin">
          ✿ FlowerShop
        </a>
        <p>TRANG QUẢN TRỊ</p>
        <nav aria-label="Điều hướng Admin">
          <a
            href="/admin"
            aria-current={path === "/admin" ? "page" : undefined}
          >
            Tổng quan
          </a>
          {options.map((item) => (
            <a
              key={item.href}
              href={item.href}
              aria-current={path === item.href ? "page" : undefined}
            >
              <span aria-hidden="true">{item.icon}</span> {item.title}
            </a>
          ))}
        </nav>
        <a className="admin-back" href="/">
          ← Về cửa hàng
        </a>
      </aside>
      <main className="account-page admin-content">
        <header className="account-header">
          <div>
            <p className="admin-eyebrow">FLOWERSHOP / ADMIN</p>
            <h1>
              {profile
                ? "Tài khoản Admin"
                : current?.title || "Dashboard Admin"}
            </h1>
          </div>
          <div className="admin-user">
            <a
              className="admin-profile-link"
              href="/admin/profile"
              aria-current={profile ? "page" : undefined}
            >
              <strong>{user.fullName || "Admin"}</strong>
              <span>{user.email}</span>
              <small>Thông tin tài khoản →</small>
            </a>
            <button disabled={busy} onClick={logout}>
              Đăng xuất
            </button>
          </div>
        </header>
        {error && (
          <p role="alert" className="message error">
            {error}
          </p>
        )}
        {!current && !profile && (
          <>
            <p>
              Chọn chức năng để quản lý người dùng, cửa hàng và xử lý các yêu
              cầu đang chờ duyệt.
            </p>
            <div className="admin-options">
              {options.map((item) => (
                <a className="admin-option" key={item.href} href={item.href}>
                  <span className="admin-option-icon" aria-hidden="true">
                    {item.icon}
                  </span>
                  <h2>{item.title}</h2>
                  <p>{item.description}</p>
                  <strong>Mở quản lý →</strong>
                </a>
              ))}
            </div>
          </>
        )}
        {path === "/admin/shops" && <AdminShopsView />}
        {path === "/admin/users" && <AdminCustomersView />}
        {profile && <AdminProfileView user={user} />}
        {path === "/admin/approvals" && (
          <>
            <p>Danh sách đơn Customer đăng ký trở thành Manager và mở shop.</p>
            <AdminManagerApplicationsView />
          </>
        )}
      </main>
    </div>
  );
}
