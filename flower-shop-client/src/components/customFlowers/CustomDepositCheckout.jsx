import { useCallback, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import orderService from '../../services/orderService'
import paymentService from '../../services/paymentService'
import VoucherPicker from '../checkout/VoucherPicker'
import CustomPaymentSummary from './CustomPaymentSummary'
import CustomOrderPayment from './CustomOrderPayment'
import CustomPaymentOptions from './CustomPaymentOptions'
import { customButton } from './CustomFlowerParts'

export default function CustomDepositCheckout({ request, userId, addresses }) {
  const [addressId, setAddressId] = useState('')
  const [paymentType, setPaymentType] = useState('DEPOSIT')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [vouchers, setVouchers] = useState({ couponCode: '', shippingCouponCode: '' })
  const [preview, setPreview] = useState(null)
  const [createdOrderId, setCreatedOrderId] = useState(null)
  const paying = useRef(false)
  const changeVouchers = useCallback(value => { setPreview(null); setVouchers(value) }, [])
  if (request.status !== 'ACCEPTED' || request.design?.quotedPrice == null) return null
  const orderId = request.orderId || createdOrderId
  if (orderId) return <>{error && <p role="alert" className="message error">{error}</p>}{busy ? <p role="status" className="mt-4">Đơn đã được tạo, đang mở cổng thanh toán… <Link className="text-brand-600 underline" to={`/orders/${orderId}`}>Xem đơn hàng</Link></p> : <CustomOrderPayment orderId={orderId} />}</>
  const selected = addressId || addresses.find(a => a.isDefault)?.id || addresses[0]?.id || ''
  const address = addresses.find(a => a.id === selected)
  async function deposit() {
    if (paying.current || !preview || !selected) return
    paying.current = true; setBusy(true); setError('')
    try {
      const order = await orderService.makeCustomOrder(userId, request.id, {
        deliveryAddressId: selected, recipientName: address?.recipientName, phone: address?.recipientPhone,
        ...vouchers, paymentType, expectedTotal: preview.totalAmount, expectedDeposit: paymentType === 'DEPOSIT' ? preview.depositAmount : null,
      })
      setCreatedOrderId(order.id)
      await paymentService.startSePayCheckout(order.id, paymentType)
    } catch (e) { setError(e.message || 'Không thể mở thanh toán. Bạn có thể tiếp tục từ đơn đã tạo.'); paying.current = false; setBusy(false) }
  }
  return <section className="mt-6 border-t border-gray-200 pt-6 dark:border-gray-700" aria-label="Đặt cọc thiết kế">
    <h3 className="text-xl font-semibold">Xác nhận thiết kế và thanh toán</h3><p className="mb-6 mt-2 text-sm text-gray-500">Chọn địa chỉ, cách thanh toán và ưu đãi trước khi chốt đơn.</p>
    {!addresses.length ? <p>Thêm địa chỉ nhận hoa trong <Link className="font-semibold text-brand-600" to="/account">tài khoản của bạn</Link> để tiếp tục.</p> : <div className="grid items-start gap-6 xl:grid-cols-[minmax(0,1fr)_340px]">
      <div className="space-y-5"><div><label className="mb-2 block text-sm font-semibold" htmlFor={`deposit-address-${request.id}`}>Địa chỉ nhận hoa</label><select id={`deposit-address-${request.id}`} value={selected} onChange={e => { setPreview(null); setAddressId(e.target.value) }} disabled={busy} className="min-h-11 w-full rounded-lg border border-gray-300 px-3 py-2 text-sm dark:border-gray-700 dark:bg-gray-900">{addresses.map(a => <option key={a.id} value={a.id}>{[a.recipientName, a.addressLine, a.ward, a.district, a.city].filter(Boolean).join(', ')}</option>)}</select>{address?.recipientPhone && <p className="mt-2 text-sm text-gray-500">Số điện thoại nhận hoa: {address.recipientPhone}</p>}</div>
        <CustomPaymentOptions value={paymentType} onChange={setPaymentType} disabled={busy} name={`request-payment-${request.id}`} />
        {error && <p role="alert" className="message error">{error}</p>}
      </div><CustomPaymentSummary preview={preview} paymentType={paymentType}>
        <VoucherPicker context={selected ? { mode: 'deposit', userId, requestId: request.id, deliveryAddressId: selected, revision: `${request.design.quotedPrice}:${request.design.depositPercent}` } : null} value={vouchers} onChange={changeVouchers} onPreview={setPreview} disabled={busy} showSummary={false} />
      </CustomPaymentSummary>
      <button type="button" className={`${customButton} inline-flex w-full items-center justify-center rounded-xl bg-brand-600 px-5 py-3 font-semibold text-white disabled:opacity-50 xl:col-span-2`} disabled={busy || !selected || !preview} onClick={deposit}>{busy ? 'Đang mở thanh toán…' : paymentType === 'DEPOSIT' ? 'Đồng ý báo giá và thanh toán cọc' : 'Thanh toán toàn bộ qua SePay'}</button>
    </div>}
  </section>
}
