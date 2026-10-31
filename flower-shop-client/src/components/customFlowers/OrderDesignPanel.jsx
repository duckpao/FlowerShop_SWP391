import { useState } from 'react'
import { managerOrderService } from '../../services/managerOrderService'
import { formatPrice } from '../../models/productModel'

export function OrderDesignPanel({ order }) {
  const design = order.customDesign
  if (!design) return null
  const p = design.preferences || {}
  return <section className="my-4 rounded-xl border border-brand-200 bg-brand-50/50 p-4 dark:border-gray-700 dark:bg-white/5">
    <h3 className="mb-3 font-semibold">Hoa theo yêu cầu · {p.quantity} bó/mẫu</h3>
    <dl className="grid gap-3 text-sm sm:grid-cols-2">{[
      ['Ngân sách hoa', formatPrice(design.budget)], ['Loại hoa', p.flowers], ['Tông màu', p.colors],
      ['Dịp tặng', p.occasion], ['Giấy gói / phụ kiện', p.wrapping], ['Ngày nhận mong muốn', p.deliveryDate],
      ['Mẫu tham khảo', p.referenceProductName], ['Mô tả', design.description], ['Phản hồi của shop', order.shopNote],
    ].filter(([, value]) => value).map(([label, value]) => <div key={label}><dt className="text-gray-500">{label}</dt><dd className="whitespace-pre-wrap">{value}</dd></div>)}</dl>
    {order.status === 'AWAITING_QUOTE' && <p className="mt-3 text-sm text-gray-500">Chưa chốt giá. Phí giao hàng sẽ được tính cùng báo giá của shop.</p>}
    {order.cancelReason && <p className="mt-3 text-sm text-error-500">Lý do từ chối / hủy: {order.cancelReason}</p>}
  </section>
}

export function ShopCustomOrderActions({ order, shopId, onChanged }) {
  const [amount, setAmount] = useState(order.subTotal > 0 ? String(order.subTotal) : '')
  const [note, setNote] = useState(order.shopNote || '')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  if (!order.customDesign || !['AWAITING_QUOTE', 'QUOTED'].includes(order.status)) return null
  async function act(reject) {
    setBusy(true); setError('')
    try {
      if (reject) {
        if (!note.trim()) throw new Error('Vui lòng nhập lý do từ chối trong phần phản hồi.')
        await managerOrderService.cancel(shopId, order.id, note.trim())
      } else await managerOrderService.quote(shopId, order.id, { amount: Number(amount), note })
      await onChanged()
    } catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  return <form className="my-4 space-y-3 rounded-xl border border-gray-200 p-4 dark:border-gray-700" onSubmit={e => { e.preventDefault(); act(false) }}>
    <h3 className="font-semibold">Xác nhận thiết kế & báo giá</h3>
    <p className="text-sm text-gray-500">Báo giá tiền hoa cho toàn bộ số bó/mẫu. Hệ thống cộng phí GHN; khách cần đồng ý và chọn thanh toán trước khi chuẩn bị.</p>
    <label className="block text-sm">Giá hoa trọn bộ (VND)<input className="mt-1 block h-11 w-full rounded-lg border px-3 dark:bg-gray-900" required type="number" min="1" max={order.customDesign.budget} step="0.01" value={amount} onChange={e => setAmount(e.target.value)} disabled={busy} /></label>
    <label className="block text-sm">Phản hồi / lý do từ chối<textarea className="mt-1 block w-full rounded-lg border p-3 dark:bg-gray-900" maxLength={2000} value={note} onChange={e => setNote(e.target.value)} disabled={busy} /></label>
    {error && <p role="alert" className="text-sm text-error-500">{error}</p>}
    <div className="flex flex-wrap gap-3"><button disabled={busy} className="rounded-lg bg-brand-600 px-4 py-2 text-sm text-white">{busy ? 'Đang xử lý…' : 'Xác nhận & gửi báo giá'}</button><button type="button" disabled={busy} onClick={() => act(true)} className="rounded-lg border border-error-300 px-4 py-2 text-sm text-error-500">Từ chối yêu cầu</button></div>
  </form>
}
