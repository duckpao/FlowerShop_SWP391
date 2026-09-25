import { useEffect, useState } from 'react'
import { managerShopService as api } from '../services/managerShopService'
export function useInvitationsController(shop) {
  const [items, setItems] = useState([])
  const [email, setEmail] = useState('')
  const [busy, setBusy] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [revision, setRevision] = useState(0)
  useEffect(() => {
    let active = true
    setBusy(true); setError('')
    api.invitations(shop.id).then(data => { if (active) setItems(data) }).catch(e => { if (active) setError(e.message) })
      .finally(() => { if (active) setBusy(false) })
    return () => { active = false }
  }, [shop.id, revision])
  async function run(action, message) {
    setBusy(true); setError(''); setNotice('')
    try { await action(); setNotice(message); setRevision(x => x + 1) } catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  function send(e) { e.preventDefault(); return run(() => api.invite(shop.id, email.trim()), 'Đã tiếp nhận gửi lời mời. Người nhận kiểm tra hộp thư và Spam.') }
  function resend(item) { return run(() => api.invite(shop.id, item.email), 'Đã tiếp nhận gửi lại. Mã lời mời cũ không còn hiệu lực.') }
  function cancel(item) {
    if (!window.confirm(`Hủy lời mời gửi đến ${item.email}?`)) return
    return run(() => api.cancelInvite(shop.id, item.id), 'Đã hủy lời mời.')
  }
  return { items, email, setEmail, busy, error, notice, send, resend, cancel, retry: () => setRevision(x => x + 1) }
}
