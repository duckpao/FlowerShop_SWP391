import { useEffect, useState } from 'react'
import { reviewService } from '../services/reviewService'

const emptyDraft = () => ({ rating: 5, comment: '' })

// Review là luồng riêng trong màn hình chi tiết: data=danh sách chung, own=review của mình, draft=form.
export function useProductReviewsController(productId, user) {
  const [data, setData] = useState(null)
  const [own, setOwn] = useState(null)
  const [draft, setDraft] = useState(emptyDraft)
  const [replies, setReplies] = useState({})
  const [page, setPage] = useState(0)
  const [revision, setRevision] = useState(0)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  useEffect(() => {
    let active = true
    setBusy(true); setError('')
    // Đọc danh sách theo productId/page; revision đổi sau thao tác ghi để tải lại.
    reviewService.list(productId, page)
      .then(value => { if (active) setData(value) })
      .catch(e => { if (active) setError(e.message) })
      .finally(() => { if (active) setBusy(false) })
    return () => { active = false }
  }, [productId, page, revision])

  useEffect(() => {
    if (user?.role !== 'CUSTOMER') { setOwn(null); setDraft(emptyDraft()); return }
    let active = true
    // CUSTOMER đọc review của mình; service FE đổi 404 thành null để chuyển form sang chế độ viết mới.
    reviewService.own(productId)
      .then(value => {
        if (!active) return
        setOwn(value)
        setDraft(value ? { rating: value.rating, comment: value.comment || '' } : emptyDraft())
      })
      .catch(() => { if (active) setOwn(null) })
    return () => { active = false }
  }, [productId, user, revision])

  // Thao tác thành công -> tăng revision -> tải lại danh sách và own; không tải lại product ở hook cha.
  async function run(action, message) {
    setBusy(true); setError(''); setNotice('')
    try { await action(); setNotice(message); setRevision(x => x + 1) }
    catch (e) { setError(e.message) } finally { setBusy(false) }
  }

  // Có own thì PUT sửa; chưa có thì POST tạo. Chuyển rating sang số và cắt khoảng trắng comment.
  const submit = event => {
    event.preventDefault()
    const body = { rating: Number(draft.rating), comment: draft.comment.trim() }
    return own
      ? run(() => reviewService.update(productId, body), 'Đã cập nhật đánh giá.')
      : run(() => reviewService.write(productId, body), 'Cảm ơn bạn đã đánh giá.')
  }

  // DELETE review của mình; xóa draft/own tại FE rồi effect đồng bộ lại với BE.
  const remove = () => {
    if (!window.confirm('Xóa đánh giá của bạn?')) return
    return run(async () => { await reviewService.remove(productId); setOwn(null); setDraft(emptyDraft()) },
      'Đã xóa đánh giá.')
  }

  // PUT phản hồi shop; BE xác minh chủ shop và review thuộc sản phẩm của shop đó.
  const reply = (shopId, reviewId) =>
    run(() => reviewService.reply(shopId, reviewId, replies[reviewId] ?? ''), 'Đã gửi phản hồi của shop.')

  return {
    data, own, draft, setDraft, replies, setReplies, page, setPage, busy, error, notice,
    submit, remove, reply, reload: () => setRevision(x => x + 1),
  }
}
