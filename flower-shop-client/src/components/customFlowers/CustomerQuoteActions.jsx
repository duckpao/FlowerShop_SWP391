import { useState } from 'react'
import { customerOrderService } from '../../services/customerOrderService'
import paymentApi from '../../services/paymentService'
import VoucherPicker from '../checkout/VoucherPicker'

export default function CustomerQuoteActions({ order, user, onChanged }) {
  const [method, setMethod] = useState('COD')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [vouchers, setVouchers] = useState({ couponCode: '', shippingCouponCode: '' })
  const [voucherPreview, setVoucherPreview] = useState(null)
  if (order.orderType !== 'CUSTOM' || order.customDesign?.legacy || !['QUOTED', 'PENDING'].includes(order.status)) return null
  async function proceed() {
    setBusy(true); setError('')
    try {
      const online = order.status === 'PENDING' || method === 'ONLINE'
      if (order.status === 'QUOTED') {
        await customerOrderService.acceptQuote(order.id, method, voucherPreview.totalAmount, vouchers)
        await onChanged()
      }
      if (online) {
        const result = await paymentApi.createPayment({ orderId: order.id, paymentType: 'FULL', userId: user.id })
        const data = result?.data || result
        if (!data.checkoutUrl || !data.fields) throw new Error('Không tải được cổng thanh toán. Bạn có thể thử lại tại đơn hàng này.')
        paymentApi.submitSePayCheckoutForm(data.checkoutUrl, data.fields)
      }
    } catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  return <section className="my-4 space-y-3 rounded-xl border border-brand-200 p-4 dark:border-gray-700">
    <h3 className="font-semibold">{order.status === 'QUOTED' ? 'Đồng ý thiết kế & báo giá' : 'Chờ thanh toán trực tuyến'}</h3>
    {order.status === 'QUOTED' ? <><p className="text-sm text-gray-500">Xem giá hoa và phí giao hàng ở bên dưới trước khi đồng ý. Shop bắt đầu chuẩn bị sau khi bạn chọn COD hoặc thanh toán trực tuyến thành công.</p><label className="block text-sm">Phương thức thanh toán<select className="ml-3 rounded-lg border p-2 dark:bg-gray-900" disabled={busy} value={method} onChange={e => setMethod(e.target.value)}><option value="COD">Tiền mặt khi nhận hàng (COD)</option><option value="ONLINE">Thanh toán trực tuyến qua SePay</option></select></label></> : <p className="text-sm text-gray-500">Vui lòng hoàn tất thanh toán trong 1 phút kể từ khi đồng ý báo giá.</p>}
    {error && <p role="alert" className="text-sm text-error-500">{error}</p>}
    {order.status === 'QUOTED' && <VoucherPicker context={{ mode: 'custom', orderId: order.id, revision: `${order.subTotal}:${order.shippingFee}` }} value={vouchers} onChange={setVouchers} onPreview={setVoucherPreview} disabled={busy} />}
    <button disabled={busy || (order.status === 'QUOTED' && !voucherPreview)} onClick={proceed} className="rounded-lg bg-brand-600 px-4 py-2 text-sm text-white">{busy ? 'Đang xử lý…' : order.status === 'QUOTED' ? 'Đồng ý báo giá & tiếp tục' : 'Tiếp tục thanh toán'}</button>
  </section>
}
