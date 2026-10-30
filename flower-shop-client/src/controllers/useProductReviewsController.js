import { useEffect, useState } from 'react'
import { reviewService } from '../services/reviewService'
import { useConfirm } from '../components/ui/confirm/ConfirmProvider'

const emptyDraft = () => ({ rating: 5, comment: '' })

export function useProductReviewsController(productId, user) {
  const { confirm } = useConfirm()
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
        // Saved content is shown in the review list; the composer is a fresh draft.
        setDraft(value ? { rating: value.rating, comment: '' } : emptyDraft())
      })
      .catch(() => { if (active) setOwn(null) })
    return () => { active = false }
  }, [productId, user?.id, user?.role])

  async function run(action, message) {
    setBusy(true); setError(''); setNotice('')
    try { await action(); setNotice(message); setRevision(x => x + 1) }
    catch (e) { setError(e.message) } finally { setBusy(false) }
  }

  const submit = event => {
    event.preventDefault()
    if (busy) return
    const body = { rating: Number(draft.rating), comment: draft.comment.trim() }
    const updating = Boolean(own)
    return run(async () => {
      const saved = await (updating
        ? reviewService.update(productId, body)
        : reviewService.write(productId, body))
      setOwn(saved)
      setDraft({ rating: saved.rating, comment: '' })
      if (!updating) setPage(0)
    }, updating ? 'Đã cập nhật đánh giá.' : 'Cảm ơn bạn đã đánh giá.')
  }

  const remove = async () => {
    if (!await confirm({ title: 'Xóa đánh giá', message: 'Xóa đánh giá của bạn?', confirmLabel: 'Xóa', tone: 'danger' })) return
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
