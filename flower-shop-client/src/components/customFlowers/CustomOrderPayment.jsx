import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import paymentService from '../../services/paymentService'
import { formatPrice } from '../../models/productModel'
import { customButton } from './CustomFlowerParts'

/** Resume the saved order; retrying a gateway never creates another order or consumes vouchers again. */
export default function CustomOrderPayment({ orderId }) {
  const [status, setStatus] = useState(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [revision, setRevision] = useState(0)
  const paying = useRef(false)
  useEffect(() => {
    let active = true
    setLoading(true); setError(''); setStatus(null)
    paymentService.checkPaymentStatus(orderId).then(value => { if (active) setStatus(value) })
      .catch(e => { if (active) setError(e.message) }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [orderId, revision])
  async function pay() {
    if (paying.current || !['DEPOSIT', 'FULL'].includes(status?.nextPaymentType)) return
    paying.current = true; setBusy(true); setError('')
    try { await paymentService.startSePayCheckout(orderId, status.nextPaymentType) }
    catch (e) { setError(e.message); paying.current = false; setBusy(false) }
  }
  const closed = ['CANCELLED', 'REFUNDED', 'COMPLETED'].includes(status?.orderStatus)
  return <section className="my-4 space-y-4 rounded-2xl border border-brand-200 p-5 dark:border-gray-700" aria-label="Thanh toán đơn thiết kế">
    <h3 className="text-lg font-semibold">Thanh toán đơn hoa theo yêu cầu</h3>
    {loading && <p role="status">Đang kiểm tra thanh toán…</p>}
    {status && <><dl className="grid gap-4 text-sm sm:grid-cols-3"><div><dt className="text-gray-500">Tổng đơn sau ưu đãi</dt><dd className="mt-1 font-semibold">{formatPrice(status.totalAmount)}</dd></div><div><dt className="text-gray-500">Đã thanh toán</dt><dd className="mt-1 font-semibold">{formatPrice(status.totalPaid)}</dd></div><div><dt className="text-gray-500">Còn lại</dt><dd className="mt-1 font-semibold">{formatPrice(status.remainingAmount)}</dd></div></dl>
      {status.nextPaymentType === 'DEPOSIT' && !closed && <p>Tiền cọc cần thanh toán: <strong className="text-brand-600">{formatPrice(status.depositAmount)}</strong>. Voucher đã được giữ trong đơn, bạn không cần chọn lại.</p>}
      {Number(status.remainingAmount) > 0 && Number(status.totalPaid) > 0 && !closed && <div role="status" className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm leading-6 text-amber-900 dark:border-amber-500/30 dark:bg-amber-500/10 dark:text-amber-200"><p className="font-semibold">Thanh toán phần còn lại khi nhận hoa (COD)</p><p>Bạn trả <strong>{formatPrice(status.remainingAmount)}</strong> cho nhân viên giao hàng khi nhận hoa; tiền cọc đã được trừ. Không cần thanh toán thêm trên hệ thống.</p></div>}
      {Number(status.remainingAmount) === 0 && !['CANCELLED', 'REFUNDED'].includes(status.orderStatus) && <p role="status" className="rounded-xl bg-success-50 p-4 text-sm font-semibold text-success-700 dark:bg-success-500/10 dark:text-success-400">Đơn đã được thanh toán đủ. Bạn không cần trả thêm khi nhận hoa.</p>}
    </>}
    {error && <div role="alert" className="space-y-2 text-sm text-error-500"><p>{error}</p><button type="button" disabled={busy || loading} onClick={() => setRevision(v => v + 1)} className="underline">Kiểm tra lại</button></div>}
    <div className="flex flex-wrap items-center gap-4">{['DEPOSIT', 'FULL'].includes(status?.nextPaymentType) && !closed && <button className={`${customButton} inline-flex items-center justify-center rounded-xl bg-brand-600 px-5 py-3 font-semibold text-white disabled:opacity-50`} type="button" disabled={busy || loading} onClick={pay}>{busy ? 'Đang mở SePay…' : status.nextPaymentType === 'FULL' ? `Thanh toán toàn bộ ${formatPrice(status.remainingAmount)}` : `Thanh toán cọc ${formatPrice(status.depositAmount)}`}</button>}<Link className="text-sm font-semibold text-brand-600" to={`/orders/${orderId}`}>Xem chi tiết đơn hàng</Link></div>
  </section>
}
