import ManagerShopsView from './ManagerShopsView'
import AdminProfileView from './AdminProfileView'
import '../styles/admin.css'

const options = [
  { href: '/shop-admin/staff', title: 'Quản lý tài khoản Staff', section: 'staff', icon: '♙', description: 'Xem nhân viên, bật/ngừng quyền làm việc tại shop và xử lý đơn ứng tuyển.' },
  { href: '/shop-admin/products', title: 'Quản lý mặt hàng', section: 'products', icon: '❀', description: 'Đăng, cập nhật và ẩn các sản phẩm thuộc cửa hàng của bạn.' },
  { href: '/shop-admin/shop', title: 'Hồ sơ shop', section: 'shop', icon: '⌂', description: 'Cập nhật thông tin và địa chỉ cửa hàng bạn quản lý.' },
]
export default function ManagerDashboardView({ user, logout, busy, error, path }) {
  const current = options.find(item => item.href === path)
  const profile = path === '/shop-admin/profile'
  return <div className="admin-shell">
    <aside className="admin-sidebar"><a className="admin-brand" href="/shop-admin">✿ FlowerShop</a><p>SHOP MANAGER</p>
      <nav aria-label="Điều hướng Shop Manager"><a href="/shop-admin" aria-current={path === '/shop-admin' ? 'page' : undefined}>Tổng quan</a>
        {options.map(item => <a key={item.href} href={item.href} aria-current={current === item ? 'page' : undefined}>{item.icon} {item.title}</a>)}
      </nav><a className="admin-back" href="/">← Về cửa hàng</a>
    </aside>
    <main className="account-page admin-content"><header className="account-header"><div><p className="admin-eyebrow">FLOWERSHOP / SHOP MANAGER</p><h1>{profile ? 'Hồ sơ cá nhân' : current?.title || 'Dashboard Shop Manager'}</h1></div>
      <div className="admin-user"><a className="manager-avatar-link" href="/shop-admin/profile" aria-label="Xem thông tin cá nhân Shop Manager" aria-current={profile ? 'page' : undefined}>
        <span className="manager-avatar" aria-hidden="true"><svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7"><circle cx="12" cy="8" r="4" /><path d="M4 22v-2a8 8 0 0 1 16 0v2" /></svg></span>
        <span><strong>{user.fullName || 'Shop Manager'}</strong><small>{user.email}</small></span>
      </a><button disabled={busy} onClick={logout}>Đăng xuất</button></div>
    </header>
      {error && <p role="alert" className="message error">{error}</p>}
      {!current && !profile && <><p>Quản lý nhân viên và mặt hàng của cửa hàng bạn sở hữu.</p><div className="admin-options">{options.map(item => <a key={item.href} href={item.href} className="admin-option"><span className="admin-option-icon" aria-hidden="true">{item.icon}</span><h2>{item.title}</h2><p>{item.description}</p><strong>Mở quản lý →</strong></a>)}</div></>}
      {current && <ManagerShopsView key={current.section} role="SHOP" section={current.section} />}
      {profile && <AdminProfileView user={user} roleLabel="Shop Manager" />}
    </main>
  </div>
}
