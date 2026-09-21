import { useEffect, useState } from 'react'
import { staffApplicationService as api } from '../services/staffApplicationService'
export function useStaffApplicationsController(shopId) {
  const [items, setItems] = useState([])
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [revision, setRevision] = useState(0)
  const [notice, setNotice] = useState('')
  useEffect(() => {
    let active = true
    setBusy(true)
    let timer
    async function load() {
      try { const data = await api.list(shopId); if (active) { setItems(data); setError('') } }
      catch (e) { if (active) setError(e.message) }
      finally { if (active) { setBusy(false); timer = setTimeout(load, 15000) } }
    }
    load()
    return () => { active = false; clearTimeout(timer) }
  }, [shopId, revision])
  async function decide(id, approve) {
    setBusy(true); setError('')
    try { await api.decide(shopId, id, approve); setNotice(approve ? 'Đã duyệt đơn và chuyển tài khoản thành Staff.' : 'Đã từ chối đơn. Tài khoản vẫn là Customer.'); setRevision(x => x + 1) } catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  return { items, busy, error, notice, decide, reload: () => setRevision(x => x + 1) }
}
