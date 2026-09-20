import { useAccountController } from '../controllers/useAccountController'
import { roleLabels } from '../models/authModel'
import '../styles/account.css'
import AdminCustomersView from './AdminCustomersView'
import AdminShopsView from './AdminShopsView'
import ManagerShopsView from './ManagerShopsView'
import AcceptInvitationView from './AcceptInvitationView'

export default function AccountView({ user, logout, checkSession, busy: authBusy, error: authError, notice: authNotice }) {
  const c = useAccountController(user)
  const busy = c.busy || authBusy
  return <main className="account-page">
    {['CUSTOMER', 'SHOP_STAFF'].includes(user.role) && <AcceptInvitationView email={user.email} />}
    {['SHOP', 'SHOP_STAFF'].includes(user.role) && <ManagerShopsView role={user.role} />}
    {user.role === 'ADMIN' && <AdminCustomersView />}
    {user.role === 'ADMIN' && <AdminShopsView />}
    <header className="account-header"><div><h1>Tài khoản FlowerShop</h1><p>{roleLabels[user.role]}</p></div>
      <button type="button" disabled={busy} onClick={logout}>Đăng xuất</button></header>
    {(c.error || authError) && <p className="message error" role="alert">{c.error || authError}</p>}
    {(c.notice || authNotice) && <p className="message success" role="status">{c.notice || authNotice}</p>}
    {c.busy && <p role="status">Đang tải…</p>}
    {!c.profile && !c.busy && <button onClick={c.retry}>Tải lại hồ sơ</button>}
    {c.profile && <section className="account-card"><h2>Hồ sơ cá nhân</h2>
      <p>Email: {c.profile.email} {c.profile.emailVerified && '· Đã xác thực'}</p>
      <form onSubmit={c.saveProfile}><fieldset disabled={busy}>
        <label>Họ và tên<input required maxLength={100} autoComplete="name" value={c.form.fullName}
          onChange={e => c.setForm({ ...c.form, fullName: e.target.value })} /></label>
        <label>Số điện thoại<input type="tel" maxLength={16} pattern="[+]?[0-9]{9,15}" autoComplete="tel"
          value={c.form.phone} onChange={e => c.setForm({ ...c.form, phone: e.target.value })} />
          <small>Có thể để trống; nếu nhập cần 9–15 chữ số, có thể bắt đầu bằng +.</small></label>
        <button className="primary">Lưu hồ sơ</button>
      </fieldset></form>
    </section>}
    {c.profile && user.role === 'CUSTOMER' && <section className="account-card"><h2>Địa chỉ giao hàng</h2>
      <p>Khu vực hỗ trợ: {c.cities.join(', ') || 'Chưa mở giao hàng'}. Tối đa 10 địa chỉ.</p>
      {!c.addresses.length && <p>Bạn chưa có địa chỉ giao hàng.</p>}
      <ul className="address-list">{c.addresses.map(item => <li key={item.id}>
        <strong>{item.addressLine}</strong> {item.isDefault && <span className="address-badge">Mặc định</span>}
        <p>{[item.ward, item.district, item.city].filter(Boolean).join(', ')}</p>
        <div className="address-actions">
          <button disabled={busy} onClick={() => c.edit(item)}>Sửa</button>
          {!item.isDefault && <button disabled={busy} onClick={() => c.makeDefault(item.id)}>Đặt mặc định</button>}
          <button disabled={busy} onClick={() => c.remove(item)}>Xóa</button>
        </div>
      </li>)}</ul>
      <h3>{c.editing ? 'Sửa địa chỉ' : 'Thêm địa chỉ'}</h3>
      <form onSubmit={c.saveAddress}><fieldset disabled={busy || !c.cities.length}>
        <label>Tỉnh / Thành phố<select required value={c.address.city} onChange={e => c.setAddress({ ...c.address, city: e.target.value })}>
          <option value="">Chọn khu vực</option>{c.cities.map(city => <option key={city} value={city}>{city}</option>)}
        </select></label>
        <label>Quận / Huyện<input required maxLength={100} value={c.address.district} onChange={e => c.setAddress({ ...c.address, district: e.target.value })} /></label>
        <label>Phường / Xã<input required maxLength={100} value={c.address.ward} onChange={e => c.setAddress({ ...c.address, ward: e.target.value })} /></label>
        <label>Số nhà, đường, tòa nhà<input required maxLength={255} autoComplete="street-address" value={c.address.addressLine}
          onChange={e => c.setAddress({ ...c.address, addressLine: e.target.value })} /></label>
        <label className="checkbox-label"><input type="checkbox" checked={c.address.isDefault}
          onChange={e => c.setAddress({ ...c.address, isDefault: e.target.checked })} />Đặt làm địa chỉ mặc định</label>
        <button className="primary">{c.editing ? 'Lưu thay đổi' : 'Thêm địa chỉ'}</button>
        {c.editing && <button type="button" onClick={c.cancel}>Hủy chỉnh sửa</button>}
      </fieldset></form>
    </section>}
    <button className="text-button" disabled={busy} onClick={checkSession}>Kiểm tra phiên đăng nhập</button>
  </main>
}
