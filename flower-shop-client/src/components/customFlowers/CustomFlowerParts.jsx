import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { customFlowerService } from '../../services/customFlowerService'
import { formatPrice } from '../../models/productModel'

export const customStatus = { REQUESTED: 'Chờ shop tiếp nhận', NEGOTIATING: 'Đang trao đổi', ACCEPTED: 'Shop đã chấp nhận', REJECTED: 'Đã hủy (shop từ chối)' }
export const customButton = 'custom-flower-button'

export function ReferenceSuggestions({ quantity, shopId, onChoose, selectedId = '' }) {
  const [data, setData] = useState(null)
  const [page, setPage] = useState(0)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  useEffect(() => { setPage(0) }, [quantity, shopId])
  useEffect(() => {
    let active = true
    setData(null); setError(''); setBusy(false)
    if (!shopId || !Number.isInteger(Number(quantity)) || Number(quantity) < 1 || Number(quantity) > 100) return
    setBusy(true)
    customFlowerService.suggestions({ quantity, shopId, page })
      .then(value => { if (active) setData(value) })
      .catch(e => { if (active) setError(e.message) })
      .finally(() => { if (active) setBusy(false) })
    return () => { active = false }
  }, [quantity, shopId, page])
  return <section className="space-y-4" aria-label="Mẫu hoa tham khảo">
    <div><h2 className="text-xl font-semibold">Chọn mẫu hoa tham khảo</h2><p className="text-sm text-gray-500">Bạn có thể chọn mẫu để shop hiểu thiết kế mong muốn. Giá catalog chỉ để tham khảo cho {quantity || 1} mẫu/bó; shop sẽ báo giá riêng sau khi nhận yêu cầu.</p></div>
    {busy && <p role="status">Đang tìm mẫu hoa…</p>}
    {error && <p role="alert" className="message error">{error}</p>}
    {data && !data.content.length && <div className="rounded-xl bg-gray-50 p-5 dark:bg-white/5">Chưa có mẫu còn hàng. Bạn vẫn có thể chọn loại hoa và mô tả thiết kế riêng để shop tư vấn.</div>}
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">{data?.content.map(({ product, estimatedTotal }) => <article key={product.id} className={`rounded-2xl border p-4 ${selectedId === product.id ? 'border-brand-500 ring-1 ring-brand-500' : 'border-gray-200 dark:border-gray-700'}`}>
      {product.imageUrl ? <img src={product.imageUrl} alt={product.name} className="mb-3 h-40 w-full rounded-xl object-cover" /> : <div className="mb-3 flex h-28 items-center justify-center rounded-xl bg-gray-50 text-gray-400 dark:bg-white/5">Chưa có ảnh mẫu</div>}
      <p className="text-sm text-gray-500">{product.shopName}</p><h3 className="font-semibold">{product.name}</h3>
      <p className="my-2 font-semibold text-brand-600">Giá catalog tham khảo: {formatPrice(estimatedTotal)}</p>
      {onChoose ? <button type="button" className={customButton} onClick={() => onChoose(product)}>{selectedId === product.id ? 'Đang chọn mẫu này' : 'Chọn làm mẫu tham khảo'}</button>
        : <Link className={customButton} to={`/custom-orders/new?${new URLSearchParams({ shopId: product.shopId, productId: product.id, quantity: String(quantity) })}`}>Đặt hoa theo mẫu này</Link>}
    </article>)}</div>
    {data && data.totalPages > 1 && <div className="catalog-actions"><button type="button" disabled={busy || page === 0} onClick={() => setPage(p => p - 1)}>Trang trước</button><span>{page + 1}/{data.totalPages}</span><button type="button" disabled={busy || page + 1 >= data.totalPages} onClick={() => setPage(p => p + 1)}>Trang sau</button></div>}
  </section>
}

export function CustomRequestCard({ request, children }) {
  const d = request.status === 'REJECTED' ? { ...request.design, quotedPrice: null, depositPercent: null } : request.design || {}
  return <article className="rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-700 dark:bg-white/3">
    <div className="mb-3 flex flex-wrap items-start justify-between gap-2"><div><h3 className="text-lg font-semibold">{request.shopName}</h3><p className="text-sm text-gray-500">{request.customerName} · {new Date(request.createdAt).toLocaleString('vi-VN')}</p></div><span className="rounded-full bg-brand-50 px-3 py-1 text-sm text-brand-700 dark:bg-brand-500/10 dark:text-brand-300">{customStatus[request.status] || request.status}</span></div>
    <p className="mb-3 whitespace-pre-wrap">{request.description}</p>
    <dl className="grid gap-2 text-sm sm:grid-cols-2">{request.status !== 'REJECTED' && <div><dt className="text-gray-500">Giá thiết kế</dt><dd className="font-semibold">{d.quotedPrice != null ? formatPrice(d.quotedPrice) : 'Chờ shop báo giá'}</dd></div>}<div><dt className="text-gray-500">Số mẫu/bó</dt><dd>{d.quantity || 1}</dd></div>
      {[["Loại hoa", d.flowers], ["Màu sắc", d.colors], ["Dịp tặng", d.occasion], ["Giấy gói / phụ kiện", d.wrapping], ["Ngày mong muốn", d.deliveryDate], ["Mẫu tham khảo", d.referenceProductName]].filter(([, value]) => value).map(([label, value]) => <div key={label}><dt className="text-gray-500">{label}</dt><dd>{value}</dd></div>)}
    </dl>
    {(d.shopNote || d.quotedPrice != null) && <div className="mt-4 border-t border-gray-200 pt-4 dark:border-gray-700"><p className="font-semibold">Phản hồi của shop</p>{d.shopNote && <p className="whitespace-pre-wrap">{d.shopNote}</p>}{d.quotedPrice != null && <p className="mt-2 font-semibold">Báo giá tổng: {formatPrice(d.quotedPrice)}</p>}{d.quotedPrice != null && d.depositPercent != null && <p className="mt-1">Cọc {d.depositPercent}%: {formatPrice(Math.round(d.quotedPrice * d.depositPercent / 100))}</p>}</div>}
    {request.status === 'ACCEPTED' && <p className="mt-3 text-sm text-gray-500">Shop đã nhận yêu cầu thiết kế. Đây chưa phải xác nhận thanh toán hoặc đơn giao hàng.</p>}
    {children}
  </article>
}
