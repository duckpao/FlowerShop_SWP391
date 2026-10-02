import { useEffect, useState } from 'react'
import { managerShopService as api } from '../services/managerShopService'
export function useManagerShopController(role) {
  const [shops, setShops] = useState([])
  const [selected, setSelected] = useState(null)
  const [members, setMembers] = useState([])
  const [form, setForm] = useState({ name: '', description: '', logoUrl: '' })
  const [address, setAddress] = useState({ addressLine: '', city: 'Hà Nội', district: '', ward: '', isDefault: true })
  const [busy, setBusy] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [revision, setRevision] = useState(0)
  useEffect(() => {
    let active = true
    setBusy(true)
    setSelected(null); setMembers([])
    // Tiền đề của màn hình quản lý sản phẩm: tải shop của tài khoản để có selected.id truyền xuống ProductsView.
    api.list(role).then(async s => {
      if (!active) return
      if (role === 'SHOP' && s.length > 1) throw new Error('Dữ liệu chưa đúng: mỗi Manager chỉ được quản lý một shop.')
      setShops(s); setError('')
      if (role === 'SHOP' && s.length === 1) {
        const shop = s[0]
        const [staff, list] = await Promise.all([api.staff(shop.id), api.address(shop.id)])
        if (!active) return
        const a = list.find(x => x.isDefault) || list[0]
        setSelected(shop); setMembers(staff)
        setForm({ name: shop.name, description: shop.description || '', logoUrl: shop.logoUrl || '' })
        setAddress({ addressLine: a?.addressLine || '', city: a?.city || 'Hà Nội', district: a?.district || '', ward: a?.ward || '', isDefault: true })
      }
    }).catch(e => { if (active) setError(e.message) })
      .finally(() => { if (active) setBusy(false) })
    return () => { active = false }
  }, [role, revision])
  async function run(action) {
    setBusy(true); setError(''); setNotice('')
    try { await action() } catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  const select = shop => run(async () => {
    setSelected(null); setMembers([])
    const staff = role === 'SHOP' ? await api.staff(shop.id) : []
    const list = role === 'SHOP' ? await api.address(shop.id) : []
    const a = list.find(x => x.isDefault) || list[0]
    setAddress(a ? { addressLine: a.addressLine || '', city: a.city || 'Hà Nội', district: a.district || '', ward: a.ward || '', isDefault: true } : { addressLine: '', city: 'Hà Nội', district: '', ward: '', isDefault: true })
    setSelected(shop); setMembers(staff)
    setForm({ name: shop.name, description: shop.description || '', logoUrl: shop.logoUrl || '' })
  })
  const save = e => { e.preventDefault(); return run(async () => {
    const shop = await api.save(selected.id, form); setSelected(shop); setShops(s => s.map(x => x.id === shop.id ? shop : x)); setNotice('Đã cập nhật hồ sơ shop.')
  }) }
  const saveAddress = e => { e.preventDefault(); return run(async () => { await api.saveAddress(selected.id, address); setNotice('Đã lưu địa chỉ cửa hàng.') }) }
  const toggle = member => {
    if (!window.confirm(`${member.active ? 'Ngừng' : 'Bật'} quyền làm việc của ${member.email}?`)) return
    return run(async () => { await api.active(selected.id, member.userId, !member.active); setMembers(await api.staff(selected.id)); setNotice('Đã cập nhật quyền làm việc.') })
  }
  return { shops, selected, members, form, setForm, address, setAddress, saveAddress, busy, error, notice, select, save, toggle,
    retry: () => { setSelected(null); setRevision(x => x + 1) } }
}
