import { useEffect, useRef } from 'react'
import PersonalAddressForm from './PersonalAddressForm'

export default function AddressEditDialog({ controller: c, busy }) {
  const ref = useRef(null)
  useEffect(() => {
    const dialog = ref.current
    const previousOverflow = document.body.style.overflow
    dialog.showModal()
    document.body.style.overflow = 'hidden'
    return () => {
      dialog.close()
      document.body.style.overflow = previousOverflow
    }
  }, [])

  return <dialog ref={ref} aria-labelledby="address-edit-title"
    onCancel={event => { event.preventDefault(); if (!busy) c.cancel() }}
    className="m-auto max-h-[90dvh] w-[calc(100%-2rem)] max-w-lg overflow-y-auto rounded-2xl border-0 bg-white p-4 text-gray-900 shadow-xl backdrop:bg-black/50 dark:bg-gray-800 dark:text-white sm:p-6">
    <div className="mb-4 flex items-center justify-between gap-4">
      <h2 id="address-edit-title" className="text-xl font-semibold">Cập nhật địa chỉ</h2>
      <button type="button" disabled={busy} onClick={c.cancel} aria-label="Đóng dialog sửa địa chỉ"
        className="rounded-lg px-3 py-2 text-xl text-gray-500 hover:bg-gray-100 disabled:opacity-50 dark:hover:bg-gray-700">×</button>
    </div>
    {c.error && <p role="alert" className="mb-3 rounded-lg bg-error-50 p-3 text-sm text-error-600 dark:bg-error-500/10">{c.error}</p>}
    {busy && <p role="status" className="mb-3 text-sm text-gray-500">Đang lưu địa chỉ…</p>}
    <PersonalAddressForm controller={c} busy={busy} />
  </dialog>
}
