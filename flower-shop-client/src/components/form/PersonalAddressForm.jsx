import GhnAddressPicker from '../GhnAddressPicker'

export default function PersonalAddressForm({ controller: c, busy }) {
  const a = c.address
  const set = (key, value) => c.setAddress(current => ({ ...current, [key]: value }))
  const complete = a.recipientName.trim() && /^0[0-9]{9}$/.test(a.recipientPhone)
    && a.city && a.ward && a.ghnWardCode && a.ghnDistrictId && a.addressLine.trim()
  const row = 'w-full border-0 border-b border-gray-100 bg-transparent px-0 py-4 text-sm outline-none focus:border-brand-500 dark:border-gray-700 dark:text-white'
  const required = <span className="text-error-500"> *</span>
  return <form onSubmit={c.saveAddress} className="rounded-xl bg-gray-50 p-3 dark:bg-gray-900">
    <fieldset disabled={busy} className="space-y-3">
      <div className="rounded-xl bg-white px-4 py-2 dark:bg-gray-800">
        <h3 className="py-2 font-semibold">{c.editing ? 'Sửa địa chỉ' : 'Địa chỉ'}</h3>
        <label className="block text-sm text-gray-500" htmlFor="recipient-name">Họ và tên{required}</label>
        <input id="recipient-name" className={row} required maxLength={100} autoComplete="name" value={a.recipientName}
          onChange={e => { e.target.setCustomValidity(e.target.value.trim() ? '' : 'Vui lòng nhập họ và tên.'); set('recipientName', e.target.value) }} placeholder="Họ và tên người nhận" />
        <label className="block pt-3 text-sm text-gray-500" htmlFor="recipient-phone">Số điện thoại{required}</label>
        <input id="recipient-phone" className={row} type="tel" inputMode="numeric" required maxLength={10} pattern="0[0-9]{9}" autoComplete="tel" value={a.recipientPhone}
          onChange={e => set('recipientPhone', e.target.value)} aria-describedby="recipient-phone-hint" />
        <p id="recipient-phone-hint" className="pt-1 text-xs text-gray-500">10 chữ số, bắt đầu bằng 0.</p>
        <div className="grid gap-3 py-3">
          <GhnAddressPicker value={a} onChange={c.setAddress} />
        </div>
        <label className="block pt-3 text-sm text-gray-500" htmlFor="address-street">Tên đường, Tòa nhà, Số nhà{required}</label>
        <input id="address-street" className={row} required maxLength={255} autoComplete="street-address" value={a.addressLine}
          onChange={e => { e.target.setCustomValidity(e.target.value.trim() ? '' : 'Vui lòng nhập địa chỉ chi tiết.'); set('addressLine', e.target.value) }} />
      </div>
      <div className="rounded-xl bg-white px-4 dark:bg-gray-800">
        {[['isDefault', 'Đặt làm địa chỉ mặc định'], ['isPickup', 'Đặt làm địa chỉ lấy hàng'], ['isReturn', 'Đặt làm địa chỉ trả hàng']].map(([key, label]) =>
          <div key={key} className="flex items-center justify-between border-b border-gray-100 py-3 dark:border-gray-700">
            <span id={`label-${key}`} className="text-sm">{label}</span>
            <button type="button" role="switch" aria-checked={!!a[key]} aria-labelledby={`label-${key}`} onClick={() => set(key, !a[key])}
              className={`relative h-6 w-11 rounded-full transition-colors focus-visible:outline-2 focus-visible:outline-brand-500 ${a[key] ? 'bg-brand-500' : 'bg-gray-200 dark:bg-gray-600'}`}>
              <span className={`absolute top-0.5 h-5 w-5 rounded-full bg-white shadow transition-all ${a[key] ? 'left-5' : 'left-0.5'}`} />
            </button>
          </div>)}
      </div>
      <p className="px-1 text-xs text-gray-500">Các trường có dấu <span className="text-error-500">*</span> là bắt buộc.</p>
      <button type="submit" disabled={!complete || busy} className="mt-6 w-full rounded-lg bg-brand-500 py-3 font-semibold text-white disabled:bg-gray-200 disabled:text-gray-400">{c.editing ? 'LƯU THAY ĐỔI' : 'HOÀN THÀNH'}</button>
      {c.editing && <button type="button" className="w-full py-2 text-sm text-gray-500" onClick={c.cancel}>Hủy chỉnh sửa</button>}
    </fieldset>
  </form>
}
