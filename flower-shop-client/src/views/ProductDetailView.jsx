import { useProductDetailController } from '../controllers/useProductDetailController'
import { useFavoriteToggle } from '../controllers/useFavoritesController'
import ProductReviewsView from './ProductReviewsView'
import { formatPrice, productTypeLabels } from '../models/productModel'
import '../styles/account.css'
import '../styles/catalog.css'

export default function ProductDetailView({ auth, productId }) {
  const c = useProductDetailController(productId)
  const favorites = useFavoriteToggle(auth?.user)
  const p = c.product
  return <main className="catalog-page">
    <header className="catalog-page-header">
      <div><h1>Chi tiết sản phẩm</h1></div>
      <nav className="flex flex-wrap gap-4">
        <a href="/products">Tất cả sản phẩm</a>
        {auth?.user?.role === 'CUSTOMER' && <a href="/favorites">Sản phẩm yêu thích</a>}
        {!auth?.user && <a href="/login">Đăng nhập</a>}
      </nav>
    </header>

    {c.busy && <p role="status">Đang tải…</p>}
    {c.error && <div role="alert"><p className="message error">{c.error}</p>
      <button onClick={c.reload}>Thử lại</button></div>}

    {p && <section className="catalog-panel catalog-detail">
      <div className="catalog-detail-layout">
        <div>
          {p.images.length
            ? <div className="catalog-gallery">{p.images.map((url, index) =>
                <img key={url} src={url} alt={`${p.name} ${index + 1}`} loading="lazy" />)}</div>
            : <div className="catalog-thumb-empty">Chưa có ảnh</div>}
        </div>
        <div className="catalog-detail-info">
          <span className={`catalog-type ${p.type === 'CUSTOM' ? 'catalog-type-custom' : ''}`}>
            {productTypeLabels[p.type] || productTypeLabels.READY_MADE}
          </span>
          <h2>{p.name}</h2>
          <p className="price">{formatPrice(p.price)}</p>
          <p className="muted">Danh mục: {p.categoryName} · Còn {p.stock} sản phẩm</p>
          <p className="muted">
            {p.reviewCount ? `★ ${p.rating} từ ${p.reviewCount} đánh giá` : 'Chưa có đánh giá'}
          </p>
          <p className="muted">Bán bởi <a href={`/shops/${encodeURIComponent(p.shopId)}`}>{p.shopName}</a></p>
          {favorites.error && <p role="alert" className="message error">{favorites.error}</p>}
          {(!auth?.user || auth.user.role === 'CUSTOMER') && <button className="primary favorite-button"
            aria-pressed={favorites.saved.has(p.id)} onClick={() => favorites.toggle(p.id)}>
            {favorites.saved.has(p.id) ? '♥ Đã lưu' : '♡ Lưu yêu thích'}
          </button>}
        </div>
      </div>
      <p className="catalog-detail-description">{p.description || 'Cửa hàng chưa thêm mô tả cho sản phẩm này.'}</p>
    </section>}

    {p && <ProductReviewsView product={p} auth={auth} />}
  </main>
}
