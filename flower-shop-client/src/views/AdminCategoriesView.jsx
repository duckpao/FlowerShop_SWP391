import { useAdminCategoriesController } from '../controllers/useAdminCategoriesController'
import { formatPrice } from '../models/productModel'
import '../styles/catalog.css'

const statusLabels = { ACTIVE: 'Đang dùng', INACTIVE: 'Ngừng dùng' }

export default function AdminCategoriesView() {
  const c = useAdminCategoriesController()
  return <>
    <section className="catalog-panel catalog-management">
      <h2>Danh mục sản phẩm</h2>
      {c.error && <p role="alert" className="message error">{c.error}</p>}
      {c.notice && <p role="status" className="message success">{c.notice}</p>}
      {c.busy && <p role="status">Đang tải…</p>}
      <button disabled={c.busy} onClick={c.reload}>Tải lại</button>

      {!c.categories.length && !c.busy && <p>Chưa có danh mục nào. Thêm danh mục để Manager đăng được sản phẩm.</p>}
      <ul className="catalog-list">{c.categories.map(x => <li key={x.id}>
        <strong>{x.name}</strong>
        <p>{x.description || 'Chưa có mô tả'}</p>
        <p className="muted">{statusLabels[x.status] || x.status} · {x.productCount} sản phẩm đang bán</p>
        <button disabled={c.busy} onClick={() => c.edit(x)}>Sửa</button>
      </li>)}</ul>

      <h3>{c.editing ? 'Sửa danh mục' : 'Thêm danh mục'}</h3>
      <form onSubmit={c.save} className="catalog-form"><fieldset disabled={c.busy}>
        <label>Tên danh mục<input required maxLength={100} value={c.form.name}
          onChange={e => c.setForm({ ...c.form, name: e.target.value })} /></label>
        <label>Mô tả<textarea maxLength={1000} value={c.form.description}
          onChange={e => c.setForm({ ...c.form, description: e.target.value })} /></label>
        <label>Trạng thái<select value={c.form.status} onChange={e => c.setForm({ ...c.form, status: e.target.value })}>
          {Object.entries(statusLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
        </select></label>
        <p className="muted">Danh mục ngừng dùng không xuất hiện khi Manager đăng sản phẩm mới, nhưng sản phẩm đang dùng nó vẫn hiển thị bình thường.</p>
        <button className="primary">{c.editing ? 'Lưu thay đổi' : 'Thêm danh mục'}</button>
        {c.editing && <button type="button" onClick={c.cancel}>Hủy</button>}
      </fieldset></form>
    </section>

    <section className="catalog-panel catalog-management">
      <h2>Kiểm duyệt sản phẩm toàn sàn</h2>
      <form onSubmit={c.search}><fieldset disabled={c.busy}>
        <label>Tìm theo tên sản phẩm<input maxLength={100} value={c.query}
          onChange={e => c.setQuery(e.target.value)} /></label>
        <button>Tìm kiếm</button>
      </fieldset></form>

      {c.products && <>
        <p>{c.products.totalElements} sản phẩm</p>
        {!c.products.content.length && <p>Không có sản phẩm phù hợp.</p>}
        <ul className="catalog-list">{c.products.content.map(p => <li key={p.id}>
          <strong>{p.name}</strong>
          <p className="muted">{p.shopName} · {p.categoryName} · {formatPrice(p.price)}</p>
          <p className="muted">{p.adminHidden
            ? 'Bị quản trị viên ẩn'
            : p.available ? 'Đang hiển thị công khai' : 'Shop đang ẩn hoặc cửa hàng chưa hoạt động'}</p>
          <button disabled={c.busy} onClick={() => c.toggleHidden(p)}>
            {p.adminHidden ? 'Bỏ ẩn' : 'Ẩn vi phạm'}
          </button>
        </li>)}</ul>

        <div className="catalog-actions">
          <button disabled={c.busy || c.products.page === 0} onClick={() => c.changePage(-1)}>Trang trước</button>
          <span>Trang {c.products.page + 1} / {Math.max(1, c.products.totalPages)}</span>
          <button disabled={c.busy || c.products.page + 1 >= c.products.totalPages} onClick={() => c.changePage(1)}>Trang sau</button>
        </div>
      </>}
    </section>
  </>
}
