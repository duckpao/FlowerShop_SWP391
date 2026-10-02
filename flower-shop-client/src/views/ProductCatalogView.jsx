import { useCatalogController } from '../controllers/useCatalogController'
import { useFavoriteToggle } from '../controllers/useFavoritesController'
import { formatPrice, productTypeLabels, ratingLabel, sortOptions } from '../models/productModel'
import '../styles/catalog.css'

// Product List + Product Category: hook useCatalogController tải dữ liệu/bộ lọc; useFavoriteToggle xử lý nút tim.
// View chỉ hiển thị state và chuyển sự kiện onSubmit/onChange/onClick sang các hàm của hook.
export default function ProductCatalogView({ auth, shops }) {
  const c = useCatalogController(shops)
  const favorites = useFavoriteToggle(auth?.user)
  return <section id="products" className="catalog-section">
    <header className="catalog-heading">
      <div>
        <p className="catalog-eyebrow">Chợ hoa trực tuyến</p>
        <h2>Sản phẩm từ mọi cửa hàng</h2>
        <p>Khám phá và lọc các sản phẩm đang được bán công khai.</p>
      </div>
      {auth?.user?.role === 'CUSTOMER' && <a href="/favorites">Sản phẩm yêu thích</a>}
    </header>

    <form onSubmit={c.search} className="catalog-filters">
      <label className="catalog-search-field">Tên sản phẩm
        <input type="search" maxLength={100} value={c.draft} onChange={e => c.setDraft(e.target.value)} placeholder="Ví dụ: hoa hồng" />
      </label>
      <label>Danh mục
        <select value={c.filter.categoryId} onChange={e => c.setCategory(e.target.value)}>
          <option value="">Tất cả danh mục</option>
          {c.categories.map(x => <option key={x.id} value={x.id}>{x.name} ({x.productCount})</option>)}
        </select>
      </label>
      <label>Cửa hàng
        <select value={c.filter.shopId} onChange={e => c.setShop(e.target.value)}>
          <option value="">Tất cả cửa hàng</option>
          {c.shops.map(shop => <option key={shop.id} value={shop.id}>{shop.name}</option>)}
        </select>
      </label>
      <label>Loại bó hoa
        <select value={c.filter.type} onChange={e => c.setType(e.target.value)}>
          <option value="">Tất cả loại</option>
          {Object.entries(productTypeLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
        </select>
      </label>
      <label>Giá từ
        <input type="number" min="0" step="1000" inputMode="numeric" value={c.priceDraft.minPrice}
          onChange={e => c.setPriceDraft(current => ({ ...current, minPrice: e.target.value }))} placeholder="0 đ" />
      </label>
      <label>Đến
        <input type="number" min="0" step="1000" inputMode="numeric" value={c.priceDraft.maxPrice}
          onChange={e => c.setPriceDraft(current => ({ ...current, maxPrice: e.target.value }))} placeholder="Không giới hạn" />
      </label>
      <label>Sắp xếp
        <select value={c.filter.sort} onChange={e => c.setSort(e.target.value)}>
          {sortOptions.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
        </select>
      </label>
      <button type="submit" disabled={c.busy}>Tìm sản phẩm</button>
    </form>

    {c.busy && <p role="status">Đang tải sản phẩm…</p>}
    {c.error && <div role="alert"><p className="message error">{c.error}</p>
      <button onClick={c.retry}>Thử lại</button></div>}
    {favorites.error && <p role="alert" className="message error">{favorites.error}</p>}

    {c.data && <>
      <p className="catalog-result-count">{c.data.totalElements} sản phẩm phù hợp</p>
      {!c.data.content.length && <p>Không có sản phẩm phù hợp với bộ lọc.</p>}
      <ul className="catalog-grid">{c.data.content.map(p => <li key={p.id} className="catalog-card">
        {p.imageUrl
          ? <img className="catalog-thumb" src={p.imageUrl} alt={p.name} loading="lazy" />
          : <div className="catalog-thumb-empty">Chưa có ảnh</div>}
        <span className={`catalog-type ${p.type === 'CUSTOM' ? 'catalog-type-custom' : ''}`}>
          {productTypeLabels[p.type] || productTypeLabels.READY_MADE}
        </span>
        <h3><a href={`/products/${encodeURIComponent(p.id)}`}>{p.name}</a></h3>
        <span className="price">{formatPrice(p.price)}</span>
        <span className="muted"><a href={`/shops/${encodeURIComponent(p.shopId)}`}>{p.shopName}</a> · {p.categoryName}</span>
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
  </section>
}
