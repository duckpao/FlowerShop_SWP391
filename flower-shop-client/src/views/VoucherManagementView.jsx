import { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { managerShopService } from '../services/managerShopService'
import { productService } from '../services/productService'
import { voucherService } from '../services/voucherService'
import { formatPrice } from '../models/productModel'
import '../styles/vouchers.css'

const empty = () => ({ code: '', target: 'ORDER', discountType: 'PERCENTAGE', discountValue: '', minOrderValue: '0', maxDiscountValue: '20000', startDate: '', endDate: '', usageLimit: '100', status: 'ACTIVE', productId: '' })
const targetLabel = { ORDER: 'Tiền đơn hàng', SHIPPING: 'Phí giao hàng', PRODUCT: 'Sản phẩm' }
export default function VoucherManagementView() {
  const { user } = useAuth()
  const admin = user?.role === 'ADMIN'
  const [shops, setShops] = useState([])
  const [shopId, setShopId] = useState('')
  const [items, setItems] = useState([])
  const [products, setProducts] = useState([])
  const [productQuery, setProductQuery] = useState('')
  const [form, setForm] = useState(empty)
  const [editing, setEditing] = useState(null)
  const [deleting, setDeleting] = useState(null)
  const [busy, setBusy] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [revision, setRevision] = useState(0)
  useEffect(() => {
    if (user?.role !== 'SHOP') return
    let active = true
    managerShopService.list('SHOP').then(list => { if (active) { setShops(list); setShopId(list[0]?.id || ''); if (!list.length) setLoading(false) } }).catch(e => { if (active) { setError(e.message); setLoading(false) } })
    return () => { active = false }
  }, [user?.id, user?.role])
  useEffect(() => {
    if (!admin && !shopId) return
    let active = true
    setLoading(true); setError(''); setEditing(null); setForm(empty())
    voucherService.list(admin ? null : shopId).then(list => { if (active) setItems(list) }).catch(e => { if (active) setError(e.message) }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [admin, shopId, revision])
  useEffect(() => {
    if (!admin || form.target !== 'PRODUCT') return
    let active = true
    const timer = setTimeout(() => productService.catalog(0, '', productQuery).then(result => { if (active) setProducts(result.content || []) }).catch(e => { if (active) setError(e.message) }), 250)
    return () => { active = false; clearTimeout(timer) }
  }, [admin, form.target, productQuery])
  const edit = (key, value) => setForm(f => ({ ...f, [key]: value, ...(key === 'target' ? { productId: '' } : {}) }))
  const scope = admin ? null : shopId
  async function save(e) {
    e.preventDefault(); setBusy(true); setError(''); setNotice('')
    try {
      await voucherService.save(scope, editing, { ...form, discountValue: Number(form.discountValue), minOrderValue: Number(form.minOrderValue), maxDiscountValue: Number(form.maxDiscountValue), usageLimit: Number(form.usageLimit), productId: form.target === 'PRODUCT' ? form.productId : null })
      setNotice(editing ? 'Đã cập nhật mã giảm giá.' : 'Đã tạo mã giảm giá.'); setRevision(v => v + 1)
    } catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  async function remove() {
    setBusy(true); setError('')
    try { await voucherService.remove(scope, deleting.id); setDeleting(null); setNotice('Đã xóa mã giảm giá.'); setRevision(v => v + 1) }
    catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  if (!['ADMIN', 'SHOP'].includes(user?.role)) return <p>Bạn không có quyền quản lý mã giảm giá.</p>
  return <main className="voucher-page">
    <header><p className="text-sm text-brand-600">{admin ? 'Ưu đãi toàn sàn' : 'Ưu đãi cửa hàng'}</p><h1 className="text-2xl font-bold">Quản lý mã giảm giá</h1><p className="mt-2 text-sm text-gray-500">{admin ? 'Quản lý mã tiền hàng, phí giao hàng và sản phẩm của sàn.' : 'Mã của bạn chỉ giảm tiền đơn hàng tại cửa hàng đang chọn.'} Trần giảm mỗi mã từ 20.000đ đến 70.000đ.</p></header>
    {!admin && <label>Cửa hàng<select value={shopId} disabled={busy} onChange={e => setShopId(e.target.value)}>{shops.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}</select></label>}
    {error && <p role="alert" className="message error">{error}</p>}{notice && <p role="status" className="message success">{notice}</p>}
    {!admin && !loading && !shopId ? <p>Chưa có cửa hàng để tạo voucher.</p> : <div className="voucher-layout">
      <section className="catalog-panel"><h2 className="mb-4 text-lg font-semibold">{editing ? 'Chỉnh sửa mã' : 'Tạo mã giảm giá'}</h2>
        <form onSubmit={save}><fieldset disabled={busy || loading}><div className="voucher-form-grid">
          <label>Mã giảm giá<input required minLength={3} maxLength={50} pattern="[A-Za-z0-9_-]{3,50}" value={form.code} onChange={e => edit('code', e.target.value.toUpperCase())} placeholder="Ví dụ: HOA20" /></label>
          <label>Áp dụng cho<select value={form.target} onChange={e => edit('target', e.target.value)}><option value="ORDER">Tiền đơn hàng</option>{admin && <><option value="SHIPPING">Phí giao hàng</option><option value="PRODUCT">Sản phẩm</option></>}</select></label>
          <label>Kiểu giảm<select value={form.discountType} onChange={e => edit('discountType', e.target.value)}><option value="PERCENTAGE">Phần trăm (%)</option><option value="FIXED_AMOUNT">Số tiền cố định (VND)</option></select></label>
          <label>{form.discountType === 'PERCENTAGE' ? 'Phần trăm giảm' : 'Số tiền giảm (VND)'}<input required type="number" min="0.01" step="0.01" max={form.discountType === 'PERCENTAGE' ? '100' : '9999999999.99'} value={form.discountValue} onChange={e => edit('discountValue', e.target.value)} /></label>
          <label>Giảm tối đa (VND)<input required type="number" min="20000" max="70000" step="1000" value={form.maxDiscountValue} onChange={e => edit('maxDiscountValue', e.target.value)} /></label>
          <label>Đơn tối thiểu (VND)<input required type="number" min="0" step="1000" value={form.minOrderValue} onChange={e => edit('minOrderValue', e.target.value)} /></label>
          <label>Ngày bắt đầu<input required type="datetime-local" value={form.startDate} onChange={e => edit('startDate', e.target.value)} /></label>
          <label>Ngày kết thúc<input required type="datetime-local" min={form.startDate} value={form.endDate} onChange={e => edit('endDate', e.target.value)} /></label>
          <label>Số lượt dùng tối đa<input required type="number" min="1" step="1" value={form.usageLimit} onChange={e => edit('usageLimit', e.target.value)} /></label>
          <label>Trạng thái<select value={form.status} onChange={e => edit('status', e.target.value)}><option value="ACTIVE">Đang bật</option><option value="DISABLED">Tạm ngừng</option><option value="EXPIRED">Hết hạn</option></select></label>
          {admin && form.target === 'PRODUCT' && <div className="space-y-2 sm:col-span-2"><label>Tìm sản phẩm<input value={productQuery} onChange={e => setProductQuery(e.target.value)} placeholder="Nhập tên hoa để tìm" /></label><label>Sản phẩm áp dụng<select required value={form.productId} onChange={e => edit('productId', e.target.value)}><option value="">Chọn sản phẩm</option>{form.productId && !products.some(p => p.id === form.productId) && <option value={form.productId}>Sản phẩm đang áp dụng</option>}{products.map(p => <option key={p.id} value={p.id}>{p.name} · {p.shopName}</option>)}</select></label></div>}
        </div><div className="mt-5 flex gap-3"><button className="rounded-lg bg-brand-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-brand-700" type="submit">{busy ? 'Đang lưu…' : editing ? 'Lưu thay đổi' : 'Tạo mã'}</button>{editing && <button type="button" onClick={() => { setEditing(null); setForm(empty()) }}>Hủy chỉnh sửa</button>}</div></fieldset></form>
      </section>
      <section className="catalog-panel"><h2 className="mb-4 text-lg font-semibold">Mã đã tạo ({items.length})</h2>{loading ? <p role="status">Đang tải…</p> : !items.length ? <p>Chưa có mã giảm giá.</p> : <div className="space-y-3">{items.map(item => {
        const ended = new Date(item.endDate) <= new Date()
        return <article key={item.id} className="rounded-xl border border-gray-200 p-4 dark:border-gray-700"><div className="flex flex-wrap items-center justify-between gap-2"><h3 className="font-semibold">{item.code}</h3><span className="text-xs text-gray-500">{item.status === 'DISABLED' ? 'Tạm ngừng' : ended || item.status === 'EXPIRED' ? 'Hết hạn' : new Date(item.startDate) > new Date() ? 'Sắp bắt đầu' : 'Đang bật'}</span></div>
          <p className="mt-2 text-sm">{targetLabel[item.target]} · Giảm {item.discountType === 'PERCENTAGE' ? `${item.discountValue}%` : formatPrice(item.discountValue)}, tối đa {formatPrice(item.maxDiscountValue)}</p><p className="mt-1 text-xs text-gray-500">Đơn từ {formatPrice(item.minOrderValue)} · Đã dùng {item.usedCount}/{item.usageLimit}</p><p className="mt-1 text-xs text-gray-500">{new Date(item.startDate).toLocaleString('vi-VN')} → {new Date(item.endDate).toLocaleString('vi-VN')}</p>
          <div className="mt-3 flex gap-4 text-sm"><button disabled={busy} className="text-brand-600" onClick={() => { setEditing(item.id); const fields = Object.fromEntries(Object.keys(empty()).map(key => [key, item[key] ?? ''])); setForm({ ...fields, startDate: item.startDate.slice(0,16), endDate: item.endDate.slice(0,16) }) }}>Chỉnh sửa</button><button disabled={busy} className="text-error-500" onClick={() => setDeleting(item)}>Xóa mã</button></div>
        </article>
      })}</div>}</section>
    </div>}
    {deleting && <div role="dialog" aria-modal="true" aria-label="Xóa mã giảm giá" className="fixed inset-0 z-99999 flex items-center justify-center bg-black/40 p-4"><section className="w-full max-w-md rounded-xl bg-white p-6 dark:bg-gray-900"><h2 className="text-lg font-semibold">Xóa mã {deleting.code}?</h2><p className="mt-2 text-sm text-gray-500">Mã sẽ ngừng áp dụng. Đơn hàng đã sử dụng vẫn giữ nguyên ưu đãi.</p><div className="mt-5 flex gap-4"><button disabled={busy} onClick={remove} className="rounded-lg bg-error-500 px-4 py-2 text-white">Xóa mã</button><button disabled={busy} onClick={() => setDeleting(null)}>Quay lại</button></div></section></div>}
  </main>
}
