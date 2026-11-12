import { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { shopPlanService as api } from '../services/shopPlanService'
import { formatPrice } from '../models/productModel'
import { useConfirm } from '../components/ui/confirm/ConfirmProvider'

const empty = () => ({ name: '', durationMonths: 1, price: 59000, productLimit: 15, featuredLimit: 3, enabled: true })
export default function AdminPlansView() {
  const { user } = useAuth()
  const { confirm } = useConfirm()
  const [items, setItems] = useState([])
  const [form, setForm] = useState(empty)
  const [editing, setEditing] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [revision, setRevision] = useState(0)
  useEffect(() => {
    if (user?.role !== 'ADMIN') return
    let active = true
    api.adminPlans().then(data => { if (active) setItems(data) }).catch(e => { if (active) setError(e.message) })
    return () => { active = false }
  }, [user?.role, revision])
  const change = (key, value) => setForm(previous => ({ ...previous, [key]: value }))
  async function save(e) {
    e.preventDefault()
    if (busy) return
    setBusy(true); setError(''); setNotice('')
    try {
      await api.savePlan(editing, { ...form, durationMonths: Number(form.durationMonths), price: Number(form.price), productLimit: form.productLimit === '' ? null : Number(form.productLimit), featuredLimit: Number(form.featuredLimit) })
      setEditing(null); setForm(empty()); setRevision(v => v + 1); setNotice('Đã lưu cấu hình gói.')
    } catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  async function remove(plan) {
    if (!await confirm({ title: 'Ngừng bán gói', message: `Ngừng bán ${plan.name}? Shop đã mua vẫn giữ thời hạn. Bạn có thể bật lại gói sau.` })) return
    setBusy(true); setError('')
    try { await api.removePlan(plan.id); setRevision(v => v + 1); if (editing === plan.id) { setEditing(null); setForm(empty()) } }
    catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  if (user?.role !== 'ADMIN') return <p>Chỉ admin được quản lý gói đăng ký.</p>
  const inputClass = 'mt-1 w-full rounded-lg border border-gray-300 p-2 dark:border-gray-700 dark:bg-gray-900'
  return <main className="mx-auto max-w-6xl space-y-6">
    <header><h1 className="text-2xl font-bold">Quản lý gói Pro</h1><p className="mt-2 text-sm text-gray-500">Giới hạn là tổng sản phẩm hiện có, gồm cả sản phẩm ẩn. Xóa sản phẩm sẽ giải phóng chỗ; sang tháng không đặt lại giới hạn.</p></header>
    {error && <p role="alert" className="rounded-lg bg-error-50 p-3 text-error-600">{error}</p>}
    {notice && <p role="status" className="text-success-600">{notice}</p>}
    <div className="overflow-x-auto rounded-xl border border-gray-200 dark:border-gray-700"><table className="w-full text-left text-sm"><thead><tr className="border-b"><th className="p-3">Gói</th><th>Giá</th><th>Thời hạn</th><th>Sản phẩm</th><th>Nổi bật</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
      <tbody>{items.map(p => <tr key={p.id} className="border-b"><td className="p-3 font-medium">{p.name}</td><td>{formatPrice(p.price)}</td><td>{p.durationMonths ? `${p.durationMonths} tháng` : 'Mặc định'}</td><td>{p.productLimit ?? 'Không giới hạn'}</td><td>{p.featuredLimit}</td><td>{p.enabled ? 'Đang bán' : 'Ngừng bán'}</td><td>{p.id !== 'FREE' && <div className="flex gap-3 p-3"><button disabled={busy} onClick={() => { setEditing(p.id); setForm({ ...p, productLimit: p.productLimit ?? '' }); setNotice('') }}>Sửa / bật lại</button><button disabled={busy || !p.enabled} onClick={() => remove(p)}>Xóa / ngừng bán</button></div>}</td></tr>)}</tbody>
    </table></div>
    <form onSubmit={save} className="space-y-4 rounded-xl border border-gray-200 p-5 dark:border-gray-700">
      <h2 className="font-semibold">{editing ? 'Sửa gói Pro' : 'Tạo gói Pro mới'}</h2>
      <p className="text-sm text-gray-500">Giá mới áp dụng cho lượt mua mới; số tiền hóa đơn đã tạo giữ nguyên. Thay đổi quyền lợi áp dụng cho cả shop đang dùng gói. Nếu giảm giới hạn, sản phẩm cũ vẫn được giữ và chỉ chặn tạo thêm; mục nổi bật chỉ hiển thị trong giới hạn mới.</p>
      <fieldset disabled={busy} className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <label>Tên gói<input required maxLength={100} className={inputClass} value={form.name} onChange={e => change('name', e.target.value)} /></label>
        <label>Thời hạn (tháng)<input required type="number" min="1" max="120" className={inputClass} value={form.durationMonths} onChange={e => change('durationMonths', e.target.value)} /></label>
        <label>Giá (đ)<input required type="number" min="1" step="1" className={inputClass} value={form.price} onChange={e => change('price', e.target.value)} /></label>
        <label>Tối đa sản phẩm<input type="number" min="1" placeholder="Để trống = không giới hạn" className={inputClass} value={form.productLimit} onChange={e => change('productLimit', e.target.value)} /></label>
        <label>Tối đa sản phẩm nổi bật<input required type="number" min="0" max="10000" className={inputClass} value={form.featuredLimit} onChange={e => change('featuredLimit', e.target.value)} /></label>
        <label className="flex items-center gap-2"><input type="checkbox" checked={form.enabled} onChange={e => change('enabled', e.target.checked)} />Cho phép mua / kích hoạt mới</label>
      </fieldset>
      <div className="flex gap-3"><button disabled={busy} className="rounded-lg bg-brand-600 px-4 py-2 text-white">{busy ? 'Đang lưu…' : 'Lưu gói'}</button><button type="button" disabled={busy} onClick={() => { setEditing(null); setForm(empty()) }}>Tạo gói khác</button></div>
    </form>
  </main>
}
