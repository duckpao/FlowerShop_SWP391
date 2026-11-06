import { useAdminCategoriesController } from '../controllers/useAdminCategoriesController'
import { formatPrice } from '../models/productModel'
import PageMeta from '../components/common/PageMeta'
import PageBreadCrumb from '../components/common/PageBreadCrumb'
import ComponentCard from '../components/common/ComponentCard'
import Badge from '../components/ui/badge/Badge'

const statusLabels = { ACTIVE: 'Đang dùng', INACTIVE: 'Ngừng dùng' }

const field = 'w-full rounded-lg border border-gray-300 px-3.5 py-2.5 text-sm text-gray-900 focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 disabled:opacity-60 dark:border-gray-700 dark:bg-gray-900 dark:text-white'
const primaryBtn = 'inline-flex items-center justify-center rounded-lg bg-brand-500 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-brand-600 disabled:cursor-not-allowed disabled:bg-brand-300'
const outlineBtn = 'inline-flex items-center justify-center rounded-lg px-3 py-2 text-sm font-medium text-gray-700 ring-1 ring-inset ring-gray-300 transition hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-60 dark:text-gray-300 dark:ring-gray-700 dark:hover:bg-white/5'
const dangerBtn = 'inline-flex items-center justify-center rounded-lg px-3 py-2 text-sm font-medium text-error-600 ring-1 ring-inset ring-error-200 transition hover:bg-error-50 disabled:cursor-not-allowed disabled:opacity-60 dark:ring-error-500/30 dark:hover:bg-error-500/10'

