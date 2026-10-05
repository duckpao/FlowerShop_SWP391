import { useEffect, useId, useRef } from 'react'
import { createPortal } from 'react-dom'
import '../styles/application-dialog.css'

export default function DetailDialog({ title, onClose, children }) {
  const ref = useRef(null)
  const titleId = useId()
  useEffect(() => {
    const dialog = ref.current
    const previousFocus = document.activeElement
    const overflow = document.body.style.overflow
    dialog.showModal()
    document.body.style.overflow = 'hidden'
    return () => {
      dialog.close()
      document.body.style.overflow = overflow
      if (previousFocus?.isConnected) previousFocus.focus()
    }
  }, [])
  return createPortal(<dialog ref={ref} className="application-dialog" aria-labelledby={titleId}
    onCancel={event => { event.preventDefault(); onClose() }}
    onClick={event => {
      if (event.target !== event.currentTarget) return
      const bounds = event.currentTarget.getBoundingClientRect()
      if (event.clientX < bounds.left || event.clientX > bounds.right || event.clientY < bounds.top || event.clientY > bounds.bottom) onClose()
    }}>
    <header className="application-dialog-header"><h2 id={titleId}>{title}</h2>
      <button type="button" autoFocus onClick={onClose} aria-label="Đóng chi tiết">✕</button>
    </header>
    <div className="application-dialog-body">{children}</div>
    <footer className="application-dialog-footer"><button type="button" onClick={onClose}>Đóng chi tiết</button></footer>
  </dialog>, document.body)
}
