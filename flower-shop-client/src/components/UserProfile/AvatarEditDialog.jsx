import { useEffect, useRef, useState } from 'react'
import { accountService } from '../../services/accountService'
import AvatarUpload from './AvatarUpload'

export default function AvatarEditDialog({ user, onClose }) {
  const ref = useRef(null)
  const [profile, setProfile] = useState(user)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  useEffect(() => {
    const dialog = ref.current
    const previousOverflow = document.body.style.overflow
    dialog.showModal()
    document.body.style.overflow = 'hidden'
    return () => { dialog.close(); document.body.style.overflow = previousOverflow }
  }, [])
  async function upload(file) {
    setBusy(true); setError('')
    try {
      setProfile(await accountService.uploadAvatar(file))
      onClose()
      return true
    } catch (failure) { setError(failure.message); return false }
    finally { setBusy(false) }
  }
  return <dialog ref={ref} aria-labelledby="avatar-dialog-title"
    onCancel={e => { e.preventDefault(); if (!busy) onClose() }}
    className="m-auto max-h-[90dvh] w-[calc(100%-2rem)] max-w-md overflow-y-auto rounded-2xl border-0 bg-white p-6 text-gray-900 shadow-xl backdrop:bg-black/50 dark:bg-gray-800 dark:text-white">
    <div className="mb-5 flex items-center justify-between gap-4">
      <h2 id="avatar-dialog-title" className="text-lg font-semibold">Cập nhật ảnh đại diện</h2>
      <button type="button" disabled={busy} aria-label="Đóng" onClick={onClose} className="rounded px-3 py-1 text-xl disabled:opacity-50">×</button>
    </div>
    {error && <p role="alert" className="mb-4 text-sm text-error-500">{error}</p>}
    <AvatarUpload profile={profile} busy={busy} upload={upload} />
  </dialog>
}
