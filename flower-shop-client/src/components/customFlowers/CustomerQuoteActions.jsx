import { useCallback, useRef, useState } from 'react'
import { customerOrderService } from '../../services/customerOrderService'
import paymentApi from '../../services/paymentService'
import VoucherPicker from '../checkout/VoucherPicker'
import CustomPaymentSummary from './CustomPaymentSummary'
import CustomPaymentOptions from './CustomPaymentOptions'

export default function CustomerQuoteActions({ order, user, onChanged }) {
  const [busy, setBusy] = useState(false)
  const [paymentType, setPaymentType] = useState('DEPOSIT')
  const [error, setError] = useState('')
  const [vouchers, setVouchers] = useState({ couponCode: '', shippingCouponCode: '' })
  const [preview, setPreview] = useState(null)
  const paying = useRef(false)
  const accepted = useRef(false)
  const changeVouchers = useCallback(value => { setPreview(null); setVouchers(value) }, [])
  if (order.orderType !== 'CUSTOM' || order.customDesign?.legacy || order.status !== 'QUOTED') return null
  async function proceed() {
    if (paying.current || !preview) return
    paying.current = true; setBusy(true); setError('')
    try {
      if (!accepted.current) {
        await customerOrderService.acceptQuote(order.id, 'ONLINE', preview.totalAmount, {
          ...vouchers, paymentType, expectedDeposit: paymentType === 'DEPOSIT' ? preview.depositAmount : null,
        })
        accepted.current = paymentType
      }
      await paymentApi.startSePayCheckout(order.id, accepted.current || paymentType)
    } catch (e) {
      setError(e.message); paying.current = false; setBusy(false)
      if (accepted.current) await onChanged().catch(() => {})
    }
  }
  return <section className="my-5 rounded-2xl border border-brand-200 p-5 dark:border-gray-700" aria-label="Thanh toán báo giá custom">
    <h3 className="text-xl font-semibold">Xác nhận báo giá và thanh toán</h3>
    <p className="mb-6 mt-2 text-sm text-gray-500">Ưu đãi được áp cho đơn trước khi tính tiền cọc. Shop chuẩn bị hoa sau khi nhận thanh toán.</p>
    <div className="grid items-start gap-6 xl:grid-cols-[minmax(0,1fr)_340px]"><div className="space-y-5">
      <CustomPaymentOptions value={paymentType} onChange={setPaymentType} disabled={busy || !!accepted.current} name={`quote-payment-${order.id}`} />
      {error && <p role="alert" className="text-sm text-error-500">{error}</p>}
    </div><CustomPaymentSummary preview={preview} paymentType={paymentType}>
      <VoucherPicker context={{ mode: 'custom', orderId: order.id, revision: `${order.subTotal}:${order.shippingFee}` }} value={vouchers} onChange={changeVouchers} onPreview={setPreview} disabled={busy || !!accepted.current} showSummary={false} />
    </CustomPaymentSummary>
      <button type="button" disabled={busy || !preview} onClick={proceed} className="custom-flower-button inline-flex w-full items-center justify-center rounded-xl bg-brand-600 px-5 py-3 font-semibold text-white disabled:opacity-50 xl:col-span-2">{busy ? 'Đang mở SePay…' : paymentType === 'DEPOSIT' ? 'Đồng ý báo giá và thanh toán cọc' : 'Thanh toán toàn bộ qua SePay'}</button>
    </div>
  </section>
}
