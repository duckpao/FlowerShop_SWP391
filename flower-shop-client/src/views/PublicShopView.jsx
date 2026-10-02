import { useEffect, useState } from 'react'
import { useParams } from 'react-router'
import { staffApplicationService } from '../services/staffApplicationService'
import ProductsView from './ProductsView'
import '../styles/catalog.css'

export default function PublicShopView() {
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
      <a href="/#shops">Danh sách cửa hàng</a>
    </header>

    {busy && <p role="status">Đang tải cửa hàng…</p>}
    {error && <div role="alert">
      <p className="message error">{error}</p>
      <a href="/#shops">Quay lại danh sách cửa hàng</a>
    </div>}

    {shop && <>
      <section className="catalog-panel">
        <h1>{shop.name}</h1>
        <p>{shop.description || 'Cửa hàng chưa thêm mô tả.'}</p>
      </section>
      {/* Không truyền manage: ProductsView gọi API công khai chỉ lấy sản phẩm của shop.id này. */}
      <ProductsView key={shop.id} shop={shop} />
    </>}
  </main>
}