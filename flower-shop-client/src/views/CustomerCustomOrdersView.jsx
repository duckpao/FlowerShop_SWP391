import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { customFlowerService } from '../services/customFlowerService'
import { CustomRequestCard, customButton } from '../components/customFlowers/CustomFlowerParts'
import '../styles/custom-flowers.css'

export default function CustomerCustomOrdersView({ auth }) {
  const [data, setData] = useState(null), [page, setPage] = useState(0), [revision, setRevision] = useState(0)
  const [busy, setBusy] = useState(false), [error, setError] = useState('')
  useEffect(() => {
    if (auth?.user?.role !== 'CUSTOMER') return
    let active = true; setBusy(true); setError('')
    customFlowerService.mine(page).then(v => { if (active) setData(v) }).catch(e => { if (active) setError(e.message) }).finally(() => { if (active) setBusy(false) })
    return () => { active = false }
  }, [auth?.user?.id, auth?.user?.role, page, revision])
  if (auth?.busy && !auth?.user) return <p role="status">Đang kiểm tra đăng nhập…</p>
  if (!auth?.user) return <main className="catalog-page"><p>Đăng nhập để xem yêu cầu của bạn.</p><Link to="/login?next=%2Fcustom-orders">Đăng nhập</Link></main>
  if (auth.user.role !== 'CUSTOMER') return <main className="catalog-page"><p>Chức năng này dành cho khách hàng.</p></main>
  return <main className="catalog-page custom-flower-page"><header className="catalog-page-header"><h1>Yêu cầu hoa riêng của tôi</h1><Link className={customButton} to="/custom-orders/new">Tạo yêu cầu mới</Link></header>
    <section className="catalog-panel space-y-5">

      {busy && <p role="status">Đang tải yêu cầu…</p>}{error && <p role="alert" className="message error">{error}</p>}
      {data?.totalElements === 0 && <p>Bạn chưa gửi yêu cầu nào. Hãy bắt đầu bằng ngân sách và loại hoa yêu thích.</p>}
      {data?.content.map(r => <CustomRequestCard key={r.id} request={r} />)}
      {data && data.totalPages > 1 && <div className="catalog-actions"><button disabled={busy || page === 0} onClick={() => setPage(v => v - 1)}>Trang trước</button><span>{page + 1}/{data.totalPages}</span><button disabled={busy || page + 1 >= data.totalPages} onClick={() => setPage(v => v + 1)}>Trang sau</button></div>}
    </section></main>
}
