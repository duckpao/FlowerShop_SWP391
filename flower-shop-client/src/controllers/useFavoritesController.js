import { useEffect, useState } from 'react'
import { favoriteService } from '../services/favoriteService'

export function useFavoritesController(user) {
  const [data, setData] = useState(null)
  const [page, setPage] = useState(0)
  const [revision, setRevision] = useState(0)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  useEffect(() => {
    if (user?.role !== 'CUSTOMER') { setData(null); return }
    let active = true
    setBusy(true); setError('')
    favoriteService.mine(page)
      .then(value => { if (active) setData(value) })
      .catch(e => { if (active) setError(e.message) })
      .finally(() => { if (active) setBusy(false) })
    return () => { active = false }
  }, [user, page, revision])

  async function remove(productId) {
    setBusy(true); setError(''); setNotice('')
    try {
      await favoriteService.remove(productId)
      setNotice('Đã bỏ khỏi danh sách yêu thích.')
      setRevision(x => x + 1)
    } catch (e) { setError(e.message) } finally { setBusy(false) }
  }

  return { data, page, setPage, busy, error, notice, remove, reload: () => setRevision(x => x + 1) }
}

/** Tập id đã lưu + hành động bật/tắt, dùng chung cho trang danh sách và trang chi tiết. */
export function useFavoriteToggle(user) {
  const [saved, setSaved] = useState(() => new Set())
  const [error, setError] = useState('')

  useEffect(() => {
    if (user?.role !== 'CUSTOMER') { setSaved(new Set()); return }
    let active = true
    favoriteService.ids()
      .then(ids => { if (active) setSaved(ids) })
      .catch(() => { if (active) setSaved(new Set()) })
    return () => { active = false }
  }, [user])

  async function toggle(productId) {
    if (user?.role !== 'CUSTOMER') {
      window.location.assign(`/login?next=${encodeURIComponent(window.location.pathname)}`)
      return
    }
    const wasSaved = saved.has(productId)
    setError('')
    try {
      if (wasSaved) await favoriteService.remove(productId)
      else await favoriteService.add(productId)
      setSaved(current => {
        const next = new Set(current)
        if (wasSaved) next.delete(productId); else next.add(productId)
        return next
      })
    } catch (e) { setError(e.message) }
  }

  return { saved, toggle, error }
}
