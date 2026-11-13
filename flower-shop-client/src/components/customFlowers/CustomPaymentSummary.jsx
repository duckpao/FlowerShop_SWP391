import { formatPrice } from '../../models/productModel'

export default function CustomPaymentSummary({ preview, paymentType = 'DEPOSIT', children }) {
  const deposit = paymentType === 'DEPOSIT'
  const row = (label, amount, discount = false) => <div className="flex justify-between gap-4"><dt>{label}</dt><dd className="shrink-0 font-medium">{discount ? '−' : ''}{formatPrice(amount)}</dd></div>
  return <aside className="rounded-2xl border border-brand-100 bg-brand-50/40 p-5 dark:border-gray-700 dark:bg-gray-900" aria-label="Tóm tắt thanh toán custom" aria-live="polite">
    <h3 className="mb-5 text-lg font-semibold">Tóm tắt thanh toán</h3>
    {children && <div className="mb-5">{children}</div>}
    {!preview ? <p className="text-sm text-gray-500">Chọn địa chỉ và chờ kiểm tra ưu đãi để xem số tiền chính xác.</p> : <>
      <dl className="space-y-3 text-sm text-gray-600 dark:text-gray-300">
        {row('Giá hoa shop báo', preview.subTotal)}
        {row('Giảm tiền hoa', preview.discountAmount, true)}
        {row('Phí giao hàng', preview.shippingFee)}
        {row('Giảm phí giao hàng', preview.shippingDiscountAmount, true)}
        <div className="flex justify-between gap-4 border-t border-gray-200 pt-4 font-semibold text-gray-900 dark:border-gray-700 dark:text-white"><dt>Tổng đơn sau ưu đãi</dt><dd>{formatPrice(preview.totalAmount)}</dd></div>
      </dl>
      <div className="mt-5 border-t border-brand-200 pt-5 dark:border-gray-700"><p className="text-sm font-medium">{deposit ? 'Tiền cọc thanh toán ngay' : 'Thanh toán toàn bộ'}</p><p className="mt-2 text-3xl font-bold text-brand-600">{formatPrice(deposit ? preview.depositAmount : preview.totalAmount)}</p>
        {deposit && <><p className="mt-2 text-xs leading-relaxed text-gray-500">{preview.depositPercent != null ? `${preview.depositPercent}% tiền hoa sau giảm giá. Phí giao hàng tính vào phần còn lại.` : 'Cọc 50% tổng đơn sau ưu đãi theo báo giá này.'}</p><div className="mt-5 flex justify-between gap-3 text-sm"><span>Còn lại khi nhận hoa</span><strong>{formatPrice(preview.remainingAmount)}</strong></div></>}
      </div>
    </>}
  </aside>
}
