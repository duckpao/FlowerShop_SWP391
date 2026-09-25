import { useEffect, useState } from 'react'
import { managerApplicationService as api } from '../services/managerApplicationService'
export function useAdminManagerApplicationsController() {
  const [data, setData] = useState(null)
  const [status, setStatus] = useState('PENDING')
  const [page, setPage] = useState(0)
  const [revision, setRevision] = useState(0)
  const [notes, setNotes] = useState({})
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [selectedId, setSelectedId] = useState(null)
  useEffect(() => {
    let active = true
    setBusy(true)
    setSelectedId(null)
    api.list(status, page).then(result => { if (active) { setData(result); setError('') } }).catch(e => { if (active) setError(e.message) }).finally(() => { if (active) setBusy(false) })
    return () => { active = false }
  }, [status, page, revision])
  async function decide(id, approve) {
    if (!window.confirm(approve ? 'Duyệt đơn, tạo shop và cấp quyền Manager?' : 'Từ chối đơn mở shop này?')) return
    setBusy(true); setError(''); setNotice('')
    try { await api.decide(id, approve, notes[id] || ''); setSelectedId(null); setNotice(approve ? 'Đã tạo shop và cấp quyền Manager. Người dùng cần đăng nhập lại.' : 'Đã từ chối. Tài khoản giữ nguyên quyền.'); setRevision(x => x + 1) }
    catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  return { data, status, selectedId, setSelectedId, filter: value => { setSelectedId(null); setStatus(value); setPage(0) }, setPage, notes, setNotes, busy, error, notice, decide, reload: () => setRevision(x => x + 1) }
}
