import { formatPrice } from '../../models/productModel'

export default function OrderDiscountSummary({ order }) {
  if (order.status === 'AWAITING_QUOTE') return null
  return <dl className="my-4 space-y-1 rounded-lg bg-gray-50 p-3 text-sm dark:bg-white/5">
    <div className="flex justify-between gap-4"><dt>Tiền hàng</dt><dd>{formatPrice(order.subTotal)}</dd></div>
    <div className="flex justify-between gap-4"><dt>Phí GHN</dt><dd>{formatPrice(order.shippingFee)}</dd></div>
    {Number(order.discountAmount) > 0 && <div className="flex justify-between gap-4"><dt>Giảm tiền hàng {order.couponCode}</dt><dd>−{formatPrice(order.discountAmount)}</dd></div>}
    {Number(order.shippingDiscountAmount) > 0 && <div className="flex justify-between gap-4"><dt>Giảm phí ship {order.shippingCouponCode}</dt><dd>−{formatPrice(order.shippingDiscountAmount)}</dd></div>}
    <div className="flex justify-between gap-4 border-t border-gray-200 pt-2 font-semibold dark:border-gray-700"><dt>Tổng thanh toán</dt><dd>{formatPrice(order.totalAmount)}</dd></div>
  </dl>
}
