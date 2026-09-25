import DataTable from './DataTable'
import InfoPopup from './InfoPopup'
import { useManagerShopController } from '../controllers/useManagerShopController'
import { shopStatusLabels } from '../models/shopModel'
import StaffApplicationsView from './StaffApplicationsView'
import ProductsView from './ProductsView'
export default function ManagerShopsView({ role, section = 'all' }) {
  const c = useManagerShopController(role)
  const editable = role === 'SHOP' && ['ACTIVE', 'PENDING'].includes(c.selected?.status)
  return <section className="account-card"><h2>Cửa hàng của tôi</h2>
    {c.error && <p role="alert" className="message error">{c.error}</p>}
    {c.notice && <p role="status" className="message success">{c.notice}</p>}
    {c.busy && <p>Đang tải…</p>}
    <button disabled={c.busy} onClick={c.retry}>Tải lại cửa hàng</button>
    {!c.busy && !c.shops.length && <p>Chưa có cửa hàng được phân công.</p>}
    {role !== 'SHOP' && <DataTable title="Cửa hàng được phân công" rows={c.shops} columns={[
 {key:'name',label:'Tên shop'},{key:'status',label:'Trạng thái',render:s=>shopStatusLabels[s.status]},
 {key:'actions',label:'Thao tác',render:s=><button disabled={c.busy} onClick={()=>c.select(s)}>Xem cửa hàng</button>}
 ]} />}
    {c.selected && <><h3>{c.selected.name}</h3><p>{shopStatusLabels[c.selected.status]}</p>
      <InfoPopup title="Thông tin cửa hàng"><p>{c.selected.name}</p><p>{c.selected.description || 'Chưa có mô tả'}</p><p>Trạng thái: {shopStatusLabels[c.selected.status]}</p><p>{[c.address.addressLine,c.address.ward,c.address.district,c.address.city].filter(Boolean).join(', ')}</p></InfoPopup>
      {role === 'SHOP' && ['all', 'staff'].includes(section) && <StaffApplicationsView key={c.selected.id} shop={c.selected} />}
      {role === 'SHOP' && ['all', 'products'].includes(section) && <ProductsView key={`products-${c.selected.id}`} shop={c.selected} manage />}
      {!editable && <p>Cửa hàng hiện ở chế độ chỉ xem đối với tài khoản này.</p>}
      {['all', 'shop'].includes(section) && <><form onSubmit={c.save}><fieldset disabled={c.busy || !editable}>
        <label>Tên shop<input required maxLength={255} value={c.form.name} onChange={e => c.setForm({ ...c.form, name: e.target.value })} /></label>
        <label>Mô tả<textarea maxLength={5000} value={c.form.description} onChange={e => c.setForm({ ...c.form, description: e.target.value })} /></label>
        <label>URL logo (HTTPS)<input type="url" maxLength={255} pattern="https://.*" value={c.form.logoUrl} onChange={e => c.setForm({ ...c.form, logoUrl: e.target.value })} /></label>
        {role === 'SHOP' && <button>Lưu hồ sơ cửa hàng</button>}
      </fieldset></form>
      {role === 'SHOP' && <><h3>Địa chỉ cửa hàng</h3><form onSubmit={c.saveAddress}><fieldset disabled={c.busy || !editable}>
        <label>Thành phố<input required readOnly value={c.address.city} /></label>
        {[['district','Quận / Huyện',100],['ward','Phường / Xã',100],['addressLine','Số nhà, đường',255]].map(([key,label,max]) => <label key={key}>{label}<input required maxLength={max} value={c.address[key]} onChange={e => c.setAddress({ ...c.address, [key]: e.target.value })} /></label>)}
        <button>Lưu địa chỉ shop</button></fieldset></form></>}
      </>}
      {role === 'SHOP' && ['all', 'staff'].includes(section) && <>
        <h3>Nhân viên cửa hàng</h3><p>Danh sách nhân viên đã được phân công vào shop. Sau khi duyệt đơn, bấm “Tải lại cửa hàng” để cập nhật danh sách.</p>
        {!c.members.length && <p>Chưa có nhân viên tham gia shop.</p>}
        <DataTable title="Nhân viên cửa hàng" rows={c.members} rowKey="userId" columns={[
 {key:'fullName',label:'Họ tên'},{key:'email',label:'Email'},
 {key:'detail',label:'Chi tiết',render:m=><InfoPopup title="Thông tin nhân viên"><p>Họ tên: {m.fullName}</p><p>Email: {m.email}</p><p>Trạng thái: {m.active ? 'Được làm việc' : 'Đã ngừng quyền'}</p></InfoPopup>},
 {key:'active',label:'Trạng thái',render:m=>m.active?'Được làm việc':'Đã ngừng quyền'},
 {key:'actions',label:'Thao tác',render:m=><button disabled={c.busy||c.selected.status!=='ACTIVE'} onClick={()=>c.toggle(m)}>{m.active?'Ngừng quyền':'Bật quyền'}</button>}
 ]} />
      </>}
    </>}
  </section>
}
