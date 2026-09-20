import { useManagerShopController } from '../controllers/useManagerShopController'
import { shopStatusLabels } from '../models/shopModel'
import ShopInvitationsView from './ShopInvitationsView'
export default function ManagerShopsView({ role }) {
  const c = useManagerShopController(role)
  const editable = role === 'SHOP' && ['ACTIVE', 'PENDING'].includes(c.selected?.status)
  return <section className="account-card"><h2>Cửa hàng của tôi</h2>
    {c.error && <p role="alert" className="message error">{c.error}</p>}
    {c.notice && <p role="status" className="message success">{c.notice}</p>}
    {c.busy && <p>Đang tải…</p>}
    <button disabled={c.busy} onClick={c.retry}>Tải lại cửa hàng</button>
    {!c.busy && !c.shops.length && <p>Chưa có cửa hàng được phân công.</p>}
    {role !== 'SHOP' && <ul className="address-list">{c.shops.map(s => <li key={s.id}>{s.name} · {shopStatusLabels[s.status]} <button disabled={c.busy} onClick={() => c.select(s)}>Xem cửa hàng</button></li>)}</ul>}
    {c.selected && <><h3>{c.selected.name}</h3><p>{shopStatusLabels[c.selected.status]}</p>
      {role === 'SHOP' && <ShopInvitationsView key={c.selected.id} shop={c.selected} />}
      {!editable && <p>Cửa hàng hiện ở chế độ chỉ xem đối với tài khoản này.</p>}
      <form onSubmit={c.save}><fieldset disabled={c.busy || !editable}>
        <label>Tên shop<input required maxLength={255} value={c.form.name} onChange={e => c.setForm({ ...c.form, name: e.target.value })} /></label>
        <label>Mô tả<textarea maxLength={5000} value={c.form.description} onChange={e => c.setForm({ ...c.form, description: e.target.value })} /></label>
        <label>URL logo (HTTPS)<input type="url" maxLength={255} pattern="https://.*" value={c.form.logoUrl} onChange={e => c.setForm({ ...c.form, logoUrl: e.target.value })} /></label>
        {role === 'SHOP' && <button>Lưu hồ sơ cửa hàng</button>}
      </fieldset></form>
      {role === 'SHOP' && <><h3>Địa chỉ cửa hàng</h3><form onSubmit={c.saveAddress}><fieldset disabled={c.busy || !editable}>
        <label>Thành phố<input required readOnly value={c.address.city} /></label>
        {[['district','Quận / Huyện',100],['ward','Phường / Xã',100],['addressLine','Số nhà, đường',255]].map(([key,label,max]) => <label key={key}>{label}<input required maxLength={max} value={c.address[key]} onChange={e => c.setAddress({ ...c.address, [key]: e.target.value })} /></label>)}
        <button>Lưu địa chỉ shop</button></fieldset></form>
        <h3>Nhân viên cửa hàng</h3><p>Thêm bằng email tài khoản Shop Staff đang hoạt động, đã xác thực. Không tự đổi role của tài khoản.</p>
        <form onSubmit={c.add}><fieldset disabled={c.busy || c.selected.status !== 'ACTIVE'}>
          <label>Email nhân viên<input type="email" required maxLength={255} value={c.email} onChange={e => c.setEmail(e.target.value)} /></label><button>Thêm nhân viên</button>
        </fieldset></form>
        <ul className="address-list">{c.members.map(m => <li key={m.userId}>{m.fullName} · {m.email} · {m.active ? 'Được làm việc' : 'Đã ngừng quyền'}
          <button disabled={c.busy || c.selected.status !== 'ACTIVE'} onClick={() => c.toggle(m)}>{m.active ? 'Ngừng quyền' : 'Bật quyền'}</button></li>)}</ul>
      </>}
    </>}
  </section>
}
