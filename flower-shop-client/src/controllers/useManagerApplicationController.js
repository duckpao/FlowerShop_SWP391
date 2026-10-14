import { useEffect, useState } from 'react'
import { managerApplicationService as api } from '../services/managerApplicationService'
export function useManagerApplicationController(user) {
  const [open, setOpen] = useState(false)
  const [items, setItems] = useState([])
  const [form, setForm] = useState({ fullName: user.fullName || '', phone: '', shopName: '', description: '', addressLine: '', city: 'Hà Nội', district: '', ward: '' })
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  useEffect(() => {
    let active = true
    api.mine().then(data => { if (active) setItems(data) }).catch(e => { if (active) setError(e.message) })
    return () => { active = false }
  }, [user.id])
  async function reload() {
    setBusy(true); setError('')
    try { setItems(await api.mine()) } catch (e) { setError(e.status === 401 ? 'Phiên đã hết hạn hoặc quyền đã thay đổi. Hãy đăng nhập lại để xem kết quả.' : e.message) } finally { setBusy(false) }
  }
  async function submit(e) {
    e.preventDefault(); setBusy(true); setError(''); setNotice('')
    try { const result = await api.submit(form); setItems([result]); setOpen(false); setNotice('Đã gửi đơn mở shop đến Admin. Bạn vẫn là Customer trong thời gian chờ duyệt.') }
    catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  return { open, setOpen, items, form, setForm, busy, error, notice, submit, reload }
}
