import HomeView from './HomeView'
import AccountView from './AccountView'
import ManagerApplicationView from './ManagerApplicationView'
import '../styles/admin.css'

const options = [
  { href: '/home', key: 'home', title: 'Cửa hàng hoa mẫu', icon: '🌸', description: 'Xem các bó hoa thiết kế và thêm vào giỏ hàng.' },
  { href: '/cart', key: 'cart', title: 'Giỏ hàng của tôi', icon: '🛒', description: 'Xem giỏ hàng và thanh toán SePay / COD.' },
  { href: '/?view=shops', key: 'shops', title: 'Tìm cửa hàng hoa', icon: '❀', description: 'Tìm shop, xem sản phẩm và đăng ký làm nhân viên.' },
  { href: '/?view=applications', key: 'applications', title: 'Đơn ứng tuyển', icon: '♙', description: 'Theo dõi phản hồi và kết quả đăng ký làm nhân viên.' },
  { href: '/?view=manager', key: 'manager', title: 'Đăng ký mở shop', icon: '⌂', description: 'Gửi đơn mở cửa hàng và theo dõi kết quả xét duyệt.' },
  { href: '/account', key: 'profile', title: 'Tài khoản và địa chỉ', icon: '♧', description: 'Cập nhật hồ sơ cá nhân và địa chỉ giao hàng.' },
]

export default function CustomerDashboardView({ auth, path }) {
  const section = path === '/account' ? 'profile' : path.startsWith('/shops/') ? 'shops' : new URLSearchParams(window.location.search).get('view')
  const current = options.find(option => option.key === section)
  return <div className="admin-shell">
    <aside className="admin-sidebar"><a className="admin-brand" href="/">✿ FlowerShop</a><p>CUSTOMER</p>
      <nav aria-label="Điều hướng Customer"><a href="/" aria-current={!current ? 'page' : undefined}>Tổng quan</a>
        {options.map(option => <a key={option.key} href={option.href} aria-current={current === option ? 'page' : undefined}>{option.icon} {option.title}</a>)}
      </nav>
    </aside>
    <main className="account-page admin-content"><header className="account-header">
      <div><p className="admin-eyebrow">FLOWERSHOP / CUSTOMER</p><h1>{current?.title || 'Dashboard Customer'}</h1></div>
      <div className="admin-user"><a className="manager-avatar-link" href="/account" aria-label="Xem thông tin cá nhân Customer">
        <span className="manager-avatar" aria-hidden="true">♙</span><span><strong>{auth.user.fullName || 'Customer'}</strong><small>{auth.user.email}</small></span>
      </a><button disabled={auth.busy} onClick={auth.logout}>Đăng xuất</button></div>
    </header>
      {auth.error && <p className="message error" role="alert">{auth.error}</p>}
      {!current && <><p>Bạn đang là Customer. Khám phá cửa hàng và quản lý tài khoản của bạn.</p>
        <div className="admin-options">{options.map(option => <a className="admin-option" key={option.key} href={option.href}><span className="admin-option-icon" aria-hidden="true">{option.icon}</span><h2>{option.title}</h2><p>{option.description}</p><strong>Mở chức năng →</strong></a>)}</div>
      </>}
      {['shops', 'applications'].includes(current?.key) && <HomeView auth={auth} embedded section={current.key} />}
      {current?.key === 'manager' && <ManagerApplicationView user={auth.user} />}
      {current?.key === 'profile' && <AccountView {...auth} embedded />}
    </main>
  </div>
}
