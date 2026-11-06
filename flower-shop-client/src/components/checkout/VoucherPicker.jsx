import { useEffect, useState } from 'react'
import { voucherService } from '../../services/voucherService'
import { formatPrice } from '../../models/productModel'

/** The server determines eligibility, combination restrictions and payable total. */
export default function VoucherPicker({ context, value, onChange, onPreview, disabled = false }) {
  const [data, setData] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [retry, setRetry] = useState(0)
  const request = context ? JSON.stringify({ ...context, couponCode: value.couponCode || '', shippingCouponCode: value.shippingCouponCode || '' }) : ''
  useEffect(() => {
    let active = true
    setData(null); setError(''); onPreview(null)
    if (!request) { setBusy(false); return }
    setBusy(true)
    const timer = setTimeout(() => voucherService.preview(JSON.parse(request))
      .then(result => { if (active) { setData(result); onPreview(result) } })
      .catch(e => { if (active) setError(e.message) })
      .finally(() => { if (active) setBusy(false) }), 250)
    return () => { active = false; clearTimeout(timer) }
  // Request captures every input; onPreview is a stable state setter supplied by the parent.
  }, [request, retry, onPreview])

  const choose = (key, code) => onChange(key === 'shippingCouponCode'
    ? { couponCode: '', shippingCouponCode: code } // Money eligibility changes when the shipping slot changes.
    : { ...value, couponCode: code })
  const renderChoices = (items, key, title) => <fieldset disabled={disabled || busy} className="space-y-2">
    <legend className="mb-2 text-sm font-semibold">{title}</legend>
    <label className="flex items-center gap-2 text-sm"><input type="radio" name={`voucher-${key}`} checked={!value[key]} onChange={() => choose(key, '')} />Không dùng mã</label>
    {items.map(item => <label key={item.id} className={`flex items-start gap-3 rounded-lg border p-3 text-sm ${item.eligible ? 'cursor-pointer border-gray-200 dark:border-gray-700' : 'border-gray-100 bg-gray-50 text-gray-400 dark:border-gray-800 dark:bg-gray-900'}`}>
      <input className="mt-1" type="radio" name={`voucher-${key}`} disabled={!item.eligible} checked={value[key] === item.code} onChange={() => choose(key, item.code)} />
      <span className="min-w-0 flex-1"><span className="font-semibold">{item.code}</span><span className="ml-2 text-xs">{item.scope === 'SHOP' ? 'Của shop' : 'Toàn sàn'}{item.target === 'PRODUCT' ? ' · Sản phẩm' : ''}</span>
        <span className="mt-1 block">Giảm {item.discountType === 'PERCENTAGE' ? `${item.discountValue}%` : formatPrice(item.discountValue)}, tối đa {formatPrice(item.maxDiscountValue)} · Đơn từ {formatPrice(item.minOrderValue)}</span>
        {item.eligible ? <span className="mt-1 block font-medium text-brand-600">Tiết kiệm {formatPrice(item.saving)}</span> : <span className="mt-1 block" role="status">{item.reason}</span>}
      </span>
    </label>)}
    {!items.length && <p className="text-sm text-gray-500">Chưa có mã giảm giá.</p>}
  </fieldset>
  return <section className="space-y-4 rounded-xl border border-gray-200 p-4 dark:border-gray-700" aria-label="Chọn voucher">
    <h3 className="font-semibold">Ưu đãi cho đơn hàng</h3>
    {/* <p className="text-xs text-gray-500">Tối đa 1 mã tiền hàng + 1 mã phí ship. Khi dùng mã ship, chỉ dùng được mã tiền hàng có số tiền giảm thực tế thấp nhất trong các mã đủ điều kiện.</p> */}
    {!context && <p className="text-sm text-gray-500">Chọn sản phẩm và địa chỉ nhận hàng để xem ưu đãi.</p>}
    {busy && <p role="status" className="text-sm">Đang kiểm tra voucher…</p>}
    {error && <div role="alert" className="space-y-2 text-sm text-error-500"><p>{error}</p><button type="button" onClick={() => setRetry(v => v + 1)}>Thử lại</button> · <button type="button" onClick={() => onChange({ couponCode: '', shippingCouponCode: '' })}>Bỏ mã đã chọn</button></div>}
    {data && <>
      {data.bestSaving > 0 && <div className="rounded-lg bg-brand-50 p-3 text-sm dark:bg-brand-500/10"><p>Gợi ý tiết kiệm nhất: <strong>{[data.recommendedOrderCode, data.recommendedShippingCode].filter(Boolean).join(' + ')}</strong> — giảm {formatPrice(data.bestSaving)}</p>
        <button type="button" disabled={disabled || busy} className="mt-2 font-semibold text-brand-600" onClick={() => onChange({ couponCode: data.recommendedOrderCode || '', shippingCouponCode: data.recommendedShippingCode || '' })}>Dùng ưu đãi tốt nhất</button></div>}
      {renderChoices(data.shippingVouchers, 'shippingCouponCode', 'Giảm phí giao hàng')}
      {renderChoices(data.orderVouchers, 'couponCode', 'Giảm tiền hàng / sản phẩm')}
      <div className="space-y-1 border-t border-gray-200 pt-3 text-sm dark:border-gray-700" aria-live="polite">
        <p className="flex justify-between"><span>Giảm tiền hàng</span><strong>−{formatPrice(data.discountAmount)}</strong></p>
        <p className="flex justify-between"><span>Giảm phí ship</span><strong>−{formatPrice(data.shippingDiscountAmount)}</strong></p>
        <p className="flex justify-between"><span>Thanh toán sau ưu đãi</span><strong className="text-brand-600">{formatPrice(data.totalAmount)}</strong></p>
      </div>
    </>}
  </section>
}
