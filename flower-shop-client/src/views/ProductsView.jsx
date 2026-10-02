import { useProductsController } from '../controllers/useProductsController'
import { formatPrice, productStatusLabels, productTypeLabels, ratingLabel } from '../models/productModel'
import '../styles/catalog.css'

// Product Management: manage=true mở form Shop; manage=false chỉ xem sản phẩm công khai của một shop.
// useProductsController chọn API theo manage. Form hiện tại lưu URL ảnh HTTPS, chưa gọi API upload ảnh/video.
export default function ProductsView({ shop, manage = false }) {
  const c = useProductsController(shop.id, manage)
  const disabled = c.busy || shop.status !== 'ACTIVE'
  return <section className="catalog-panel catalog-management">
    <h2>{manage ? 'Quản lý mặt hàng của shop' : 'Sản phẩm của cửa hàng'}</h2>
    {c.error && <p role="alert" className="message error">{c.error}</p>}
    {c.notice && <p role="status" className="message success">{c.notice}</p>}
    <button disabled={c.busy} onClick={c.reload}>Tải lại sản phẩm</button>

    {manage && <form onSubmit={c.save} className="catalog-form">
      <h3>{c.editing ? 'Sửa sản phẩm' : 'Đăng sản phẩm mới'}</h3>
      <fieldset disabled={disabled}>
        <label>Tên sản phẩm<input required maxLength={255} value={c.form.name} onChange={e => c.setForm({ ...c.form, name: e.target.value })} /></label>
        <label>Mô tả<textarea maxLength={5000} value={c.form.description} onChange={e => c.setForm({ ...c.form, description: e.target.value })} /></label>
        <label>Danh mục<select required value={c.form.categoryId} onChange={e => c.setForm({ ...c.form, categoryId: e.target.value })}>
          <option value="">Chọn danh mục</option>{c.categories.map(x => <option key={x.id} value={x.id}>{x.name}</option>)}
        </select></label>
        {!c.categories.length && <p>Chưa có danh mục đang hoạt động. Admin cần thêm danh mục trước khi đăng sản phẩm.</p>}
        <label>Giá (VND)<input required type="number" min="0.01" max="9999999999.99" step="0.01" value={c.form.price} onChange={e => c.setForm({ ...c.form, price: e.target.value })} /></label>
        <label>Tồn kho<input required type="number" min="0" max="1000000" step="1" value={c.form.stock} onChange={e => c.setForm({ ...c.form, stock: e.target.value })} /></label>
        <label>Trạng thái<select value={c.form.status} onChange={e => c.setForm({ ...c.form, status: e.target.value })}>
          {Object.entries(productStatusLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
        </select></label>
        <label>Loại bó hoa<select value={c.form.type} onChange={e => c.setForm({ ...c.form, type: e.target.value })}>
          {Object.entries(productTypeLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
        </select></label>

        <h4>Ảnh sản phẩm</h4>
        <p className="muted">Dán URL ảnh HTTPS, tối đa 10 ảnh. Ảnh chính hiển thị ở trang danh sách.</p>
        {c.form.images.map((image, index) => <div key={index} className="product-image-row">
          <input type="url" maxLength={500} pattern="https://.*" placeholder="https://..." value={image.url}
            onChange={e => c.setImageUrl(index, e.target.value)} />
          <label><input type="radio" name="primary-image" checked={image.primary} onChange={() => c.setPrimaryImage(index)} /> Ảnh chính</label>
          <button type="button" onClick={() => c.removeImage(index)}>Xóa ảnh</button>
        </div>)}
        {c.form.images.length < 10 && <button type="button" onClick={c.addImage}>Thêm ảnh</button>}

        <button disabled={!c.categories.length}>Lưu sản phẩm</button>
        {c.editing && <button type="button" onClick={c.cancel}>Hủy sửa</button>}
      </fieldset>
    </form>}

    {c.data && <>
      <p>{c.data.totalElements} sản phẩm</p>
      <ul className="catalog-grid">{c.data.content.map(p => <li key={p.id} className="catalog-card">
        {manage
          ? (p.images?.length
              ? <img className="catalog-thumb" src={p.images[0]} alt={p.name} loading="lazy" />
              : <div className="catalog-thumb-empty">Chưa có ảnh</div>)
          : (p.imageUrl
              ? <img className="catalog-thumb" src={p.imageUrl} alt={p.name} loading="lazy" />
              : <div className="catalog-thumb-empty">Chưa có ảnh</div>)}
        <h3>{manage ? p.name : <a href={`/products/${encodeURIComponent(p.id)}`}>{p.name}</a>}</h3>
        <span className={`catalog-type ${p.type === 'CUSTOM' ? 'catalog-type-custom' : ''}`}>
          {productTypeLabels[p.type] || productTypeLabels.READY_MADE}
        </span>
        <span className="price">{formatPrice(p.price)}</span>
        <span className="muted">{p.categoryName} · Còn {p.stock}</span>
        {!manage && <span className="muted">{ratingLabel(p)}</span>}
        {manage && <>
          <span className="muted">{productStatusLabels[p.status] || p.status}</span>
          {p.adminHidden && <span className="message error">Bị quản trị viên ẩn khỏi trang công khai</span>}
          <div className="catalog-actions">
            <button disabled={disabled} onClick={() => c.edit(p)}>Sửa</button>
            <button disabled={disabled || p.status === 'INACTIVE'} onClick={() => c.hide(p.id)}>Ẩn sản phẩm</button>
          </div>
        </>}
      </li>)}</ul>

      <div className="catalog-actions">
        <button disabled={c.busy || c.page === 0} onClick={() => c.setPage(x => x - 1)}>Trang trước</button>
        <button disabled={c.busy || c.page + 1 >= c.data.totalPages} onClick={() => c.setPage(x => x + 1)}>Trang sau</button>
      </div>
    </>}
  </section>
}
