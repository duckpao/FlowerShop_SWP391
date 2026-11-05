import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { customFlowerService } from '../services/customFlowerService'
import { CustomRequestCard, customButton } from '../components/customFlowers/CustomFlowerParts'
import '../styles/custom-flowers.css'

// Yêu cầu bị từ chối đã đóng nên không còn lối vào trao đổi
const ChatLink = ({ request }) => request.status === 'REJECTED' ? null
  : <Link className={`${customButton} mt-4 inline-flex`} to={`/chat?${new URLSearchParams({ shopId: request.shopId, customOrderId: request.id })}`}>Trao đổi với shop</Link>

export default function CustomerCustomOrdersView({ auth }) {
  const [params] = useSearchParams()
  const openId = params.get('open') || ''
  const [data, setData] = useState(null), [page, setPage] = useState(0), [revision, setRevision] = useState(0)
  const [opened, setOpened] = useState(null), [openError, setOpenError] = useState('')
  const [busy, setBusy] = useState(false), [error, setError] = useState('')
  useEffect(() => {
    if (auth?.user?.role !== 'CUSTOMER') return
    let active = true; setBusy(true); setError('')
    customFlowerService.mine(page).then(v => { if (active) setData(v) }).catch(e => { if (active) setError(e.message) }).finally(() => { if (active) setBusy(false) })
    return () => { active = false }
  }, [auth?.user?.id, auth?.user?.role, page, revision])
  // Thẻ chat dẫn tới ?open=: tải riêng yêu cầu đó để hiện ngay dù nó nằm ở trang khác của danh sách
  useEffect(() => {
    setOpened(null); setOpenError('')
    if (!openId || auth?.user?.role !== 'CUSTOMER') return
    let active = true
    customFlowerService.detail(openId).then(v => { if (active) setOpened(v) }).catch(e => { if (active) setOpenError(e.message) })
    return () => { active = false }
  }, [openId, auth?.user?.id, auth?.user?.role])
  if (auth?.busy && !auth?.user) return <p role="status">Đang kiểm tra đăng nhập…</p>
  if (!auth?.user) return <main className="catalog-page"><p>Đăng nhập để xem yêu cầu của bạn.</p><Link to="/login?next=%2Fcustom-orders">Đăng nhập</Link></main>
  if (auth.user.role !== 'CUSTOMER') return <main className="catalog-page"><p>Chức năng này dành cho khách hàng.</p></main>
  return <main className="catalog-page custom-flower-page"><header className="catalog-page-header"><h1>Yêu cầu hoa riêng của tôi</h1><Link className={customButton} to="/custom-orders/new">Tạo yêu cầu mới</Link></header>
    <section className="catalog-panel space-y-5">
      {openError && <p role="alert" className="message error">{openError}</p>}
      {opened && <div aria-label="Yêu cầu đang xem" className="rounded-2xl ring-2 ring-brand-500"><CustomRequestCard request={opened}><ChatLink request={opened} /></CustomRequestCard></div>}
      {busy && <p role="status">Đang tải yêu cầu…</p>}{error && <p role="alert" className="message error">{error}</p>}
      {data?.totalElements === 0 && <p>Bạn chưa gửi yêu cầu nào. Hãy bắt đầu bằng ngân sách và loại hoa yêu thích.</p>}
      {data?.content.filter(r => r.id !== openId).map(r => <CustomRequestCard key={r.id} request={r}><ChatLink request={r} /></CustomRequestCard>)}
      {data && data.totalPages > 1 && <div className="catalog-actions"><button disabled={busy || page === 0} onClick={() => setPage(v => v - 1)}>Trang trước</button><span>{page + 1}/{data.totalPages}</span><button disabled={busy || page + 1 >= data.totalPages} onClick={() => setPage(v => v + 1)}>Trang sau</button></div>}
    </section></main>
}
