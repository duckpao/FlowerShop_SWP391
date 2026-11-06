import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { staffApplicationService } from '../services/staffApplicationService'
import { customerOrderService } from '../services/customerOrderService'
import { accountService } from '../services/accountService'
import { productService } from '../services/productService'
import { customFlowerService } from '../services/customFlowerService'
import { BudgetSuggestions, customButton } from '../components/customFlowers/CustomFlowerParts'
import '../styles/custom-flowers.css'

export default function CustomFlowerOrderView({ auth }) {
  const [params] = useSearchParams()
  const [shops, setShops] = useState([])
  const [options, setOptions] = useState([])
  const [catalogBusy, setCatalogBusy] = useState(true)
  const [catalogError, setCatalogError] = useState('')
  const [catalogVersion, setCatalogVersion] = useState(0)
  const [form, setForm] = useState({ shopId: params.get('shopId') || '', budget: params.get('budget') || '', quantity: params.get('quantity') || '1', description: '', flowers: '', colors: '', occasion: '', wrapping: [], deliveryDate: '' })
  const [selected, setSelected] = useState(null)
  const [saved, setSaved] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [addresses, setAddresses] = useState([])
  const [addressId, setAddressId] = useState('')
  useEffect(() => {
    let active = true
    setCatalogBusy(true); setCatalogError('')
    customFlowerService.options()
      .then(items => { if (active) setOptions(items) })
      .catch(e => { if (active) { setOptions([]); setCatalogError(e.message) } })
      .finally(() => { if (active) setCatalogBusy(false) })
    return () => { active = false }
  }, [catalogVersion])
  useEffect(() => {
    if (auth?.user?.role !== 'CUSTOMER') return
    let active = true
    accountService.addresses().then(items => { if (active) { setAddresses(items); setAddressId(items.find(a => a.isDefault)?.id || items[0]?.id || '') } }).catch(e => { if (active) setError(e.message) })
    return () => { active = false }
  }, [auth?.user?.id, auth?.user?.role])
  useEffect(() => { let active = true; staffApplicationService.search('').then(v => { if (active) setShops(v) }).catch(e => { if (active) setError(e.message) }); return () => { active = false } }, [])
  useEffect(() => {
    const id = params.get('productId'); if (!id) return
    let active = true
    productService.detail(id).then(p => { if (active && p.shopId === params.get('shopId')) setSelected(p) }).catch(e => { if (active) setError(e.message) })
    return () => { active = false }
  }, [params])
  const edit = (name, value) => { setForm(f => ({ ...f, [name]: value })); if (['shopId', 'budget', 'quantity'].includes(name)) setSelected(null) }
  const flowerOptions = options.filter(option => option.group === 'FLOWER')
  const occasionOptions = options.filter(option => option.group === 'OCCASION')
  const wrappingOptions = options.filter(option => ['WRAPPING', 'ACCESSORY'].includes(option.group))
  const enabledWrappingOptions = wrappingOptions.filter(option => !option.disabled)
  async function submit(e) {
    e.preventDefault(); if (busy || catalogBusy || catalogError) return; setBusy(true); setError('')
    try {
      const { shopId, budget, quantity, description, ...preferences } = form
      const enabledLabels = new Set(enabledWrappingOptions.map(option => option.label))
      const wrapping = preferences.wrapping.filter(label => enabledLabels.has(label)).join(', ')
      const result = await customerOrderService.createCustom({ addressId, design: { shopId, budget: Number(budget), quantity: Number(quantity), description, preferences: { ...preferences, wrapping, referenceProductId: selected?.id || null } } })
      setSaved(result)
    } catch (failure) { setError(failure.message) } finally { setBusy(false) }
  }
  if (auth?.busy && !auth?.user) return <p role="status">Đang kiểm tra đăng nhập…</p>
  if (!auth?.user) return <main className="catalog-page"><h1>Đặt hoa theo yêu cầu</h1><p>Đăng nhập bằng tài khoản khách hàng để gửi yêu cầu.</p><Link className={customButton} to={`/login?next=${encodeURIComponent('/custom-orders/new' + window.location.search)}`}>Đăng nhập</Link></main>
  if (auth.user.role !== 'CUSTOMER') return <main className="catalog-page"><p>Chức năng này dành cho tài khoản khách hàng.</p></main>
  return <main className="catalog-page custom-flower-page">
    <header className="catalog-page-header"><div><p className="text-sm text-brand-600">Thiết kế theo ý bạn</p><h1>Đặt hoa theo yêu cầu riêng</h1></div><Link to="/orders">Đơn hàng của tôi</Link></header>
    {saved ? <section className="catalog-panel space-y-4"><p role="status" className="message success">Đã gửi đơn hoa riêng. Cửa hàng sẽ xem và báo giá trong luồng đơn hàng.</p><Link className={customButton} to={`/orders/${saved.orderId}`}>Xem đơn hàng & báo giá</Link></section> : <>
      <section className="catalog-panel custom-order-panel"><div className="custom-order-intro"><h2>Thiết kế bó hoa của bạn</h2><p>Chọn hoa và những chi tiết bạn yêu thích. Cửa hàng sẽ tư vấn và báo giá phù hợp với ngân sách.</p></div>
        {error && <p role="alert" className="message error">{error}</p>}
        {catalogBusy && <p role="status">Đang tải các lựa chọn đặt hoa…</p>}
        {catalogError && <div role="alert" className="message error">{catalogError} <button type="button" onClick={() => setCatalogVersion(v => v + 1)}>Thử lại</button></div>}
        <form onSubmit={submit}><fieldset disabled={busy || catalogBusy || !!catalogError} className="custom-order-fields">
          <section className="custom-order-section"><h3><span>01</span> Thông tin đặt hoa</h3><div className="custom-order-grid custom-order-grid-three"><label>Cửa hàng *<select required value={form.shopId} onChange={e => edit('shopId', e.target.value)}><option value="">Chọn cửa hàng</option>{shops.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}</select></label>
            <label>Ngân sách tiền hoa (VND)<input required type="number" min="1000" max="999999999" step="1000" value={form.budget} onChange={e => edit('budget', e.target.value)} /></label>
            <label>Số bó/mẫu<input required type="number" min="1" max="100" step="1" value={form.quantity} onChange={e => edit('quantity', e.target.value)} /></label></div></section>
          <section className="custom-order-section"><label>Địa chỉ nhận hoa *<select required value={addressId} onChange={e => setAddressId(e.target.value)}><option value="">Chọn địa chỉ nhận hàng</option>{addresses.map(a => <option key={a.id} value={a.id}>{[a.recipientName, a.addressLine, a.ward, a.district, a.city].filter(Boolean).join(', ')}</option>)}</select></label><p className="custom-order-hint"><Link to="/account">Thêm hoặc cập nhật địa chỉ</Link> · Phí giao hàng được cộng khi shop báo giá. Ngân sách ở trên dành cho tiền hoa.</p></section>
          <section className="custom-order-section"><h3><span>02</span> Phong cách & dịp tặng</h3><div className="custom-order-grid">
            <label>Loại hoa mong muốn<select value={form.flowers} onChange={e => edit('flowers', e.target.value)}><option value="">Chọn loại hoa</option>{flowerOptions.map(option => <option key={option.id} value={option.label} disabled={option.disabled}>{option.label}{option.disabled ? ' (Tạm ngưng)' : ''}</option>)}</select></label>
            <label>Tông màu<input maxLength={100} value={form.colors} placeholder="Ví dụ: hồng pastel, trắng kem" onChange={e => edit('colors', e.target.value)} /></label>
            <label>Dịp tặng<select value={form.occasion} onChange={e => edit('occasion', e.target.value)}><option value="">Chưa chọn</option>{occasionOptions.map(option => <option key={option.id} value={option.label} disabled={option.disabled}>{option.label}{option.disabled ? ' (Tạm ngưng)' : ''}</option>)}</select></label>
            <label>Ngày mong muốn nhận hoa<input type="date" value={form.deliveryDate} onChange={e => edit('deliveryDate', e.target.value)} /></label>
          </div></section>
          <section className="custom-order-section"><h3><span>03</span> Giấy gói & phụ kiện</h3><p className="custom-order-hint">Tích chọn một hoặc nhiều loại bạn muốn thêm vào bó hoa. Bạn có thể bỏ qua bước này.</p>
            <div className="custom-accessory-groups">{[['WRAPPING', 'Giấy gói'], ['ACCESSORY', 'Phụ kiện']].map(([group, title]) => {
              const options = wrappingOptions.filter(option => option.group === group)
              if (!options.length) return null
              return <fieldset key={group} className="custom-wrapping-options"><legend>{title}</legend><div className="custom-accessory-grid">{options.map(({ id, label, disabled }) => <label key={id} className={`custom-wrapping-option${disabled ? ' is-disabled' : form.wrapping.includes(label) ? ' is-selected' : ''}`} title={disabled ? 'Tạm ngưng lựa chọn' : undefined}><input type="checkbox" name="wrapping" value={label} disabled={disabled} checked={!disabled && form.wrapping.includes(label)} onChange={e => { if (disabled) return; setForm(f => ({ ...f, wrapping: e.target.checked ? [...f.wrapping, label] : f.wrapping.filter(value => value !== label) })) }} /><span>{label}</span></label>)}</div></fieldset>
            })}</div>
          </section>
          <section className="custom-order-section"><h3><span>04</span> Lời nhắn cho cửa hàng</h3><label>Mô tả thêm (không bắt buộc)<textarea maxLength={3000} rows={4} value={form.description} placeholder="Kiểu bó hoa, nội dung thiệp hoặc những điều bạn muốn cửa hàng lưu ý…" onChange={e => edit('description', e.target.value)} /></label><p className="custom-order-hint">Có thể để trống · Tối đa 3.000 ký tự</p></section>
          {selected && <div className="rounded-xl bg-brand-50 p-4 dark:bg-brand-500/10"><p>Mẫu tham khảo: <strong>{selected.name}</strong></p><button type="button" onClick={() => setSelected(null)}>Bỏ mẫu tham khảo</button></div>}
          <div className="custom-order-footer"><p>Gửi yêu cầu chưa phát sinh thanh toán.<br />Cửa hàng sẽ liên hệ để xác nhận thiết kế và giá.</p><button className={customButton} disabled={!form.shopId || !addressId || busy}>{busy ? 'Đang gửi…' : 'Gửi yêu cầu cho shop'}</button></div>
        </fieldset></form>
      </section>
      {form.shopId && <section className="catalog-panel"><BudgetSuggestions budget={form.budget} quantity={form.quantity} shopId={form.shopId} selectedId={selected?.id} onChoose={setSelected} /></section>}
    </>}
  </main>
}
