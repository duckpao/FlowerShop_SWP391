import { useFavoritesController } from '../controllers/useFavoritesController'
import { formatPrice, ratingLabel } from '../models/productModel'
import '../styles/account.css'
import '../styles/catalog.css'

export default function FavoritesView({ auth }) {
  const c = useFavoritesController(auth?.user)

  if (auth?.busy && !auth?.user) return <main className="account-page"><p role="status">Đang kiểm tra đăng nhập…</p></main>

  if (!auth?.user) return <main className="account-page">
    <h1>Sản phẩm yêu thích</h1>
    <p>Hãy đăng nhập bằng tài khoản Khách hàng để xem danh sách đã lưu.</p>
    <a href="/login?next=%2Ffavorites">Đăng nhập</a>
  </main>

  if (auth.user.role !== 'CUSTOMER') return <main className="account-page">
    <h1>Sản phẩm yêu thích</h1>
    <p>Chỉ tài khoản Khách hàng mới có danh sách sản phẩm yêu thích.</p>
    <a href="/">Về trang chủ</a>
  </main>

  return <main className="account-page">
    <header className="account-header">
      <div><h1>✿ Sản phẩm yêu thích</h1><p>{auth.user.email}</p></div>
      <a href="/products">Tất cả sản phẩm</a>
      <a href="/">Trang chủ</a>
    </header>

    {c.busy && <p role="status">Đang tải…</p>}
    {c.error && <div role="alert"><p className="message error">{c.error}</p>
      <button onClick={c.reload}>Thử lại</button></div>}
    {c.notice && <p className="message success" role="status">{c.notice}</p>}

    {c.data && <>
      <p>{c.data.totalElements} sản phẩm đã lưu</p>
      {!c.data.content.length && <p>Bạn chưa lưu sản phẩm nào. Mở <a href="/products">trang sản phẩm</a> để bắt đầu.</p>}
      <ul className="catalog-grid">{c.data.content.map(p => <li key={p.id}
        className={p.available ? 'catalog-card' : 'catalog-card catalog-unavailable'}>
        {p.imageUrl
          ? <img className="catalog-thumb" src={p.imageUrl} alt={p.name} loading="lazy" />
          : <div className="catalog-thumb-empty">Chưa có ảnh</div>}
        <h3>{p.available
          ? <a href={`/products/${encodeURIComponent(p.id)}`}>{p.name}</a>
          : p.name}</h3>
        <span className="price">{formatPrice(p.price)}</span>
        <span className="muted">{p.shopName} · {p.categoryName}</span>
        <span className="muted">{ratingLabel(p)}</span>
        {!p.available && <span className="message error">Không còn bán</span>}
        <button disabled={c.busy} onClick={() => c.remove(p.id)}>Bỏ lưu</button>
      </li>)}</ul>

      <div className="catalog-actions">
        <button disabled={c.busy || c.data.page === 0} onClick={() => c.setPage(x => x - 1)}>Trang trước</button>
        <span>Trang {c.data.page + 1} / {Math.max(1, c.data.totalPages)}</span>
        <button disabled={c.busy || c.data.page + 1 >= c.data.totalPages} onClick={() => c.setPage(x => x + 1)}>Trang sau</button>
      </div>
    </>}
  </main>
}
