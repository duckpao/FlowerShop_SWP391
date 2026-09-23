import { useAdminManagerApplicationsController } from '../controllers/useAdminManagerApplicationsController'
import ApplicationDetailDialog from './ApplicationDetailDialog'
const labels = { PENDING: 'Chờ duyệt', APPROVED: 'Đã duyệt', REJECTED: 'Đã từ chối' }
export default function AdminManagerApplicationsView() {
  const c = useAdminManagerApplicationsController()
  const selected = c.data?.content.find(a => a.id === c.selectedId)
  return <section className="account-card"><h2>Đơn đăng ký Manager / mở shop</h2>
    <label>Trạng thái<select disabled={c.busy} value={c.status} onChange={e => c.filter(e.target.value)}><option value="PENDING">Chờ duyệt</option><option value="APPROVED">Đã duyệt</option><option value="REJECTED">Đã từ chối</option></select></label>
    <button disabled={c.busy} onClick={c.reload}>Tải lại đơn</button>{c.error && <p role="alert">{c.error}</p>}{c.notice && <p role="status">{c.notice}</p>}
    {c.busy && <p role="status">Đang tải hoặc xử lý đơn…</p>}
    {!c.busy && !c.error && c.data?.content.length === 0 && <p>Không có đơn trong trạng thái đã chọn. Bấm “Tải lại đơn” sau khi Customer gửi đơn mới.</p>}
    {c.data && <><p><strong>{c.data.totalElements} đơn · {labels[c.status]}</strong></p><ul className="address-list">{c.data.content.map(a => <li key={a.id}>
      <div className="account-header"><div><h3>{a.shopName}</h3><p>Người đăng ký: {a.fullName}</p><p>Ngày gửi: {new Date(a.submittedAt).toLocaleString('vi-VN')} · <span className="address-badge">{labels[a.status]}</span></p></div>
        <button disabled={c.busy} aria-haspopup="dialog" onClick={() => c.setSelectedId(a.id)}>Xem chi tiết</button>
      </div>
    </li>)}</ul><button disabled={c.busy || c.data.page === 0} onClick={() => c.setPage(x => x - 1)}>Trang trước</button><button disabled={c.busy || c.data.page + 1 >= c.data.totalPages} onClick={() => c.setPage(x => x + 1)}>Trang sau</button></>}
    {selected && <ApplicationDetailDialog key={selected.id} application={selected} controller={c} />}
  </section>
}
