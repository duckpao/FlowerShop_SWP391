import { useEffect, useState } from 'react'
import { reviewService } from '../services/reviewService'

const emptyDraft = () => ({ rating: 5, comment: '' })

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
    reviewService.list(productId, page)
      .then(value => { if (active) setData(value) })
      .catch(e => { if (active) setError(e.message) })
      .finally(() => { if (active) setBusy(false) })
    return () => { active = false }
  }, [productId, page, revision])

  useEffect(() => {
    if (user?.role !== 'CUSTOMER') { setOwn(null); setDraft(emptyDraft()); return }
    let active = true
    reviewService.own(productId)
      .then(value => {
        if (!active) return
        setOwn(value)
        setDraft(value ? { rating: value.rating, comment: value.comment || '' } : emptyDraft())
      })
      .catch(() => { if (active) setOwn(null) })
    return () => { active = false }
  }, [productId, user, revision])

  async function run(action, message) {
    setBusy(true); setError(''); setNotice('')
    try { await action(); setNotice(message); setRevision(x => x + 1) }
    catch (e) { setError(e.message) } finally { setBusy(false) }
  }

  const submit = event => {
    event.preventDefault()
    const body = { rating: Number(draft.rating), comment: draft.comment.trim() }
    return own
      ? run(() => reviewService.update(productId, body), 'Đã cập nhật đánh giá.')
      : run(() => reviewService.write(productId, body), 'Cảm ơn bạn đã đánh giá.')
  }

  const remove = () => {
    if (!window.confirm('Xóa đánh giá của bạn?')) return
    return run(async () => { await reviewService.remove(productId); setOwn(null); setDraft(emptyDraft()) },
      'Đã xóa đánh giá.')
  }

  const reply = (shopId, reviewId) =>
    run(() => reviewService.reply(shopId, reviewId, replies[reviewId] ?? ''), 'Đã gửi phản hồi của shop.')

  return {
    data, own, draft, setDraft, replies, setReplies, page, setPage, busy, error, notice,
    submit, remove, reply, reload: () => setRevision(x => x + 1),
  }
}
