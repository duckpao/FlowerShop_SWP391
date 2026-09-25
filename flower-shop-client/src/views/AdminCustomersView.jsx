import DetailDialog from './DetailDialog'
import DataTable from './DataTable'
import { useAdminCustomersController } from '../controllers/useAdminCustomersController'

const labels = { ACTIVE: 'Đang hoạt động', BANNED: 'Đã khóa', INACTIVE: 'Ngừng hoạt động' }
export default function AdminCustomersView() {
  const c = useAdminCustomersController()
  return <section className="account-card"><h2>Quản lý khách hàng</h2>
    <form onSubmit={c.search}><fieldset disabled={c.busy}>
      <label>Tìm theo tên người dùng<input maxLength={100} placeholder="Nhập tên người dùng" value={c.query} onChange={e => c.setQuery(e.target.value)} /></label>
      <label>Trạng thái<select value={c.status} onChange={e => c.setStatus(e.target.value)}>
        <option value="">Tất cả</option>{Object.entries(labels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
      </select></label><button type="submit">Tìm kiếm</button>
    </fieldset></form>
    {c.busy && <p role="status">Đang tải…</p>}
    {c.error && <div role="alert"><p className="message error">{c.error}</p><button disabled={c.busy} onClick={c.retry}>Thử lại</button></div>}
    {c.notice && <p className="message success" role="status">{c.notice}</p>}
    {c.data && <><p>Tổng: {c.data.totalElements} khách hàng</p>
      {!c.data.content.length && <p>Không có khách hàng phù hợp.</p>}
      <DataTable title="Danh sách khách hàng" rows={c.data.content} columns={[
 {key:'fullName',label:'Họ tên'}, {key:'email',label:'Email'},
 {key:'status',label:'Trạng thái',render:x=>labels[x.status]},
 {key:'actions',label:'Thao tác',render:customer=><div className="address-actions"><button disabled={c.busy} aria-haspopup="dialog" onClick={()=>c.detail(customer.id)}>Xem chi tiết</button><button disabled={c.busy} onClick={()=>c.toggle(customer)}>{customer.status==='BANNED'?'Mở khóa':'Khóa tài khoản'}</button></div>}
 ]} />
      <div className="address-actions"><button disabled={c.busy || c.data.page === 0} onClick={() => c.next(-1)}>Trang trước</button>
        <span>Trang {c.data.page + 1} / {Math.max(1, c.data.totalPages)}</span>
        <button disabled={c.busy || c.data.page + 1 >= c.data.totalPages} onClick={() => c.next(1)}>Trang sau</button></div>
    </>}
    {c.selected && <DetailDialog title="Chi tiết khách hàng" onClose={() => c.setSelected(null)}>
      <p>Họ tên: {c.selected.fullName}</p><p>Email: {c.selected.email}</p><p>Điện thoại: {c.selected.phone || 'Chưa cập nhật'}</p>
      <p>Trạng thái: {labels[c.selected.status]}</p><p>Email: {c.selected.emailVerified ? 'Đã xác thực' : 'Chưa xác thực'}</p>
      <p>Ngày tạo: {c.selected.createdAt ? new Date(c.selected.createdAt).toLocaleString('vi-VN') : 'Chưa có'}</p>
      <button onClick={() => c.setSelected(null)}>Đóng chi tiết</button>
    </DetailDialog>}
  </section>
}
