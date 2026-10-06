import { useEffect, useState } from 'react'
import { adminCustomerService as api } from '../services/adminCustomerService'

export function useAdminCustomersController() {
  const [query, setQuery] = useState('')
  const [status, updateStatus] = useState('')
  const [params, setParams] = useState({ q: '', status: '', page: 0, size: 10 })
  const [data, setData] = useState(null)
  const [selected, setSelected] = useState(null)
  const [busy, setBusy] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [revision, setRevision] = useState(0)
  useEffect(() => {
    let active = true
    setBusy(true); setError('')
    api.list(params).then(value => { if (active) setData(value) })
      .catch(e => { if (active) setError(e.message) }).finally(() => { if (active) setBusy(false) })
    return () => { active = false }
  }, [params, revision])
  async function detail(id) {
    setBusy(true); setError(''); setNotice(''); setSelected(null)
    try { setSelected(await api.detail(id)) } catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  async function toggle(customer) {
    const blocked = customer.status !== 'BANNED'
    if (!window.confirm(`${blocked ? 'Khóa' : 'Mở khóa'} tài khoản ${customer.email}?${blocked ? ' Các phiên đăng nhập sẽ bị thu hồi.' : ''}`)) return
    setBusy(true); setError(''); setNotice('')
    try {
      const result = await api.block(customer.id, blocked)
      if (selected?.id === result.id) setSelected(result)
      setNotice(blocked ? 'Đã khóa tài khoản và thu hồi phiên.' : 'Đã mở khóa. Khách hàng cần đăng nhập lại.')
      setRevision(x => x + 1)
    } catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  function search(e) {
    e.preventDefault(); setSelected(null); setNotice('')
    setParams({ ...params, q: query.trim(), status, page: 0 })
  }
  function setStatus(value) {
    updateStatus(value)
    setSelected(null); setNotice('')
    setParams(current => ({ ...current, q: query.trim(), status: value, page: 0 }))
  }
  return { query, setQuery, status, setStatus, data, selected, setSelected, busy, error, notice, detail, toggle, search,
    next: delta => setParams(p => ({ ...p, page: p.page + delta })), retry: () => setRevision(x => x + 1) }
}
