import { useShopSearchController } from '../controllers/useShopSearchController'
import ManagerNotificationsView from './ManagerNotificationsView'
import ManagerApplicationView from './ManagerApplicationView'
import ProductsView from './ProductsView'
const roles = { CUSTOMER: 'Customer', SHOP_STAFF: 'Staff', SHOP: 'Manager', ADMIN: 'Admin' }
const statuses = { PENDING: 'Chờ duyệt', APPROVED: 'Đã duyệt', REJECTED: 'Đã từ chối' }
export default function HomeView({ auth }) {
  const c = useShopSearchController(auth.user)
  return <main className="account-page"><h1>✿ FlowerShop</h1>
    <header className="account-header">{auth.user ? <><div><p>{auth.user.email}</p><strong>Bạn đang là {roles[auth.user.role] || auth.user.role}</strong></div>
      {auth.user.role !== 'CUSTOMER' && <a href="/account">Hồ sơ cá nhân</a>}
      {auth.user.role === 'ADMIN' && <a className="primary" href="/admin">Dashboard Admin</a>}
      {['SHOP', 'SHOP_STAFF'].includes(auth.user.role) && <a href="/shop-admin">Vào trang quản trị shop</a>}
      <div className="customer-header-actions"><button disabled={auth.busy} onClick={auth.logout}>Đăng xuất</button>
        {auth.user.role === 'CUSTOMER' && <a className="customer-profile-icon" href="/account" aria-label="Quản lý thông tin tài khoản" title="Quản lý thông tin tài khoản">
          <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" aria-hidden="true"><circle cx="12" cy="8" r="4" /><path d="M4 22v-2a8 8 0 0 1 16 0v2" /></svg>
        </a>}
      </div></> : <a href="/login">Đăng nhập</a>}</header>
    {auth.error && <p role="alert">{auth.error}</p>}
    {auth.user?.role === 'SHOP' && <ManagerNotificationsView />}
    {auth.user && ['CUSTOMER', 'SHOP'].includes(auth.user.role) && <ManagerApplicationView key={auth.user.id} user={auth.user} />}
    {c.sessionExpired && <p role="alert">Phiên đăng nhập đã thay đổi hoặc hết hạn. <a href="/login">Đăng nhập lại để xem quyền và kết quả ứng tuyển mới nhất.</a></p>}
    {!c.selected && <>
    <p><a className="primary" href="/products">Xem tất cả sản phẩm</a>
      {auth.user?.role === 'CUSTOMER' && <> · <a href="/favorites">Sản phẩm yêu thích</a></>}</p>
    <h2>Tìm cửa hàng hoa</h2><form onSubmit={c.search}><label>Tên shop<input maxLength={100} value={c.query} onChange={e => c.setQuery(e.target.value)} /></label><button disabled={c.busy}>Tìm kiếm</button></form>
    {c.error && <p role="alert" className="message error">{c.error}</p>}{c.notice && <p role="status">{c.notice}</p>}
    <p>Hiển thị tối đa 50 shop đang hoạt động. Nhập tên để thu hẹp kết quả.</p>
    <ul className="address-list">{c.shops.map(shop => <li key={shop.id}><h3>{shop.name}</h3><p>{shop.description}</p><button onClick={() => c.select(shop)}>Vào shop</button></li>)}</ul>
    </>}
    {c.selected && <section className="account-card"><button onClick={() => c.select(null)}>← Quay lại danh sách shop</button><h2>✿ {c.selected.name}</h2><p>{c.selected.description || 'Chào mừng bạn đến với cửa hàng.'}</p>
      <h3>Giới thiệu cửa hàng</h3><p>Khám phá cửa hàng và cơ hội trở thành thành viên của đội ngũ.</p>
      <ProductsView key={c.selected.id} shop={c.selected} />
      <h3>Tuyển dụng</h3><p>Bạn yêu thích hoa và muốn làm việc tại {c.selected.name}? Hãy gửi thông tin để Manager xem xét.</p>
      {c.error && <p role="alert">{c.error}</p>}{c.notice && <p role="status">{c.notice}</p>}
      {auth.user?.role === 'CUSTOMER' ? <><button aria-expanded={c.showApplication} onClick={() => c.setShowApplication(!c.showApplication)}>{c.showApplication ? 'Đóng form' : 'Đăng ký làm nhân viên'}</button>
      {c.showApplication && <><h3>Thông tin ứng tuyển</h3><form onSubmit={c.submit}><fieldset disabled={c.busy}>
        <label>Họ và tên<input required maxLength={100} value={c.form.fullName} onChange={e => c.setForm({ ...c.form, fullName: e.target.value })} /></label>
        <label>Số điện thoại<input required pattern="[+0-9 ()\-]{8,20}" maxLength={20} value={c.form.phone} onChange={e => c.setForm({ ...c.form, phone: e.target.value })} /></label>
        <label>Giới thiệu / kinh nghiệm<textarea required maxLength={1000} value={c.form.introduction} onChange={e => c.setForm({ ...c.form, introduction: e.target.value })} /></label>
        <p>Khi Manager duyệt, tài khoản của bạn sẽ chuyển từ Customer sang Staff. Bạn cần đăng nhập lại để sử dụng quyền mới.</p><button>Gửi đơn đăng ký làm nhân viên</button>
      </fieldset></form></>}</> : !auth.user ? <a href={`/login?next=${encodeURIComponent(window.location.pathname)}`}>Đăng ký làm nhân viên — đăng nhập để tiếp tục</a> : <p>Chỉ Customer được gửi đơn xin làm nhân viên.</p>}
    </section>}
    {auth.user && <details className="account-card"><summary>Thông báo ứng tuyển của tôi ({c.applications.length})</summary><ul>{c.applications.map(a => <li key={a.id}><strong>{a.shopName}: {statuses[a.status]}</strong><p>{a.status === 'APPROVED' ? 'Chúc mừng! Bạn đã trở thành nhân viên của shop. Đăng nhập lại để sử dụng quyền Staff.' : a.status === 'REJECTED' ? 'Cảm ơn bạn đã ứng tuyển. Shop chưa thể tiếp nhận bạn ở thời điểm này. Tài khoản của bạn vẫn là Customer.' : 'Đơn đã được gửi đến Manager và đang chờ xem xét.'}</p></li>)}</ul></details>}
  </main>
}
