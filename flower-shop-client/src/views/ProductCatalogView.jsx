import { useCatalogController } from '../controllers/useCatalogController'
import { useFavoriteToggle } from '../controllers/useFavoritesController'
import { formatPrice, ratingLabel, sortOptions } from '../models/productModel'
import '../styles/account.css'
import '../styles/catalog.css'

export default function ProductCatalogView({ auth }) {
  const c = useCatalogController()
  const favorites = useFavoriteToggle(auth?.user)
  return <main className="account-page">
    <header className="account-header">
      <div><h1>✿ Sản phẩm</h1><p>Hoa tươi từ tất cả cửa hàng đang hoạt động.</p></div>
      <a href="/">Trang chủ</a>
      {auth?.user?.role === 'CUSTOMER' && <a href="/favorites">Sản phẩm yêu thích</a>}
      {!auth?.user && <a href="/login">Đăng nhập</a>}
    </header>

    <form onSubmit={c.search} className="catalog-filters">
      <label>Tên sản phẩm
        <input maxLength={100} value={c.draft} onChange={e => c.setDraft(e.target.value)} placeholder="Nhập tên hoa" />
      </label>
      <label>Danh mục
        <select value={c.filter.categoryId} onChange={e => c.setCategory(e.target.value)}>
          <option value="">Tất cả danh mục</option>
          {c.categories.map(x => <option key={x.id} value={x.id}>{x.name} ({x.productCount})</option>)}
        </select>
      </label>
      <label>Sắp xếp
        <select value={c.filter.sort} onChange={e => c.setSort(e.target.value)}>
          {sortOptions.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
        </select>
      </label>
      <button disabled={c.busy}>Tìm kiếm</button>
    </form>

    {c.busy && <p role="status">Đang tải sản phẩm…</p>}
    {c.error && <div role="alert"><p className="message error">{c.error}</p>
      <button onClick={c.retry}>Thử lại</button></div>}
    {favorites.error && <p role="alert" className="message error">{favorites.error}</p>}

    {c.data && <>
      <p>{c.data.totalElements} sản phẩm</p>
      {!c.data.content.length && <p>Không có sản phẩm phù hợp với bộ lọc.</p>}
      <ul className="catalog-grid">{c.data.content.map(p => <li key={p.id} className="catalog-card">
        {p.imageUrl
          ? <img className="catalog-thumb" src={p.imageUrl} alt={p.name} loading="lazy" />
          : <div className="catalog-thumb-empty">Chưa có ảnh</div>}
        <h3><a href={`/products/${encodeURIComponent(p.id)}`}>{p.name}</a></h3>
        <span className="price">{formatPrice(p.price)}</span>
        <span className="muted">{p.shopName} · {p.categoryName}</span>
        <span className="muted">{ratingLabel(p)}</span>
        {(!auth?.user || auth.user.role === 'CUSTOMER') && <button className="favorite-button"
          aria-pressed={favorites.saved.has(p.id)} onClick={() => favorites.toggle(p.id)}>
          {favorites.saved.has(p.id) ? '♥ Đã lưu' : '♡ Lưu yêu thích'}
        </button>}
      </li>)}</ul>

      <div className="catalog-actions">
        <button disabled={c.busy || c.data.page === 0} onClick={() => c.changePage(-1)}>Trang trước</button>
        <span>Trang {c.data.page + 1} / {Math.max(1, c.data.totalPages)}</span>
        <button disabled={c.busy || c.data.page + 1 >= c.data.totalPages} onClick={() => c.changePage(1)}>Trang sau</button>
      </div>
    </>}
  </main>
}
