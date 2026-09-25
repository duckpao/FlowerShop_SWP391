import DataTable from './DataTable'
import InfoPopup from './InfoPopup'
import DetailDialog from './DetailDialog'
import { useStaffApplicationsController } from '../controllers/useStaffApplicationsController'
import { useState } from 'react'
export default function StaffApplicationsView({ shop }) {
  const c = useStaffApplicationsController(shop.id)
  const [open, setOpen] = useState(new URLSearchParams(window.location.search).has('notifications'))
  const pending = c.items.filter(a => a.status === 'PENDING').length
  return <section><h3>Thông báo tuyển dụng</h3>
    <button aria-expanded={open} onClick={() => setOpen(!open)}>Thông báo đăng ký làm nhân viên ({pending} đơn chờ duyệt)</button>
    {c.error && <p role="alert">{c.error}</p>}
    {c.notice && <p role="status">{c.notice}</p>}
    {open && <DetailDialog title="Thông báo tuyển dụng" onClose={() => setOpen(false)}><button disabled={c.busy} onClick={c.reload}>Tải lại đơn</button>
    {!c.busy && !c.items.length && <p>Chưa có đơn đăng ký.</p>}
    <DataTable title="Đơn đăng ký nhân viên" rows={c.items} columns={[
 {key:'fullName',label:'Họ tên'},{key:'email',label:'Email'},{key:'phone',label:'Điện thoại'},
 {key:'status',label:'Trạng thái',render:a=>({PENDING:'Chờ duyệt',APPROVED:'Đã duyệt',REJECTED:'Đã từ chối'})[a.status]},
 {key:'introduction',label:'Chi tiết',render:a=><InfoPopup title="Đơn đăng ký nhân viên"><p>Họ tên: {a.fullName}</p><p>Email: {a.email}</p><p>Điện thoại: {a.phone}</p><p>Giới thiệu: {a.introduction || 'Chưa có giới thiệu'}</p></InfoPopup>},
 {key:'actions',label:'Thao tác',render:a=>a.status==='PENDING'?<><button disabled={c.busy||shop.status!=='ACTIVE'} onClick={()=>c.decide(a.id,true)}>Duyệt</button><button disabled={c.busy||shop.status!=='ACTIVE'} onClick={()=>c.decide(a.id,false)}>Từ chối</button></>:'?'}
 ]} /></DetailDialog>}</section>
}
