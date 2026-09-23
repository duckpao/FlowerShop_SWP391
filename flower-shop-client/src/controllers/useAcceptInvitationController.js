import { useState } from 'react'
import { authService } from '../services/authService'
export function useAcceptInvitationController() {
  const [code, setCode] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [done, setDone] = useState(false)
  async function accept(e) {
    e.preventDefault(); setBusy(true); setError('')
    try {
      await authService.authenticatedRequest('/api/account/staff-invitations/accept', { method: 'POST', body: { code: code.trim() } })
      authService.clear(); setCode(''); setDone(true)
    } catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  return { code, setCode, busy, error, done, accept }
}
