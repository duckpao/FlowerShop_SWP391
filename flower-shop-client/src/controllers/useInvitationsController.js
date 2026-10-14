import { validateEmail } from '../models/emailValidation'
import { useEffect, useState } from 'react'
import { managerShopService as api } from '../services/managerShopService'
import { useConfirm } from '../components/ui/confirm/ConfirmProvider'
export function useInvitationsController(shop) {
  const { confirm } = useConfirm()
  const [items, setItems] = useState([])
  const [email, setEmail] = useState('')
  const [emailError, setEmailError] = useState('')
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
  function send(e) { e.preventDefault(); const message = validateEmail(email,50); setEmailError(message); if(message) return; return run(() => api.invite(shop.id, email.trim()), 'Đã tiếp nhận gửi lời mời. Người nhận kiểm tra hộp thư và Spam.') }
  function resend(item) { return run(() => api.invite(shop.id, item.email), 'Đã tiếp nhận gửi lại. Mã lời mời cũ không còn hiệu lực.') }
  async function cancel(item) {
    if (!await confirm({ title: 'Hủy lời mời', message: `Hủy lời mời gửi đến ${item.email}?`, confirmLabel: 'Hủy lời mời', cancelLabel: 'Giữ lại', tone: 'danger' })) return
    return run(() => api.cancelInvite(shop.id, item.id), 'Đã hủy lời mời.')
  }
  return { items, email, emailError, setEmail: value => {setEmail(value); setEmailError(validateEmail(value,50))}, busy, error, notice, send, resend, cancel, retry: () => setRevision(x => x + 1) }
}
