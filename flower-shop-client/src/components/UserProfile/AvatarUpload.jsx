import { useEffect, useRef, useState } from 'react'

export default function AvatarUpload({ profile, busy, upload }) {
  const input = useRef(null)
  const [file, setFile] = useState(null)
  const [preview, setPreview] = useState('')
  const [error, setError] = useState('')
  useEffect(() => {
    if (!file) { setPreview(''); return }
    const url = URL.createObjectURL(file)
    setPreview(url)
    return () => URL.revokeObjectURL(url)
  }, [file])
  function choose(event) {
    const selected = event.target.files?.[0]
    event.target.value = ''
    if (!selected) return
    setError('')
    if (!['image/jpeg', 'image/png'].includes(selected.type)) {
      setError('Vui lòng chọn ảnh JPG hoặc PNG.'); return
    }
    if (!selected.size || selected.size > 5 * 1024 * 1024) {
      setError('Ảnh phải có dung lượng từ 1 byte đến 5 MB.'); return
    }
    setFile(selected)
  }
  async function save() {
    if (!file || busy) return
    if (await upload(file)) setFile(null)
  }
  const image = preview || profile.avatarUrl
  return <div className="space-y-3 border-b border-gray-100 pb-5 dark:border-gray-700">
    <div className="flex items-center gap-4">
      {image ? <img src={image} alt={preview ? 'Xem trước ảnh đại diện' : 'Ảnh đại diện'} className="h-20 w-20 shrink-0 rounded-full border border-gray-200 object-cover" />
        : <div aria-label="Chưa có ảnh đại diện" className="flex h-20 w-20 shrink-0 items-center justify-center rounded-full bg-brand-50 text-2xl font-semibold text-brand-600">{profile.fullName?.trim().charAt(0).toUpperCase() || '?'}</div>}
      <div className="space-y-2">
        <p className="text-sm font-medium text-gray-900 dark:text-white">Ảnh đại diện</p>
        <input ref={input} className="hidden" type="file" accept="image/jpeg,image/png" disabled={busy} onChange={choose} aria-label="Chọn ảnh đại diện" />
        <button type="button" disabled={busy} onClick={() => input.current.click()} className="rounded-lg border border-gray-300 px-3 py-2 text-sm font-medium text-gray-700 disabled:opacity-50 dark:text-gray-200">Chọn ảnh</button>
      </div>
    </div>
    <p className="text-xs text-gray-500">JPG hoặc PNG, tối đa 5 MB và 4096 × 4096 pixel.</p>
    {error && <p role="alert" className="text-sm text-error-500">{error}</p>}
    {file && <div className="flex flex-wrap items-center gap-3">
      <button type="button" disabled={busy} onClick={save} className="rounded-lg bg-brand-500 px-4 py-2 text-sm font-medium text-white disabled:opacity-50">{busy ? 'Đang tải ảnh…' : 'Tải ảnh lên'}</button>
      <button type="button" disabled={busy} onClick={() => { setFile(null); setError('') }} className="text-sm text-gray-500 disabled:opacity-50">Hủy</button>
    </div>}
  </div>
}
