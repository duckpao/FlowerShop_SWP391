import { useEffect, useState } from 'react'
import { blogService } from '../services/blogService'
import { useConfirm } from '../components/ui/confirm/ConfirmProvider'

export function useBlogInteractionsController(blogId, user) {
  const { confirm } = useConfirm()
  const [engagement, setEngagement] = useState({ likeCount: 0, commentCount: 0 })
  const [interactions, setInteractions] = useState({ liked: false, saved: false })
  const [comments, setComments] = useState(null)
  const [commentDraft, setCommentDraft] = useState('')
  const [page, setPage] = useState(0)
  const [revision, setRevision] = useState(0)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const isCustomer = user?.role === 'CUSTOMER'

  useEffect(() => {
    let active = true
    setLoading(true)
    Promise.all([blogService.engagement(blogId), blogService.comments(blogId, page)])
      .then(([engagementResult, commentResult]) => {
        if (!active) return
        setEngagement(engagementResult)
        setComments(commentResult)
        setError('')
      })
      .catch(requestError => { if (active) setError(requestError.message) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [blogId, page, revision])

  useEffect(() => {
    if (!isCustomer) {
      setInteractions({ liked: false, saved: false })
      return
    }
    let active = true
    blogService.interactions(blogId)
      .then(result => { if (active) setInteractions(result) })
      .catch(requestError => { if (active) setError(requestError.message) })
    return () => { active = false }
  }, [blogId, isCustomer])

  async function run(action, successMessage) {
    setBusy(true)
    setError('')
    setNotice('')
    try {
      await action()
      setNotice(successMessage)
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setBusy(false)
    }
  }

  const toggleLike = () => run(async () => {
    const wasLiked = interactions.liked
    const result = await blogService.setLike(blogId, !wasLiked)
    setInteractions(result)
    setEngagement(current => ({
      ...current,
      likeCount: Math.max(0, current.likeCount + (wasLiked ? -1 : 1)),
    }))
  }, interactions.liked ? 'Đã bỏ thích bài viết.' : 'Đã thích bài viết.')

  const toggleSaved = () => run(async () => {
    const result = await blogService.setSaved(blogId, !interactions.saved)
    setInteractions(result)
  }, interactions.saved ? 'Đã bỏ lưu bài viết.' : 'Đã lưu bài viết.')

  const submitComment = event => {
    event.preventDefault()
    const content = commentDraft.trim()
    if (!content) {
      setError('Vui lòng nhập nội dung bình luận.')
      return
    }
    return run(async () => {
      await blogService.addComment(blogId, content)
      setCommentDraft('')
      setPage(0)
      setRevision(value => value + 1)
    }, 'Đã đăng bình luận.')
  }

  const deleteComment = async commentId => {
    const accepted = await confirm({
      title: 'Xóa bình luận',
      message: 'Bạn có chắc muốn xóa bình luận này?',
      confirmLabel: 'Xóa',
      tone: 'danger',
    })
    if (!accepted) return
    return run(async () => {
      await blogService.deleteComment(blogId, commentId)
      setRevision(value => value + 1)
    }, 'Đã xóa bình luận.')
  }

  return {
    engagement, interactions, comments, commentDraft, setCommentDraft,
    page, setPage, loading, busy, error, notice, isCustomer,
    toggleLike, toggleSaved, submitComment, deleteComment,
  }
}
