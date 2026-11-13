import { useCallback, useRef, useState } from 'react'
import { customerOrderService } from '../../services/customerOrderService'
import paymentApi from '../../services/paymentService'
import VoucherPicker from '../checkout/VoucherPicker'
import CustomPaymentSummary from './CustomPaymentSummary'

export default function CustomerQuoteActions({ order, user, onChanged }) {
  const [paymentType, setPaymentType] = useState('DEPOSIT')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [vouchers, setVouchers] = useState({ couponCode: '', shippingCouponCode: '' })
  const [preview, setPreview] = useState(null)
  const paying = useRef(false)
  const accepted = useRef(false)
  const changeVouchers = useCallback(value => { setPreview(null); setVouchers(value) }, [])
  if (order.orderType !== 'CUSTOM' || order.customDesign?.legacy || !['QUOTED', 'PENDING'].includes(order.status)) return null
  const quoted = order.status === 'QUOTED'
  async function proceed() {
    if (paying.current || (quoted && !preview)) return
    paying.current = true; setBusy(true); setError('')
    try {
      if (quoted && !accepted.current) {
        await customerOrderService.acceptQuote(order.id, 'ONLINE', preview.totalAmount, {
          ...vouchers, paymentType, expectedDeposit: paymentType === 'DEPOSIT' ? preview.depositAmount : null,
        })
        accepted.current = paymentType
      }
      await paymentApi.startSePayCheckout(order.id, quoted ? accepted.current || paymentType : 'FULL')
    } catch (e) {
      setError(e.message); paying.current = false; setBusy(false)
      if (accepted.current) await onChanged().catch(() => {})
    }
  }
  return <section className="my-5 rounded-2xl border border-brand-200 p-5 dark:border-gray-700" aria-label="Thanh toán báo giá custom">
    <h3 className="text-xl font-semibold">{quoted ? 'Xác nhận báo giá và thanh toán' : 'Tiếp tục thanh toán đơn thiết kế'}</h3>
    <p className="mb-6 mt-2 text-sm text-gray-500">Ưu đãi được áp cho đơn trước khi tính tiền cọc. Shop chuẩn bị hoa sau khi nhận thanh toán.</p>
    <div className="grid items-start gap-6 xl:grid-cols-[minmax(0,1fr)_340px]"><div className="space-y-5">
      {quoted && <><fieldset disabled={busy || !!accepted.current} className="space-y-3"><legend className="mb-3 text-sm font-semibold">Lựa chọn thanh toán qua SePay</legend>{[['DEPOSIT', 'Đặt cọc trước, trả phần còn lại khi nhận hoa'], ['FULL', 'Thanh toán toàn bộ đơn hàng']].map(([type, label]) => <label key={type} className={`flex cursor-pointer items-start gap-3 rounded-xl border p-4 text-sm ${paymentType === type ? 'border-brand-500 bg-brand-50/40 dark:bg-brand-500/10' : 'border-gray-200 dark:border-gray-700'}`}><input type="radio" name={`custom-payment-${order.id}`} checked={paymentType === type} onChange={() => setPaymentType(type)} /><span>{label}</span></label>)}</fieldset>
        <VoucherPicker context={{ mode: 'custom', orderId: order.id, revision: `${order.subTotal}:${order.shippingFee}` }} value={vouchers} onChange={changeVouchers} onPreview={setPreview} disabled={busy || !!accepted.current} showSummary={false} /></>}
      {!quoted && <p className="text-sm text-gray-500">Đơn đã chốt thanh toán toàn bộ. Giá và voucher đã được lưu; tiếp tục thanh toán qua SePay.</p>}
      {error && <p role="alert" className="text-sm text-error-500">{error}</p>}
    </div><CustomPaymentSummary preview={quoted ? preview : order} paymentType={quoted ? paymentType : 'FULL'} />
      <button type="button" disabled={busy || (quoted && !preview)} onClick={proceed} className="custom-flower-button inline-flex w-full items-center justify-center rounded-xl bg-brand-600 px-5 py-3 font-semibold text-white disabled:opacity-50 xl:col-span-2">{busy ? 'Đang mở SePay…' : quoted && paymentType === 'DEPOSIT' ? 'Đồng ý báo giá và thanh toán cọc' : 'Thanh toán toàn bộ qua SePay'}</button>
    </div>
  </section>
}
