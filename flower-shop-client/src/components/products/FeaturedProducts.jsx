import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { shopPlanService } from '../../services/shopPlanService'
import { formatPrice, ratingLabel } from '../../models/productModel'

const PAGE_SIZE = 8

/** The public API checks Pro expiry, plan limits and product availability. */
export default function FeaturedProducts() {
  const [items, setItems] = useState([])
  const [visible, setVisible] = useState(PAGE_SIZE)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [revision, setRevision] = useState(0)
  useEffect(() => {
    let active = true
    setLoading(true)
    setError('')
    shopPlanService.featured()
      .then(value => { if (active) { setItems(value); setVisible(PAGE_SIZE) } })
      .catch(() => { if (active) setError('Chưa tải được sản phẩm nổi bật. Vui lòng thử lại.') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [revision])
  return <section id="featured-products" className="overflow-hidden rounded-3xl border border-amber-200 bg-white shadow-sm dark:border-amber-500/20 dark:bg-gray-900" aria-label="Sản phẩm nổi bật" aria-busy={loading}>
    <div className="flex flex-col gap-4 border-b border-amber-100 bg-amber-50/70 px-5 py-6 sm:flex-row sm:items-center sm:justify-between sm:px-8 dark:border-amber-500/10 dark:bg-amber-500/5">
      <div>
        <span className="mb-3 inline-flex items-center gap-2 rounded-full border border-amber-200 bg-white px-3 py-1 text-xs font-semibold text-amber-800 dark:border-amber-500/30 dark:bg-amber-500/10 dark:text-amber-300"><span aria-hidden="true">★</span> BỘ SƯU TẬP TỪ SHOP PRO</span>
        <h2 className="text-2xl font-bold tracking-tight text-gray-900 sm:text-3xl dark:text-white">Sản phẩm nổi bật</h2>
        <p className="mt-2 text-sm leading-6 text-gray-600 dark:text-gray-400">Khám phá những mẫu hoa được các cửa hàng Pro giới thiệu.</p>
      </div>
      <Link to="/products" className="inline-flex shrink-0 items-center gap-2 self-start rounded-xl border border-amber-200 bg-white px-4 py-2.5 text-sm font-semibold text-gray-900 transition hover:border-amber-400 dark:border-amber-500/30 dark:bg-gray-900 dark:text-white">Khám phá tất cả <span aria-hidden="true">→</span></Link>
    </div>
    <div className="p-5 sm:p-8">
      {loading ? <><p role="status" className="sr-only">Đang tải sản phẩm nổi bật…</p><div aria-hidden="true" className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">{Array.from({ length: 4 }, (_, i) => <div key={i} className="animate-pulse rounded-2xl border border-gray-100 p-3 dark:border-gray-800"><div className="aspect-[4/3] rounded-xl bg-gray-100 dark:bg-gray-800" /><div className="mt-4 h-3 w-1/2 rounded bg-gray-100 dark:bg-gray-800" /><div className="mt-3 h-4 w-3/4 rounded bg-gray-100 dark:bg-gray-800" /><div className="mt-4 h-5 w-1/3 rounded bg-gray-100 dark:bg-gray-800" /></div>)}</div></>
        : error ? <div role="alert" className="rounded-2xl bg-gray-50 p-6 text-center dark:bg-white/5"><p className="text-sm text-gray-600 dark:text-gray-400">{error}</p><button type="button" onClick={() => setRevision(v => v + 1)} className="mt-3 rounded-lg border border-gray-300 px-4 py-2 text-sm font-semibold dark:border-gray-700 dark:text-white">Thử lại</button></div>
          : !items.length ? <div role="status" className="rounded-2xl border border-dashed border-amber-200 px-5 py-10 text-center dark:border-amber-500/20"><span aria-hidden="true" className="text-4xl text-amber-500">✿</span><p className="mt-3 font-semibold text-gray-900 dark:text-white">Những mẫu hoa nổi bật sẽ sớm có mặt</p><p className="mt-2 text-sm text-gray-500 dark:text-gray-400">Các cửa hàng đang cập nhật bộ sưu tập. Bạn có thể khám phá thêm trong danh mục sản phẩm.</p></div>
            : <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">{items.slice(0, visible).map(product => <article key={product.id} className="group flex min-w-0 flex-col overflow-hidden rounded-2xl border border-gray-200 bg-white transition duration-200 hover:-translate-y-1 hover:border-amber-300 hover:shadow-lg dark:border-gray-800 dark:bg-gray-900">
              <Link to={'/products/' + encodeURIComponent(product.id)} className="relative block aspect-[4/3] overflow-hidden bg-gray-50 dark:bg-gray-800" aria-label={`Xem ${product.name}`}>
                <span aria-hidden="true" className="absolute inset-0 flex items-center justify-center text-6xl text-amber-200 dark:text-amber-800">✿</span>
                {product.imageUrl && <img src={product.imageUrl} alt={product.name} loading="lazy" onError={e => { e.currentTarget.hidden = true }} className="relative h-full w-full object-cover transition duration-300 group-hover:scale-105" />}
                <span className="absolute left-3 top-3 inline-flex items-center gap-1 rounded-full bg-gray-900 px-2.5 py-1 text-xs font-semibold text-amber-200 shadow-sm"><span aria-hidden="true">★</span> Shop Pro</span>
              </Link>
              <div className="flex flex-1 flex-col p-4">
                <Link to={'/shops/' + encodeURIComponent(product.shopId)} className="truncate text-xs font-medium text-gray-500 hover:text-brand-600 dark:text-gray-400">{product.shopName}</Link>
                <h3 className="mt-2 line-clamp-2 font-semibold leading-6 text-gray-900 dark:text-white"><Link to={'/products/' + encodeURIComponent(product.id)} className="hover:text-brand-600">{product.name}</Link></h3>
                <p className="mt-2 text-xs text-gray-500 dark:text-gray-400">{ratingLabel(product)}</p>
                <div className="mt-auto flex items-center justify-between gap-3 pt-4"><p className="font-bold text-brand-600 dark:text-brand-400">{formatPrice(product.price)}</p><Link to={'/products/' + encodeURIComponent(product.id)} aria-label={`Xem chi tiết ${product.name}`} className="rounded-lg bg-brand-50 px-3 py-2 text-xs font-semibold text-brand-600 transition hover:bg-brand-100 dark:bg-brand-500/10 dark:text-brand-400">Xem hoa <span aria-hidden="true">↗</span></Link></div>
              </div>
            </article>)}</div>}
      {!loading && !error && visible < items.length && <div className="mt-7 text-center"><button type="button" className="rounded-xl border border-gray-300 px-5 py-3 text-sm font-semibold text-gray-900 transition hover:border-amber-400 dark:border-gray-700 dark:text-white" onClick={() => setVisible(v => v + PAGE_SIZE)}>Xem thêm sản phẩm nổi bật</button></div>}
    </div>
  </section>
}
