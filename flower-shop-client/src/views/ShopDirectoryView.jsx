import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { staffApplicationService } from '../services/staffApplicationService'
import FollowShopButton from '../components/FollowShopButton'
import '../styles/catalog.css'

export default function ShopDirectoryView({ auth }) {
  const [query, setQuery] = useState('')
  const [term, setTerm] = useState('')
  const [shops, setShops] = useState([])
  const [busy, setBusy] = useState(true)
  const [error, setError] = useState('')
  useEffect(() => {
    let active = true
    setBusy(true); setError('')
    staffApplicationService.search(term)
      .then(data => { if (active) setShops(data) })
      .catch(e => { if (active) { setError(e.message); setShops([]) } })
      .finally(() => { if (active) setBusy(false) })
    return () => { active = false }
  }, [term])
  return <main className="catalog-page">
    <header className="catalog-page-header">
      <h1>Khám phá cửa hàng</h1>
      {auth?.user?.role === 'CUSTOMER' && <Link to="/following-shops">Cửa hàng đang theo dõi</Link>}
    </header>
    <section className="catalog-panel">
      <p>Theo dõi cửa hàng bạn yêu thích để nhận email khi có sản phẩm mới.</p>
      <form onSubmit={e => { e.preventDefault(); setTerm(query.trim()) }} className="catalog-actions">
        <label>Tên cửa hàng <input value={query} maxLength={100} onChange={e => setQuery(e.target.value)} placeholder="Tìm cửa hàng hoa…" /></label>
        <button type="submit" disabled={busy}>Tìm kiếm</button>
      </form>
      {busy && <p role="status">Đang tải cửa hàng…</p>}
      {error && <p role="alert" className="message error">{error}</p>}
      {!busy && !error && !shops.length && <p>Không tìm thấy cửa hàng phù hợp.</p>}
      <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
        {shops.map(shop => <article key={shop.id} className="rounded-2xl border border-gray-200 p-5 dark:border-white/10">
          {shop.logoUrl && <img src={shop.logoUrl} alt={shop.name} className="mb-3 h-20 w-20 rounded-xl object-cover" />}
          <h2 className="text-xl font-semibold"><Link to={`/shops/${shop.id}`}>{shop.name}</Link></h2>
          <p className="my-3 text-gray-500">{shop.description || 'Cửa hàng chưa thêm mô tả.'}</p>
          <Link to={`/shops/${shop.id}`}>Xem cửa hàng và sản phẩm</Link>
          <FollowShopButton auth={auth} shopId={shop.id} />
        </article>)}
      </div>
      {shops.length === 50 && <p className="muted">Đang hiển thị tối đa 50 cửa hàng. Hãy tìm theo tên để thu hẹp kết quả.</p>}
    </section>
  </main>
}
