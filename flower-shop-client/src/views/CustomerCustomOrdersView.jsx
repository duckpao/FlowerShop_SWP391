import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { customFlowerService } from '../services/customFlowerService'
import { accountService } from '../services/accountService'
import orderService from '../services/orderService'
import paymentService from '../services/paymentService'
import { CustomRequestCard, customButton } from '../components/customFlowers/CustomFlowerParts'
import '../styles/custom-flowers.css'

// Yêu cầu bị từ chối đã đóng nên không còn lối vào trao đổi
const ChatLink = ({ request }) => request.status === 'REJECTED' ? null
  : <Link className={`${customButton} mt-4 inline-flex`} to={`/chat?${new URLSearchParams({ shopId: request.shopId, customOrderId: request.id })}`}>Trao đổi với shop</Link>

// Khách xem báo giá, chọn địa chỉ (+ mã giảm giá tùy chọn) rồi chấp nhận: tạo đơn đặt cọc, sang SePay trả cọc. Hoặc từ chối.
// ACCEPTED: đơn cọc cũ đã hủy thì đặt lại cùng báo giá.
function QuoteActions({ request, userId, addresses, onChanged }) {
  const [addressId, setAddressId] = useState(''), [coupon, setCoupon] = useState(''), [reason, setReason] = useState('')
  const [busy, setBusy] = useState(false), [error, setError] = useState(''), [rejecting, setRejecting] = useState(false)
  const open = request.status === 'QUOTED' || (request.status === 'ACCEPTED' && request.quotedPrice != null)
  if (!open) return null
  // Hết hạn thì cả chấp nhận lần đầu lẫn đặt lại cọc đều bị chặn (backend kiểm cùng luật)
  const expired = request.quoteValidUntil && new Date(request.quoteValidUntil) <= new Date()
  const selected = addressId || addresses.find(a => a.isDefault)?.id || addresses[0]?.id || ''
  const accept = async () => {
    setBusy(true); setError('')
    try {
      const order = await orderService.makeCustomOrder(userId, request.id, { deliveryAddressId: selected, couponId: coupon.trim() || undefined })
      await paymentService.startSePayCheckout(order.id, 'DEPOSIT')
    } catch (e) {
      setError(e.response?.data?.message || e.message || 'Không thể đặt cọc. Vui lòng thử lại.')
      setBusy(false)
    }
  }
  const reject = async () => {
    setBusy(true); setError('')
    try { await customFlowerService.customerReject(request.id, reason.trim()); onChanged() }
    catch (e) { setError(e.message || 'Không thể từ chối báo giá.') } finally { setBusy(false) }
  }
  if (expired) return <p className="mt-4 text-sm text-gray-500">Báo giá đã hết hạn. Hãy nhắn shop để được báo giá lại.</p>
  if (!addresses.length) return <p className="mt-4 text-sm">Thêm địa chỉ nhận hoa trong <Link className="underline" to="/account">tài khoản</Link> để chấp nhận báo giá.</p>
  return <div className="mt-4 space-y-3 border-t border-gray-200 pt-4 dark:border-gray-700">
    <label className="block text-sm font-semibold" htmlFor={`deposit-address-${request.id}`}>Địa chỉ nhận hoa</label>
    <select id={`deposit-address-${request.id}`} value={selected} onChange={e => setAddressId(e.target.value)} disabled={busy} className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm dark:border-white/10 dark:bg-gray-800">
      {addresses.map(a => <option key={a.id} value={a.id}>{[a.addressLine, a.ward, a.district, a.city].filter(Boolean).join(', ')}</option>)}
    </select>
    <label className="block text-sm font-semibold" htmlFor={`deposit-coupon-${request.id}`}>Mã giảm giá (tùy chọn)</label>
    <input id={`deposit-coupon-${request.id}`} value={coupon} maxLength={50} onChange={e => setCoupon(e.target.value)} disabled={busy} className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm sm:max-w-xs dark:border-white/10 dark:bg-gray-800" placeholder="Mỗi người chỉ dùng 1 mã" />
    <p className="text-xs text-gray-500">Tổng = giá hoa (sau giảm giá) + phí giao hàng GHN tính theo địa chỉ. Cọc {request.depositPercent}% thanh toán online để shop bắt đầu làm hoa, phần còn lại thanh toán sau hoặc khi nhận hàng. Quá 24 giờ chưa cọc thì đơn tự hủy.</p>
    {error && <p role="alert" className="message error">{error} {error.includes('đã có đơn') && <Link className="underline" to="/orders">Xem đơn hàng</Link>}</p>}
    <div className="flex flex-wrap gap-2">
      <button type="button" className={customButton} disabled={busy || !selected} onClick={accept}>{busy ? 'Đang xử lý…' : request.status === 'QUOTED' ? 'Chấp nhận và đặt cọc' : 'Đặt cọc'}</button>
      {request.status === 'QUOTED' && <button type="button" className={customButton} disabled={busy} onClick={() => setRejecting(v => !v)}>Từ chối báo giá</button>}
    </div>
    {rejecting && request.status === 'QUOTED' && <div className="space-y-2">
      <label className="block text-sm font-semibold" htmlFor={`reject-${request.id}`}>Lý do từ chối (tùy chọn)</label>
      <textarea id={`reject-${request.id}`} rows={2} maxLength={2000} value={reason} onChange={e => setReason(e.target.value)} disabled={busy} className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm dark:border-white/10 dark:bg-gray-800" />
      <button type="button" className={customButton} disabled={busy} onClick={reject}>Xác nhận từ chối</button>
    </div>}
  </div>
}

