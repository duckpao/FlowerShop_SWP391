import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { accountService } from '../../services/accountService'
import { customerOrderService } from '../../services/customerOrderService'

export default function CustomerCustomAddress({ order, onChanged }) {
  const [addresses, setAddresses] = useState([])
  const [selected, setSelected] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const needed = order.orderType === 'CUSTOM' && order.needsDeliveryAddress && order.status === 'AWAITING_QUOTE'
  useEffect(() => {
    if (!needed) return
    let active = true
    accountService.addresses().then(items => { if (active) { setAddresses(items); setSelected(items.find(a => a.isDefault)?.id || items[0]?.id || '') } }).catch(e => { if (active) setError(e.message) })
    return () => { active = false }
  }, [needed, order.id])
  if (!needed) return null
  async function save(e) {
    e.preventDefault(); setBusy(true); setError('')
    try { await customerOrderService.customAddress(order.id, selected); await onChanged() }
    catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  return <form onSubmit={save} className="my-4 space-y-3 rounded-xl border border-yellow-200 p-4">
    <h3 className="font-semibold">Bổ sung địa chỉ nhận hoa</h3><p className="text-sm">Yêu cầu trước đây đã được chuyển vào đơn hàng. Chọn địa chỉ để shop báo giá kèm phí giao hàng.</p>
    <label className="block text-sm">Địa chỉ nhận hàng<select required disabled={busy} value={selected} onChange={e => setSelected(e.target.value)} className="mt-1 block w-full rounded-lg border p-2 dark:bg-gray-900"><option value="">Chọn địa chỉ</option>{addresses.map(a => <option value={a.id} key={a.id}>{[a.addressLine, a.ward, a.district, a.city].filter(Boolean).join(', ')}</option>)}</select></label>
    <Link to="/account" className="block text-sm text-brand-600">Thêm / cập nhật địa chỉ</Link>{error && <p role="alert" className="text-sm text-error-500">{error}</p>}
    <button disabled={busy || !selected} className="rounded-lg bg-brand-600 px-4 py-2 text-sm text-white">{busy ? 'Đang lưu…' : 'Lưu địa chỉ nhận hoa'}</button>
  </form>
}
