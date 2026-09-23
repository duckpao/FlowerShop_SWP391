import { useEffect, useState } from 'react'
import { adminShopService as api } from '../services/adminShopService'
import { shopActions } from '../models/shopModel'

export function useAdminShopsController() {
  const [q, setQ] = useState('')
  const [status, setStatus] = useState('')
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
    api.list(params).then(value => {
      if (!active) return
      if (params.page > 0 && params.page >= value.totalPages) { setParams(p => ({ ...p, page: Math.max(0, value.totalPages - 1) })); return }
      setData(value)
    }).catch(e => { if (active) setError(e.message) }).finally(() => { if (active) setBusy(false) })
    return () => { active = false }
  }, [params, revision])
  async function detail(id) {
    setBusy(true); setError(''); setSelected(null)
    try { setSelected(await api.detail(id)) } catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  async function transition(shop) {
    const [action, label] = shopActions[shop.status] || []
    if (!action || !window.confirm(`${label}: ${shop.name}?`)) return
    setBusy(true); setError(''); setNotice('')
    try {
      const result = await api.transition(shop.id, action)
      if (selected?.id === result.id) setSelected(result)
      setNotice(`${label} thành công.`); setRevision(x => x + 1)
    } catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  function search(e) { e.preventDefault(); setSelected(null); setNotice(''); setParams({ ...params, q: q.trim(), status, page: 0 }) }
  return { q, setQ, status, setStatus, data, selected, setSelected, busy, error, notice, detail, transition, search,
    next: delta => setParams(p => ({ ...p, page: p.page + delta })), retry: () => setRevision(x => x + 1) }
}
