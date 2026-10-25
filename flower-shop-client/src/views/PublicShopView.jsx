import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router'
import { staffApplicationService } from '../services/staffApplicationService'
import ProductsView from './ProductsView'
import FollowShopButton from '../components/FollowShopButton'
import '../styles/catalog.css'

export default function PublicShopView({ auth }) {
  const { id } = useParams()
  const [shop, setShop] = useState(null)
  const [busy, setBusy] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    setBusy(true)
    setError('')
    staffApplicationService.detail(id)
      .then(value => { if (active) setShop(value) })
      .catch(e => {
        if (!active) return
        setShop(null)
        setError(e.message)
      })
      .finally(() => { if (active) setBusy(false) })
    return () => { active = false }
  }, [id])

  return <main className="catalog-page">
    <header className="catalog-page-header">
      <a href="/shops">Danh sách cửa hàng</a>
      {auth?.user?.role === 'CUSTOMER' && <a href="/following-shops">Cửa hàng đang theo dõi</a>}
    </header>

    {busy && <p role="status">Đang tải cửa hàng…</p>}
    {error && <div role="alert">
      <p className="message error">{error}</p>
      <a href="/shops">Quay lại danh sách cửa hàng</a>
    </div>}

    {shop && <>
      <section className="catalog-panel">
        <h1>{shop.name}</h1>
        <div className="catalog-actions my-4">
          <Link className="rounded-lg bg-brand-500 px-4 py-3 text-white" to={`/custom-orders/new?shopId=${encodeURIComponent(shop.id)}`}>Đặt hoa theo yêu cầu</Link>
          <Link className="rounded-lg border px-4 py-3" to={`/budget-flowers?shopId=${encodeURIComponent(shop.id)}`}>Chọn hoa theo ngân sách</Link>
          <Link className="rounded-lg border px-4 py-3" to={`/floral-care?shopId=${encodeURIComponent(shop.id)}`}>Bài viết của shop</Link>
        </div>
        {shop.logoUrl && <img src={shop.logoUrl} alt={shop.name} className="my-4 h-24 w-24 rounded-xl object-cover" />}

        <FollowShopButton
          auth={auth}
          shopId={shop.id}
        />

        <p>{shop.description || 'Cửa hàng chưa thêm mô tả.'}</p>
      </section>
      <ProductsView key={shop.id} shop={shop} />
    </>}
  </main>
}
