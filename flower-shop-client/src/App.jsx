import AuthView from './views/AuthView'
import HomeView from './views/HomeView'
import { useEffect } from 'react'
import { useAuthController } from './controllers/useAuthController'
import AdminCustomersView from './views/AdminCustomersView'
import AdminShopsView from './views/AdminShopsView'
import ManagerShopsView from './views/ManagerShopsView'
import './styles/account.css'

export default function App() {
  if (import.meta.env.DEV && window.location.pathname === '/dev-manager') return <main className="account-page">
    <h1>Manager thử nghiệm local</h1>
    <p className="message error">Không cần đăng nhập. Thao tác ghi vào DB hiện tại bằng chủ shop cấu hình ở backend.</p>
    <p>Backend cần profile dev-manager. <a href="/dev-admin">Admin thử nghiệm</a> · <a href="/">Đăng nhập</a></p>
    <ManagerShopsView role="SHOP" />
  </main>
  if (import.meta.env.DEV && window.location.pathname === '/dev-admin') return <main className="account-page">
    <h1>Admin thử nghiệm local</h1>
    <p className="message error">Chế độ dev-admin: không cần đăng nhập. Các thay đổi sẽ ghi vào database đang cấu hình.</p>
    <p>Backend phải chạy với profile dev-admin. <a href="/">Về trang đăng nhập</a></p>
    <AdminCustomersView />
    <AdminShopsView />
  </main>
  return <AuthApp />
}

function AuthApp() {
  const controller = useAuthController()
  const path = window.location.pathname
  useEffect(() => {
    if (path === '/login' && controller.user) {
      const next = new URLSearchParams(window.location.search).get('next') || '/'
      window.location.replace(/^\/shops\/[a-zA-Z0-9-]+$/.test(next) ? next : '/')
    }
  }, [path, controller.user])
  if (path === '/login') return <AuthView {...controller} />
  if (path === '/account' && controller.user) return <><a href="/">Trang chủ</a><AuthView {...controller} /></>
  if (path === '/shop-admin' && controller.user && ['SHOP', 'SHOP_STAFF'].includes(controller.user.role)) return <main className="account-page"><a href="/">Trang chủ</a><h1>Bạn đang là {controller.user.role === 'SHOP' ? 'Manager' : 'Staff'}</h1><ManagerShopsView role={controller.user.role} /></main>
  return <HomeView auth={controller} />
}
