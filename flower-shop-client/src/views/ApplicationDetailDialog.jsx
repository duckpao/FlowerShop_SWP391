import { useEffect, useRef } from 'react'
import { createPortal } from 'react-dom'
import '../styles/application-dialog.css'

export default function ApplicationDetailDialog({ application: a, controller: c }) {
  const ref = useRef(null)
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
  const close = () => { if (!c.busy) c.setSelectedId(null) }
  return createPortal(<dialog ref={ref} className="application-dialog" aria-labelledby="application-dialog-title"
    onCancel={e => { e.preventDefault(); close() }}
    onClick={e => {
      if (e.target !== e.currentTarget) return
      const bounds = e.currentTarget.getBoundingClientRect()
      if (e.clientX < bounds.left || e.clientX > bounds.right || e.clientY < bounds.top || e.clientY > bounds.bottom) close()
    }}>
    <header className="application-dialog-header"><div><h2 id="application-dialog-title">Chi tiết đơn đăng ký mở shop</h2><p>{a.shopName}</p></div>
      <button type="button" autoFocus disabled={c.busy} onClick={close} aria-label="Đóng chi tiết đơn">✕</button>
    </header>
    <div className="application-dialog-body">
      {c.error && <p role="alert" className="message error">{c.error}</p>}
      <p><strong>Trạng thái:</strong> {({ PENDING: 'Chờ duyệt', APPROVED: 'Đã duyệt', REJECTED: 'Đã từ chối' })[a.status]}</p>
      <p><strong>Mã đơn:</strong> {a.id}</p><p><strong>Ngày gửi:</strong> {new Date(a.submittedAt).toLocaleString('vi-VN')}</p>
      <p><strong>Họ và tên:</strong> {a.fullName}</p><p><strong>Email:</strong> {a.email}</p><p><strong>Số điện thoại:</strong> {a.phone}</p>
      <p className="application-description"><strong>Mô tả shop:</strong> {a.description}</p><p><strong>Địa chỉ:</strong> {[a.addressLine, a.ward, a.district, a.city].join(', ')}</p>
      {a.reviewNote && <p className="application-description"><strong>Phản hồi của Admin:</strong> {a.reviewNote}</p>}
      {a.status === 'PENDING' && <label>Ghi chú phản hồi<textarea disabled={c.busy} maxLength={1000} value={c.notes[a.id] || ''} onChange={e => c.setNotes({ ...c.notes, [a.id]: e.target.value })} /></label>}
    </div>
    <footer className="application-dialog-footer"><button disabled={c.busy} onClick={close}>Đóng</button>
      {a.status === 'PENDING' && <><button disabled={c.busy} onClick={() => c.decide(a.id, false)}>Từ chối</button><button className="application-approve" disabled={c.busy} onClick={() => c.decide(a.id, true)}>{c.busy ? 'Đang xử lý…' : 'Duyệt và tạo shop'}</button></>}
    </footer>
  </dialog>, document.body)
}
