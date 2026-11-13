import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { customFlowerService } from '../services/customFlowerService'
import { CustomRequestCard, customButton, customStatus } from '../components/customFlowers/CustomFlowerParts'
import '../styles/custom-flowers.css'

function DecisionForm({ request, busy, submit }) {
  const [status, setStatus] = useState('NEGOTIATING'), [note, setNote] = useState(''), [price, setPrice] = useState(''), [deposit, setDeposit] = useState('')
  const rejecting = status === 'REJECTED'
  return <form className="mt-4 space-y-3 border-t border-gray-200 pt-4 dark:border-gray-700" onSubmit={e => { e.preventDefault(); submit(request.id, { status, note, quotedPrice: rejecting || price === '' ? null : Number(price), depositPercent: rejecting || deposit === '' ? null : Number(deposit) }) }}>
    <fieldset disabled={busy} className="grid gap-3 md:grid-cols-2"><label>Hướng xử lý<select value={status} onChange={e => { setStatus(e.target.value); if (e.target.value === 'REJECTED') { setPrice(''); setDeposit('') } }}>{['NEGOTIATING', 'ACCEPTED', 'REJECTED'].map(s => <option value={s} key={s}>{s === 'REJECTED' ? 'Từ chối và hủy yêu cầu' : customStatus[s]}</option>)}</select></label>
      {!rejecting && <><label>Báo giá tổng (VND)<input type="number" min="1" max="9999999999.99" step="0.01" required={status === 'ACCEPTED'} value={price} onChange={e => setPrice(e.target.value)} /></label>
      <label>Mức cọc (%)<input type="number" min="10" max="100" step="1" required={status === 'ACCEPTED'} value={deposit} onChange={e => setDeposit(e.target.value)} placeholder="10–100" /></label></>}
      {rejecting && <p className="md:col-span-2 text-sm text-error-600">Yêu cầu sẽ được hủy. Không gửi báo giá hoặc yêu cầu đặt cọc cho khách.</p>}
      <label className="md:col-span-2">Phản hồi cho khách (không bắt buộc)<textarea maxLength={2000} rows={3} value={note} onChange={e => setNote(e.target.value)} placeholder="Có thể để trống hoặc bổ sung góp ý về thiết kế…" /></label>
      <button className={customButton}>{rejecting ? 'Từ chối và hủy yêu cầu' : 'Cập nhật trạng thái'}</button></fieldset>
  </form>
}
// Yêu cầu bị từ chối đã đóng nên không còn lối vào trao đổi
const ChatLink = ({ request }) => request.status === 'REJECTED' ? null
  : <Link className={`${customButton} mt-4 inline-flex`} to={`/shop-admin/chat?customOrderId=${encodeURIComponent(request.id)}`}>Trao đổi với khách</Link>

export default function ManagerCustomOrdersView({ user, shop }) {
  const [params] = useSearchParams(), openId = params.get('open') || ''
  const [opened, setOpened] = useState(null), [openError, setOpenError] = useState('')
  const shopId = shop?.id || '', [status, setStatus] = useState('')
  const [data, setData] = useState(null), [page, setPage] = useState(0), [revision, setRevision] = useState(0)
  const [busy, setBusy] = useState(false), [saving, setSaving] = useState(false), [error, setError] = useState(''), [notice, setNotice] = useState('')
  useEffect(() => {
    if (!shopId) return
    let active = true; setBusy(true); setError(''); setData(null)
    customFlowerService.shopRequests(shopId, page, status).then(v => { if (active) setData(v) }).catch(e => { if (active) setError(e.message) }).finally(() => { if (active) setBusy(false) })
    return () => { active = false }
  }, [shopId, page, status, revision])
  // Thẻ chat dẫn tới ?open=: tải riêng yêu cầu đó để hiện ngay dù nó nằm ở trang khác hoặc khác bộ lọc
  useEffect(() => {
    setOpened(null); setOpenError('')
    if (!openId || !shopId) return
    let active = true
    customFlowerService.shopDetail(shopId, openId).then(v => { if (active) setOpened(v) }).catch(e => { if (active) setOpenError(e.message) })
    return () => { active = false }
  }, [openId, shopId, revision])
  async function decide(id, body) {
    if (saving) return
    setSaving(true); setError(''); setNotice('')
    try { await customFlowerService.decide(shopId, id, body); setNotice('Đã cập nhật trạng thái yêu cầu của khách.'); setRevision(v => v + 1) }
    catch (e) { setError(e.message) } finally { setSaving(false) }
  }
  if (user?.role !== 'SHOP') return <p>Chỉ chủ shop được xử lý yêu cầu thiết kế hoa.</p>
  return <main className="catalog-page custom-flower-page"><header className="catalog-page-header"><h1>Yêu cầu đặt hoa riêng</h1></header><section className="catalog-panel space-y-5">
    <div className="grid items-end gap-4 md:grid-cols-[minmax(0,1fr)_minmax(0,1fr)_auto]"><label>Cửa hàng<input readOnly value={shop?.name || ''} /></label>
      <label>Trạng thái<select disabled={saving} value={status} onChange={e => { setStatus(e.target.value); setPage(0) }}><option value="">Tất cả</option>{Object.entries(customStatus).map(([key, label]) => <option key={key} value={key}>{label}</option>)}</select></label>
      <button type="button" className="custom-reload-button" disabled={busy || saving || !shopId} onClick={() => setRevision(v => v + 1)}><svg aria-hidden="true" className={busy ? 'animate-spin' : ''} width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="M20 7v5h-5"/><path d="M4 17v-5h5"/><path d="M6.1 8a7 7 0 0 1 11.6-2L20 9M4 15l2.3 3A7 7 0 0 0 17.9 16"/></svg>{busy ? 'Đang tải…' : 'Tải lại'}</button></div>
    {error && <p role="alert" className="message error">{error}</p>}{notice && <p role="status" className="message success">{notice}</p>}{busy && <p role="status">Đang tải yêu cầu…</p>}
    {!shop && !error && <p>Chưa có cửa hàng để xử lý yêu cầu.</p>}{data?.totalElements === 0 && <p>Chưa có yêu cầu ở trạng thái này.</p>}
    {openError && <p role="alert" className="message error">{openError}</p>}
    {opened && <div aria-label="Yêu cầu đang xem" className="rounded-2xl ring-2 ring-brand-500"><CustomRequestCard request={opened}><ChatLink request={opened} />{['REQUESTED', 'NEGOTIATING'].includes(opened.status) && <DecisionForm request={opened} busy={saving} submit={decide} />}</CustomRequestCard></div>}
    {data?.content.filter(r => r.id !== openId).map(r => <CustomRequestCard key={r.id} request={r}><ChatLink request={r} />{['REQUESTED', 'NEGOTIATING'].includes(r.status) && <DecisionForm request={r} busy={saving} submit={decide} />}</CustomRequestCard>)}
    {data && data.totalPages > 1 && <div className="catalog-actions"><button disabled={busy || saving || page === 0} onClick={() => setPage(v => v - 1)}>Trang trước</button><span>{page + 1}/{data.totalPages}</span><button disabled={busy || saving || page + 1 >= data.totalPages} onClick={() => setPage(v => v + 1)}>Trang sau</button></div>}
  </section></main>
}
