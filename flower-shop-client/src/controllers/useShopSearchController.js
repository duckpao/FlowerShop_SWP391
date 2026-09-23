import { useEffect, useState } from 'react'
import { staffApplicationService as api } from '../services/staffApplicationService'
export function useShopSearchController(user) {
  const [shops, setShops] = useState([])
  const [query, setQuery] = useState('')
  const [selected, setSelected] = useState(null)
  const [showApplication, setShowApplication] = useState(false)
  const [sessionExpired, setSessionExpired] = useState(false)
  const [form, setForm] = useState({ fullName: '', phone: '', introduction: '' })
  const [applications, setApplications] = useState([])
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  useEffect(() => {
    let active = true
    const match = window.location.pathname.match(/^\/shops\/([^/]+)$/)
    if (match) api.detail(decodeURIComponent(match[1])).then(shop => { if (active) { setSelected(shop); setForm({ fullName: user?.fullName || '', phone: '', introduction: '' }) } }).catch(e => { if (active) setError(e.message) })
    api.search('').then(data => { if (active) setShops(data) }).catch(e => { if (active) setError(e.message) })
    return () => { active = false }
  }, [user])
  useEffect(() => {
    if (!user) { setApplications([]); return }
    let active = true
    let timer
    async function poll() {
      try { const data = await api.mine(); if (active) setApplications(data) }
      catch (e) { if (active) { if (e.status === 401) { setSessionExpired(true); return } setError(e.message) } }
      if (active) timer = setTimeout(poll, 15000)
    }
    poll()
    return () => { active = false; clearTimeout(timer) }
  }, [user])
  async function run(action) {
    setBusy(true); setError(''); setNotice('')
    try { await action() } catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  const search = e => { e.preventDefault(); return run(async () => setShops(await api.search(query))) }
  const submit = e => { e.preventDefault(); return run(async () => {
    await api.apply(selected.id, form); setApplications(await api.mine()); setShowApplication(false)
    setNotice('Đã gửi đơn đến Manager. Bạn vẫn là Customer cho đến khi được duyệt. Sau khi duyệt, hãy đăng nhập lại.')
  }) }
  return { shops, query, setQuery, selected, showApplication, setShowApplication, sessionExpired,
    select: shop => window.location.assign(shop ? `/shops/${encodeURIComponent(shop.id)}` : '/'),
    form, setForm, applications, busy, error, notice, search, submit }
}