function CategoryPanel({ c }) {
  const groupNames = [...new Set(c.categories.map(x => x.groupName).filter(Boolean))]
  return (
    <ComponentCard title="Quản lý danh mục" desc="Thêm, sửa, xóa danh mục và đổi tên các nhóm hiển thị trên menu cửa hàng.">
      <form onSubmit={c.save} className="space-y-4 rounded-xl border border-gray-200 p-4 dark:border-white/10">
        <fieldset disabled={c.busy} className="space-y-4">
          <h4 className="font-semibold text-gray-900 dark:text-white">{c.editing ? 'Sửa danh mục' : 'Thêm danh mục'}</h4>
          <div className="grid gap-4 sm:grid-cols-2">
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">Tên danh mục
              <input required maxLength={100} className={`${field} mt-1.5`} value={c.form.name}
                onChange={e => c.setForm({ ...c.form, name: e.target.value })} />
            </label>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">Nhóm menu
              <input list="category-group-options" maxLength={50} className={`${field} mt-1.5`} value={c.form.groupName}
                placeholder="Chọn nhóm có sẵn hoặc gõ nhóm mới"
                onChange={e => c.setForm({ ...c.form, groupName: e.target.value })} />
              <datalist id="category-group-options">{groupNames.map(name => <option key={name} value={name} />)}</datalist>
            </label>
          </div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">Mô tả
            <textarea maxLength={1000} rows={2} className={`${field} mt-1.5`} value={c.form.description}
              onChange={e => c.setForm({ ...c.form, description: e.target.value })} />
          </label>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">Trạng thái
            <select className={`${field} mt-1.5`} value={c.form.status} onChange={e => c.setForm({ ...c.form, status: e.target.value })}>
              {Object.entries(statusLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
            </select>
          </label>
          <p className="text-xs text-gray-500">Danh mục ngừng dùng không hiện trên menu và khi Manager đăng sản phẩm mới; sản phẩm đang dùng nó vẫn hiển thị bình thường. Để trống nhóm nếu chưa muốn phân nhóm.</p>
          <div className="flex gap-3">
            <button type="submit" className={primaryBtn}>{c.editing ? 'Lưu thay đổi' : 'Thêm danh mục'}</button>
            {c.editing && <button type="button" className={outlineBtn} onClick={c.cancel}>Hủy</button>}
          </div>
        </fieldset>
      </form>

      {!c.categories.length && !c.busy && <p className="text-sm text-gray-500">Chưa có danh mục nào. Thêm danh mục để Manager đăng được sản phẩm.</p>}

      {c.groups.map(group => (
        <section key={group.name || '__none'} aria-label={group.name || 'Chưa phân nhóm'}>
          <div className="mb-2 flex flex-wrap items-center justify-between gap-2">
            {c.groupEdit && group.name && c.groupEdit.from === group.name ? (
              <form onSubmit={c.saveGroup} className="flex flex-1 flex-wrap items-center gap-2">
                <input autoFocus maxLength={50} aria-label={`Tên mới của nhóm ${group.name}`} className={`${field} max-w-xs`}
                  value={c.groupEdit.to} disabled={c.busy}
                  onChange={e => c.setGroupEdit({ ...c.groupEdit, to: e.target.value })} />
                <button type="submit" className={primaryBtn} disabled={c.busy}>Lưu</button>
                <button type="button" className={outlineBtn} onClick={() => c.setGroupEdit(null)} disabled={c.busy}>Hủy</button>
              </form>
            ) : (
              <>
                <h4 className="text-sm font-semibold uppercase tracking-wide text-gray-500">
                  {group.name || 'Chưa phân nhóm'} <span className="font-normal normal-case">({group.items.length})</span>
                </h4>
                {group.name && <button type="button" className={outlineBtn} disabled={c.busy} onClick={() => c.startGroupEdit(group.name)}>Đổi tên nhóm</button>}
              </>
            )}
          </div>
          <ul className="divide-y divide-gray-100 rounded-xl border border-gray-200 dark:divide-white/5 dark:border-white/10">
            {group.items.map(x => (
              <li key={x.id} className="flex flex-col gap-3 p-4 sm:flex-row sm:items-start sm:justify-between">
                <div className="min-w-0">
                  <p className="font-medium text-gray-900 dark:text-white">{x.name}
                    <span className="ml-2 align-middle"><Badge size="sm" color={x.status === 'ACTIVE' ? 'success' : 'warning'}>{statusLabels[x.status] || x.status}</Badge></span>
                  </p>
                  <p className="mt-0.5 text-sm text-gray-500">{x.description || 'Chưa có mô tả'}</p>
                  <p className="mt-0.5 text-xs text-gray-400">{x.productCount} sản phẩm đang bán</p>
                </div>
                <div className="flex shrink-0 gap-2">
                  <button type="button" className={outlineBtn} disabled={c.busy} onClick={() => c.edit(x)}>Sửa</button>
                  <button type="button" className={dangerBtn} disabled={c.busy} onClick={() => c.removeCategory(x)}>Xóa</button>
                </div>
              </li>
            ))}
          </ul>
        </section>
      ))}
    </ComponentCard>
  )
}

function ModerationPanel({ c }) {
  return (
    <ComponentCard title="Kiểm duyệt sản phẩm toàn sàn" desc="Ẩn các sản phẩm vi phạm khỏi trang công khai hoặc bỏ ẩn khi đã xử lý.">
      <form onSubmit={c.search} className="flex flex-col gap-3 sm:flex-row sm:items-end">
        <label className="block flex-1 text-sm font-medium text-gray-700 dark:text-gray-300">Tìm theo tên sản phẩm
          <input maxLength={100} className={`${field} mt-1.5`} value={c.query} disabled={c.busy}
            onChange={e => c.setQuery(e.target.value)} placeholder="Nhập tên sản phẩm..." />
        </label>
        <button type="submit" className={primaryBtn} disabled={c.busy}>Tìm kiếm</button>
      </form>

      {c.products && <>
        <p className="text-sm text-gray-500">{c.products.totalElements} sản phẩm</p>
        {!c.products.content.length && <p className="text-sm text-gray-500">Không có sản phẩm phù hợp.</p>}
        <ul className="divide-y divide-gray-100 rounded-xl border border-gray-200 dark:divide-white/5 dark:border-white/10">
          {c.products.content.map(p => (
            <li key={p.id} className="flex flex-col gap-3 p-4 sm:flex-row sm:items-start sm:justify-between">
              <div className="min-w-0">
                <p className="font-medium text-gray-900 dark:text-white">{p.name}</p>
                <p className="text-sm text-gray-500">{p.shopName} · {p.categoryName} · {formatPrice(p.price)}</p>
                <p className="mt-1">
                  {p.adminHidden
                    ? <Badge size="sm" color="error">Bị quản trị viên ẩn</Badge>
                    : p.available
                      ? <Badge size="sm" color="success">Đang hiển thị công khai</Badge>
                      : <Badge size="sm" color="warning">Shop đang ẩn hoặc cửa hàng chưa hoạt động</Badge>}
                </p>
              </div>
              <button type="button" className={p.adminHidden ? outlineBtn : dangerBtn} disabled={c.busy} onClick={() => c.toggleHidden(p)}>
                {p.adminHidden ? 'Bỏ ẩn' : 'Ẩn vi phạm'}
              </button>
            </li>
          ))}
        </ul>
        <div className="flex items-center justify-between">
          <button type="button" className={outlineBtn} disabled={c.busy || c.products.page === 0} onClick={() => c.changePage(-1)}>Trang trước</button>
          <span className="text-sm text-gray-500">Trang {c.products.page + 1} / {Math.max(1, c.products.totalPages)}</span>
          <button type="button" className={outlineBtn} disabled={c.busy || c.products.page + 1 >= c.products.totalPages} onClick={() => c.changePage(1)}>Trang sau</button>
        </div>
      </>}
    </ComponentCard>
  )
}

export default function AdminCategoriesView() {
  const c = useAdminCategoriesController()
  return (
    <>
      <PageMeta title="Danh mục & kiểm duyệt | Admin" description="Quản lý danh mục và kiểm duyệt sản phẩm" />
      <PageBreadCrumb pageTitle="Danh mục & kiểm duyệt sản phẩm" />
      {c.error && <p role="alert" className="mb-4 rounded-lg bg-error-50 p-4 text-sm text-error-600 dark:bg-error-500/10 dark:text-error-400">{c.error}</p>}
      {c.notice && <p role="status" className="mb-4 rounded-lg bg-success-50 p-4 text-sm text-success-600 dark:bg-success-500/10">{c.notice}</p>}
      {c.busy && <p role="status" className="mb-4 text-sm text-brand-500">Đang tải…</p>}
      <div className="grid items-start gap-6 xl:grid-cols-2">
        <CategoryPanel c={c} />
        <ModerationPanel c={c} />
      </div>
    </>
  )
}
