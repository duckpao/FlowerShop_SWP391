import { useState } from 'react'
import DetailDialog from './DetailDialog'

export default function InfoPopup({ title, label = 'Xem chi tiết', children }) {
  const [open, setOpen] = useState(false)
  return <>
    <button type="button" aria-haspopup="dialog" onClick={() => setOpen(true)}>{label}</button>
    {open && <DetailDialog title={title} onClose={() => setOpen(false)}>{children}</DetailDialog>}
  </>
}
