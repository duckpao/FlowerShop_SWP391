import { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { shopPlanService as api } from '../services/shopPlanService'
import { formatPrice } from '../models/productModel'
import { useConfirm } from '../components/ui/confirm/ConfirmProvider'
import { PencilLine, Plus, Save, Trash2, X } from 'lucide-react'

const empty = () => ({ name: '', durationMonths: 1, price: 59000, productLimit: 15, featuredLimit: 3, enabled: true })
const actionClass = 'inline-flex min-h-10 items-center justify-center gap-2 whitespace-nowrap rounded-lg border px-3.5 py-2 text-sm font-semibold shadow-sm transition focus-visible:outline-2 focus-visible:outline-offset-2 disabled:cursor-not-allowed disabled:opacity-40'
const primaryAction = `${actionClass} border-brand-600 bg-brand-600 text-white hover:bg-brand-700 focus-visible:outline-brand-500`
const secondaryAction = `${actionClass} border-gray-300 bg-white text-gray-700 hover:bg-gray-50 focus-visible:outline-gray-400 dark:border-gray-600 dark:bg-gray-800 dark:text-gray-200 dark:hover:bg-gray-700`
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
    <header className="flex flex-wrap items-start justify-between gap-4"><div className="max-w-3xl"><h1 className="text-2xl font-bold">Quản lý gói Pro</h1><p className="mt-2 text-sm text-gray-500">Giới hạn là tổng sản phẩm hiện có, gồm cả sản phẩm ẩn. Xóa sản phẩm sẽ giải phóng chỗ; sang tháng không đặt lại giới hạn.</p></div><button type="button" disabled={busy} className={primaryAction} onClick={() => { setEditing(null); setForm(empty()); document.getElementById('pro-plan-form')?.scrollIntoView({ behavior: 'smooth', block: 'start' }) }}><Plus size={17} aria-hidden="true" />Tạo gói Pro</button></header>
    {error && <p role="alert" className="rounded-lg bg-error-50 p-3 text-error-600">{error}</p>}
    {notice && <p role="status" className="text-success-600">{notice}</p>}
    <div className="overflow-x-auto rounded-xl border border-gray-200 dark:border-gray-700"><table className="w-full text-left text-sm"><thead><tr className="border-b"><th className="p-3">Gói</th><th>Giá</th><th>Thời hạn</th><th>Sản phẩm</th><th>Nổi bật</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
      <tbody>{items.map(p => <tr key={p.id} className={`border-b border-gray-100 transition dark:border-gray-800 ${editing === p.id ? 'bg-brand-50/60 dark:bg-brand-500/10' : 'hover:bg-gray-50 dark:hover:bg-white/5'}`}><td className="p-3 font-medium">{p.name}</td><td className="whitespace-nowrap px-2">{formatPrice(p.price)}</td><td className="whitespace-nowrap px-2">{p.durationMonths ? `${p.durationMonths} tháng` : 'Mặc định'}</td><td className="px-2">{p.productLimit ?? 'Không giới hạn'}</td><td className="px-2">{p.featuredLimit}</td><td className="px-2"><span className={`inline-flex whitespace-nowrap rounded-full px-2.5 py-1 text-xs font-semibold ${p.enabled ? 'bg-success-50 text-success-700 dark:bg-success-500/10 dark:text-success-400' : 'bg-gray-100 text-gray-600 dark:bg-gray-800 dark:text-gray-400'}`}>{p.enabled ? 'Đang bán' : 'Ngừng bán'}</span></td><td>{p.id !== 'FREE' ? <div className="flex gap-2 p-3"><button type="button" disabled={busy} aria-label={`Sửa ${p.name}`} className={`${actionClass} border-blue-600 bg-blue-600 text-white hover:bg-blue-700 focus-visible:outline-blue-500`} onClick={() => { setEditing(p.id); setForm({ ...p, productLimit: p.productLimit ?? '' }); setNotice(''); document.getElementById('pro-plan-form')?.scrollIntoView({ behavior: 'smooth', block: 'start' }) }}><PencilLine size={16} aria-hidden="true" />{p.enabled ? 'Sửa gói' : 'Sửa / bật lại'}</button><button type="button" disabled={busy || !p.enabled} aria-label={`Ngừng bán ${p.name}`} title="Ngừng bán gói; giữ thời hạn của shop đã mua" className={`${actionClass} border-error-200 bg-error-50 text-error-700 hover:border-error-400 hover:bg-error-100 focus-visible:outline-error-500 dark:border-error-500/30 dark:bg-error-500/10 dark:text-error-400 dark:hover:bg-error-500/20`} onClick={() => remove(p)}><Trash2 size={16} aria-hidden="true" />Ngừng bán</button></div> : <span className="px-3 text-xs text-gray-400">Gói mặc định</span>}</td></tr>)}</tbody>
    </table></div>
    <form id="pro-plan-form" onSubmit={save} className="scroll-mt-6 space-y-4 rounded-xl border border-gray-200 p-5 dark:border-gray-700">
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
      <div className="flex flex-wrap gap-3 border-t border-gray-100 pt-4 dark:border-gray-800"><button type="submit" disabled={busy} className={primaryAction}><Save size={17} aria-hidden="true" />{busy ? 'Đang lưu…' : editing ? 'Lưu thay đổi' : 'Tạo gói Pro'}</button><button type="button" disabled={busy} className={secondaryAction} onClick={() => { setEditing(null); setForm(empty()) }}>{editing ? <X size={17} aria-hidden="true" /> : <Plus size={17} aria-hidden="true" />}{editing ? 'Hủy chỉnh sửa' : 'Đặt lại biểu mẫu'}</button></div>
    </form>
  </main>
}
