import { useEffect, useRef, useState } from 'react'
import { shopPlanService as api } from '../../services/shopPlanService'
import { useConfirm } from '../ui/confirm/ConfirmProvider'
import { formatPrice } from '../../models/productModel'

const date = value => value ? new Date(value).toLocaleString('vi-VN') : '—'
const sources = { ADMIN_GRANT: 'Admin cấp trực tiếp', ADMIN_CONFIRM: 'Admin xác nhận giao dịch', SEPAY: 'SePay xác nhận' }
export default function AdminShopSubscriptionPanel({ shop }) {
  const { confirm } = useConfirm()
  const [data, setData] = useState(null)
  const [plans, setPlans] = useState([])
  const [products, setProducts] = useState([])
  const [planId, setPlanId] = useState('')
  const [note, setNote] = useState('')
  const [paymentNotes, setPaymentNotes] = useState({})
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const request = useRef(null)
  const allowed = shop.status === 'ACTIVE' && shop.owner?.status === 'ACTIVE' && shop.owner?.role === 'SHOP'
  useEffect(() => {
    let active = true
    Promise.all([api.adminStatus(shop.id), api.adminPlans(), api.adminProducts(shop.id)]).then(([value, items, productItems]) => {
      if (!active) return
      setData(value); setPlans(items.filter(p => p.durationMonths > 0 && p.enabled))
      setProducts(productItems)
    }).catch(e => { if (active) setError(e.message) })
    return () => { active = false }
  }, [shop.id])
  async function toggleProduct(product) {
    setBusy(true); setError('')
    try {
      await api.adminFeature(shop.id, product.id, !product.featured)
      const [value, items] = await Promise.all([api.adminStatus(shop.id), api.adminProducts(shop.id)])
      setData(value); setProducts(items)
    } catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  async function reload() {
    setBusy(true); setError('')
    try { setData(await api.adminStatus(shop.id)) } catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  async function grant(e) {
    e.preventDefault()
    const plan = plans.find(p => p.id === planId)
    if (!plan || !note.trim() || busy) return
    if (!await confirm({ title: 'Kích hoạt Pro', message: `Cấp/gia hạn ${plan.name} cho ${shop.name}? Thời gian còn lại sẽ được giữ.` })) return
    setBusy(true); setError(''); setNotice('')
    const fingerprint = JSON.stringify([shop.id, planId, note.trim()])
    if (request.current?.fingerprint !== fingerprint) request.current = { fingerprint, id: crypto.randomUUID() }
    try {
      setData(await api.adminGrant(shop.id, { planId, note: note.trim(), requestId: request.current.id }))
      request.current = null; setNote(''); setNotice('Đã kích hoạt/gia hạn Pro và thông báo cho chủ shop.')
    } catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  async function confirmPayment(payment) {
    const reason = (paymentNotes[payment.invoiceNumber] || '').trim()
    if (!reason || busy) return
    if (!await confirm({ title: 'Xác nhận giao dịch Pro', message: `Xác nhận ${payment.invoiceNumber}, số tiền ${formatPrice(payment.amount)}, gói ${payment.durationMonths} tháng của ${shop.name} và kích hoạt Pro?` })) return
    setBusy(true); setError(''); setNotice('')
    try {
      setData(await api.adminConfirm(shop.id, payment.invoiceNumber, reason))
      setNotice('Đã xác nhận giao dịch và kích hoạt/gia hạn Pro.')
    } catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  return <section className="mb-6 space-y-4 rounded-xl border border-brand-200 p-4 dark:border-gray-700">
    <div className="flex items-center justify-between gap-3"><h4 className="font-semibold">Gói Pro của cửa hàng</h4><button type="button" onClick={reload} disabled={busy} className="text-sm text-brand-600">Tải lại</button></div>
    {error && <p role="alert" className="text-sm text-error-600">{error}</p>}
    {notice && <p role="status" className="text-sm text-success-600">{notice}</p>}
    {data && <p className="text-sm">Gói hiện tại: <strong>{data.subscription.pro ? 'Pro' : 'Free'}</strong>{data.subscription.expiresAt && ` · Hạn Pro: ${date(data.subscription.expiresAt)}`}</p>}
    {!allowed && <p className="text-sm text-warning-600">Shop và tài khoản chủ shop phải đang hoạt động trước khi kích hoạt Pro.</p>}
    {data && <div className="space-y-2 border-t pt-3"><p className="text-sm">Sản phẩm: {data.subscription.productCount}/{data.subscription.productLimit ?? 'không giới hạn'} · Nổi bật: {data.subscription.featuredProductIds.length}/{data.subscription.featuredLimit}</p>
      <h5 className="text-sm font-semibold">Admin chọn sản phẩm nổi bật cho shop</h5>
      <div className="max-h-64 overflow-y-auto">{products.map(p => <label key={p.id} className="flex items-center gap-3 py-2 text-sm"><input type="checkbox" checked={p.featured} disabled={busy || !allowed || (!p.featured && (!p.eligible || !data.subscription.pro || data.subscription.featuredProductIds.length >= data.subscription.featuredLimit))} onChange={() => toggleProduct(p)} />{p.name}</label>)}</div>
    </div>}
    {!!data?.payments.length && <div className="space-y-3"><h5 className="text-sm font-semibold">Giao dịch cần xác nhận</h5>{data.payments.map(payment => <div key={payment.invoiceNumber} className="rounded-lg border border-gray-200 p-3 dark:border-gray-700">
      <p className="break-all text-sm font-medium">{payment.invoiceNumber}</p><p className="mt-1 text-sm">{payment.planName} · {formatPrice(payment.amount)} · {payment.status === 'REVIEW' ? 'Cần kiểm tra' : 'Chờ xác nhận'}</p>
      <label className="mt-2 block text-sm">Ghi chú xác nhận<input maxLength={1000} value={paymentNotes[payment.invoiceNumber] || ''} disabled={busy || !allowed} onChange={e => setPaymentNotes(v => ({ ...v, [payment.invoiceNumber]: e.target.value }))} className="mt-1 w-full rounded-lg border p-2 dark:bg-gray-900" /></label>
      <button type="button" disabled={busy || !allowed || !(paymentNotes[payment.invoiceNumber] || '').trim()} onClick={() => confirmPayment(payment)} className="mt-2 rounded-lg bg-brand-600 px-3 py-2 text-sm text-white disabled:opacity-50">Xác nhận & kích hoạt</button>
    </div>)}</div>}
    <form onSubmit={grant} className="space-y-3 border-t border-gray-200 pt-4 dark:border-gray-700">
      <h5 className="text-sm font-semibold">Cấp Pro trực tiếp</h5>
      <p className="text-xs text-gray-500">Giao dịch đang chờ được xác nhận ở phần trên. Phần này dùng để cấp thêm thời gian Pro, không ghi nhận một khoản thanh toán.</p>
      <label className="block text-sm">Gói Pro<select required disabled={busy || !allowed} value={planId} onChange={e => setPlanId(e.target.value)} className="mt-1 w-full rounded-lg border p-2 dark:bg-gray-900"><option value="">Chọn gói</option>{plans.map(plan => <option key={plan.id} value={plan.id}>{plan.name} · {plan.durationMonths} tháng</option>)}</select></label>
      <label className="block text-sm">Lý do / ghi chú<textarea required maxLength={1000} disabled={busy || !allowed} value={note} onChange={e => setNote(e.target.value)} className="mt-1 w-full rounded-lg border p-2 dark:bg-gray-900" /></label>
      <button disabled={busy || !allowed || !planId || !note.trim()} className="rounded-lg bg-brand-600 px-3 py-2 text-sm text-white disabled:opacity-50">{busy ? 'Đang xử lý…' : 'Xác nhận cấp Pro'}</button>
    </form>
    {!!data?.history.length && <div className="space-y-2 border-t border-gray-200 pt-3 dark:border-gray-700"><h5 className="text-sm font-semibold">Lịch sử kích hoạt</h5>{data.history.map(item => <article key={item.id} className="rounded-lg bg-gray-50 p-3 text-xs dark:bg-gray-900"><p className="font-semibold">{sources[item.source] || item.source} · {item.durationMonths} tháng</p><p>{item.actorName || 'Admin'} · {date(item.createdAt)}</p><p>Thời gian cộng: {date(item.startsAt)} → {date(item.endsAt)}</p><p className="mt-1 whitespace-pre-wrap">{item.note}</p></article>)}</div>}
  </section>
}
