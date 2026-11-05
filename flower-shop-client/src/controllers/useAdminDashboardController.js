import { useEffect, useState } from 'react'
import { adminDashboardService as api } from '../services/adminDashboardService'

export function useAdminDashboardController() {
  const [granularity, setGranularity] = useState('DAY')
  const [range, setRange] = useState(null) // { from, to } (yyyy-MM-dd) hoặc null = khoảng mặc định
  const [draft, setDraft] = useState({ from: '', to: '' })
  const [rangeError, setRangeError] = useState('')
  const [data, setData] = useState(null)
  const [busy, setBusy] = useState(true)
  const [error, setError] = useState('')
  const [revision, setRevision] = useState(0)
  useEffect(() => {
    let active = true
    setBusy(true); setError('')
    api.get(granularity, range).then(value => { if (active) setData(value) })
      .catch(e => { if (active) setError(e.message || 'Không thể tải báo cáo.') })
      .finally(() => { if (active) setBusy(false) })
    return () => { active = false }
  }, [granularity, range, revision])
  function applyRange(e) {
    e?.preventDefault()
    if (!draft.from || !draft.to) { setRangeError('Vui lòng chọn cả ngày bắt đầu và ngày kết thúc.'); return }
    if (draft.from > draft.to) { setRangeError('Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.'); return }
    setRangeError(''); setRange({ from: draft.from, to: draft.to })
  }
  function resetRange() { setRangeError(''); setDraft({ from: '', to: '' }); setRange(null) }
  return { granularity, setGranularity, range, draft, setDraft, rangeError, applyRange, resetRange,
    data, busy, error, reload: () => setRevision(x => x + 1) }
}
