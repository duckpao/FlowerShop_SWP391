import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { shopPlanService } from '../../services/shopPlanService'
import { formatPrice } from '../../models/productModel'
export default function FeaturedProducts() {
  const [items, setItems] = useState([])
  const [visible, setVisible] = useState(12)
  useEffect(() => {
    let active = true
    shopPlanService.featured().then(value => { if (active) setItems(value) }).catch(() => {})
    return () => { active = false }
  }, [])
  if (!items.length) return null
  return <section className="mb-8 rounded-2xl border border-brand-200 bg-brand-50/30 p-5 dark:border-gray-700 dark:bg-white/5" aria-label="Sản phẩm nổi bật">
    <h2 className="mb-4 text-xl font-semibold">Sản phẩm nổi bật</h2>
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">{items.slice(0, visible).map(product => <Link key={product.id} to={'/products/' + encodeURIComponent(product.id)} className="rounded-xl border border-gray-200 bg-white p-3 dark:border-gray-700 dark:bg-gray-900">
      {product.imageUrl && <img src={product.imageUrl} alt={product.name} className="mb-3 h-40 w-full rounded-lg object-cover" />}
      <p className="text-xs text-gray-500">{product.shopName}</p><h3 className="my-1 font-semibold">{product.name}</h3><p className="font-semibold text-brand-600">{formatPrice(product.price)}</p>
    </Link>)}</div>
    {visible < items.length && <button type="button" className="mt-4 rounded-lg border px-4 py-2 text-sm" onClick={() => setVisible(v => v + 12)}>Xem thêm sản phẩm nổi bật</button>}
  </section>
}
