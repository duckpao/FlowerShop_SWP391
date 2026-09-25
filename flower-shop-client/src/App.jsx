import { BrowserRouter } from 'react-router-dom'
import { useEffect } from 'react'
import { useAuthController } from './controllers/useAuthController'
import { useCart } from './hooks/useCart'
import Navbar from './components/common/Navbar'
import HomePage from './pages/HomePage'
import CartPage from './pages/CartPage'
import PaymentSuccessPage from './pages/PaymentSuccessPage'
import PaymentErrorPage from './pages/PaymentErrorPage'
import PaymentCancelPage from './pages/PaymentCancelPage'

import AuthView from './views/AuthView'
import HomeView from './views/HomeView'
import CustomerDashboardView from './views/CustomerDashboardView'
import AdminCustomersView from './views/AdminCustomersView'
import AdminShopsView from './views/AdminShopsView'
import AdminManagerApplicationsView from './views/AdminManagerApplicationsView'
import AdminDashboardView from './views/AdminDashboardView'
import ManagerShopsView from './views/ManagerShopsView'
import ManagerDashboardView from './views/ManagerDashboardView'
import './styles/account.css'

const DEMO_USER_ID = 'user-customer-01'

export default function App() {
  return (
    <BrowserRouter>
      <MainApp />
    </BrowserRouter>
  )
}

function MainApp() {
  const controller = useAuthController()
  const path = window.location.pathname
  const currentUserId = controller.user?.id || DEMO_USER_ID
  const cartHook = useCart(currentUserId)

  if (import.meta.env.DEV && path === '/dev-manager') {
    return (
      <main className="account-page">
        <h1>Manager thử nghiệm local</h1>
        <p className="message error">Không cần đăng nhập. Thao tác ghi vào DB hiện tại bằng chủ shop cấu hình ở backend.</p>
        <p>Backend cần profile dev-manager. <a href="/dev-admin">Admin thử nghiệm</a> · <a href="/">Đăng nhập</a></p>
        <ManagerShopsView role="SHOP" />
      </main>
    )
  }

  if (import.meta.env.DEV && path === '/dev-admin') {
    return (
      <main className="account-page">
        <h1>Admin thử nghiệm local</h1>
        <p className="message error">Chế độ dev-admin: không cần đăng nhập. Các thay đổi sẽ ghi vào database đang cấu hình.</p>
        <p>Backend phải chạy với profile dev-admin. <a href="/">Về trang đăng nhập</a></p>
        <AdminCustomersView />
        <AdminShopsView />
        <AdminManagerApplicationsView />
      </main>
    )
  }

  // SePay payment gateway callbacks - allow direct access
  if (path === '/payment/success') return <PaymentSuccessPage />
  if (path === '/payment/error') return <PaymentErrorPage />
  if (path === '/payment/cancel') return <PaymentCancelPage />

  // Cart page
  if (path === '/cart') {
    return (
      <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', backgroundColor: '#f8f9fa' }}>
        <Navbar cartCount={cartHook.cart?.totalQuantity || 0} userId={currentUserId} />
        <main style={{ flex: 1 }}>
          <CartPage
            cart={cartHook.cart}
            loading={cartHook.loading}
            error={cartHook.error}
            fetchCart={cartHook.fetchCart}
            updateItem={cartHook.updateItem}
            removeItem={cartHook.removeItem}
            userId={currentUserId}
          />
        </main>
      </div>
    )
  }

  // Sample storefront / shopping page
  if (path === '/home') {
    return (
      <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', backgroundColor: '#f8f9fa' }}>
        <Navbar cartCount={cartHook.cart?.totalQuantity || 0} userId={currentUserId} />
        <main style={{ flex: 1 }}>
          <HomePage addToCart={cartHook.addToCart} loading={cartHook.loading} />
        </main>
      </div>
    )
  }

  return <AuthApp controller={controller} path={path} />
}

function AuthApp({ controller, path }) {
  useEffect(() => {
    if (!controller.initializing && !controller.user && path !== '/login') {
      window.location.replace('/login')
    }
  }, [controller.initializing, controller.user, path])

  useEffect(() => {
    if (path === '/login' && controller.user) {
      const next = new URLSearchParams(window.location.search).get('next') || '/'
      window.location.replace(
        controller.user.role === 'ADMIN'
          ? '/admin'
          : controller.user.role === 'SHOP'
          ? '/shop-admin'
          : controller.user.role === 'CUSTOMER'
          ? '/'
          : /^\/shops\/[a-zA-Z0-9-]+$/.test(next)
          ? next
          : '/'
      )
    }
  }, [path, controller.user])

  if (path === '/login') return <AuthView {...controller} />

  if (!controller.user) {
    return (
      <main className="account-page">
        <p role="status">{controller.initializing ? 'Đang kiểm tra đăng nhập…' : 'Đang chuyển đến trang đăng nhập…'}</p>
      </main>
    )
  }

  if (['/admin', '/admin/shops', '/admin/approvals', '/admin/users', '/admin/profile'].includes(path)) {
    if (controller.busy && !controller.user) return <main className="account-page"><p role="status">Đang kiểm tra đăng nhập…</p></main>
    if (!controller.user) return <main className="account-page"><h1>Trang quản trị</h1><p>Hãy đăng nhập tài khoản Admin.</p><a href={`/login?next=${encodeURIComponent(path)}`}>Đăng nhập</a></main>
    if (controller.user.role !== 'ADMIN') return <main className="account-page"><h1>Không có quyền truy cập</h1><p>Chỉ Admin được duyệt đơn mở shop.</p><a href="/">Về trang chủ</a></main>
    return <AdminDashboardView {...controller} path={path} />
  }

  if (controller.user?.role === 'CUSTOMER' && (['/', '/account'].includes(path) || path.startsWith('/shops/'))) {
    return <CustomerDashboardView auth={controller} path={path} />
  }

  if (path === '/account' && controller.user) {
    return <><a href="/">Trang chủ</a><AuthView {...controller} /></>
  }

  if (['/shop-admin', '/shop-admin/staff', '/shop-admin/products', '/shop-admin/shop', '/shop-admin/profile'].includes(path)) {
    if (controller.busy && !controller.user) return <main className="account-page"><p role="status">Đang kiểm tra đăng nhập…</p></main>
    if (!controller.user) return <main className="account-page"><h1>Trang quản trị shop</h1><a href="/login">Đăng nhập</a></main>
    if (controller.user.role === 'SHOP') return <ManagerDashboardView {...controller} path={path === '/shop-admin' && new URLSearchParams(window.location.search).has('notifications') ? '/shop-admin/staff' : path} />
    if (controller.user.role === 'SHOP_STAFF' && path === '/shop-admin') return <main className="account-page"><a href="/">Trang chủ</a><h1>Bạn đang là Staff</h1><ManagerShopsView role="SHOP_STAFF" /></main>
    return <main className="account-page"><h1>Không có quyền truy cập</h1><p>Chỉ Shop Manager được quản lý nhân viên và cửa hàng.</p><a href="/">Về trang chủ</a></main>
  }

  return <HomeView auth={controller} />
}