export default function CustomerCustomOrdersView({ auth }) {
  const [params] = useSearchParams()
  const openId = params.get('open') || ''
  const [data, setData] = useState(null), [page, setPage] = useState(0)
  const [opened, setOpened] = useState(null), [openError, setOpenError] = useState('')
  const [busy, setBusy] = useState(false), [error, setError] = useState('')
  const [addresses, setAddresses] = useState([]), [revision, setRevision] = useState(0)
  const refresh = () => setRevision(v => v + 1)
  useEffect(() => {
    if (auth?.user?.role !== 'CUSTOMER') return
    let active = true
    accountService.addresses().then(v => { if (active) setAddresses(Array.isArray(v) ? v : []) }).catch(() => {})
    return () => { active = false }
  }, [auth?.user?.id, auth?.user?.role])
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
  }, [openId, auth?.user?.id, auth?.user?.role, revision])
  if (auth?.busy && !auth?.user) return <p role="status">Đang kiểm tra đăng nhập…</p>
  if (!auth?.user) return <main className="catalog-page"><p>Đăng nhập để xem yêu cầu của bạn.</p><Link to="/login?next=%2Fcustom-orders">Đăng nhập</Link></main>
  if (auth.user.role !== 'CUSTOMER') return <main className="catalog-page"><p>Chức năng này dành cho khách hàng.</p></main>
  return <main className="catalog-page custom-flower-page"><header className="catalog-page-header"><h1>Yêu cầu hoa riêng của tôi</h1><Link className={customButton} to="/custom-orders/new">Tạo yêu cầu mới</Link></header>
    <section className="catalog-panel space-y-5">
      {openError && <p role="alert" className="message error">{openError}</p>}
      {opened && <div aria-label="Yêu cầu đang xem" className="rounded-2xl ring-2 ring-brand-500"><CustomRequestCard request={opened}><ChatLink request={opened} /><QuoteActions request={opened} userId={auth.user.id} addresses={addresses} onChanged={refresh} /></CustomRequestCard></div>}
      {busy && <p role="status">Đang tải yêu cầu…</p>}{error && <p role="alert" className="message error">{error}</p>}
      {data?.totalElements === 0 && <p>Bạn chưa gửi yêu cầu nào. Hãy bắt đầu bằng ngân sách và loại hoa yêu thích.</p>}
      {data?.content.filter(r => r.id !== openId).map(r => <CustomRequestCard key={r.id} request={r}><ChatLink request={r} /><QuoteActions request={r} userId={auth.user.id} addresses={addresses} onChanged={refresh} /></CustomRequestCard>)}
      {data && data.totalPages > 1 && <div className="catalog-actions"><button disabled={busy || page === 0} onClick={() => setPage(v => v - 1)}>Trang trước</button><span>{page + 1}/{data.totalPages}</span><button disabled={busy || page + 1 >= data.totalPages} onClick={() => setPage(v => v + 1)}>Trang sau</button></div>}
    </section></main>
}
