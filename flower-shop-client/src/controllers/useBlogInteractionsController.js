import { useEffect, useState } from 'react'
import { blogService } from '../services/blogService'
import { useConfirm } from '../components/ui/confirm/ConfirmProvider'

export function useBlogInteractionsController(blogId, user, shopId) {
  const { confirm } = useConfirm()
  const [engagement, setEngagement] = useState({ likeCount: 0, commentCount: 0 })
  const [interactions, setInteractions] = useState({ liked: false, saved: false })
  const [comments, setComments] = useState(null)
  const [commentDraft, setCommentDraft] = useState('')
  const [editor, setEditor] = useState(null)
  const [page, setPage] = useState(0)
  const [revision, setRevision] = useState(0)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const isCustomer = user?.role === 'CUSTOMER'
  const isManager = Boolean(shopId)

  useEffect(() => {
    setPage(0)
    setEditor(null)
    setCommentDraft('')
    setError('')
    setNotice('')
  }, [blogId, shopId, user?.id])

  useEffect(() => {
    let active = true
    setLoading(true)
    Promise.all(isManager
      ? [blogService.managedEngagement(shopId, blogId), blogService.managedComments(shopId, blogId, page)]
      : [blogService.engagement(blogId), blogService.comments(blogId, page)])
      .then(([engagementResult, commentResult]) => {
        if (!active) return
        setEngagement(engagementResult)
        setComments(commentResult)
        setError('')
      })
      .catch(requestError => { if (active) setError(requestError.message) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [blogId, shopId, isManager, page, revision])

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
  }, [blogId, isCustomer, user?.id])

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
    const isThread = comments?.content.some(comment => comment.id === commentId)
    const accepted = await confirm({
      title: 'Xóa bình luận',
      message: isThread ? 'Xóa bình luận này và các phản hồi bên dưới?' : 'Bạn có chắc muốn xóa phản hồi này?',
      confirmLabel: 'Xóa',
      tone: 'danger',
    })
    if (!accepted) return
    return run(async () => {
      if (isManager) await blogService.moderateComment(shopId, blogId, commentId)
      else await blogService.deleteComment(blogId, commentId)
      setEditor(null)
      if (isThread && comments.content.length === 1 && page > 0) setPage(value => value - 1)
      setRevision(value => value + 1)
    }, 'Đã xóa bình luận.')
  }

  const startEdit = comment => {
    setError(''); setNotice('')
    setEditor({ id: comment.id, mode: 'edit', draft: comment.content })
  }
  const startReply = comment => {
    setError(''); setNotice('')
    setEditor({ id: comment.id, mode: 'reply', draft: '' })
  }
  const submitEditor = event => {
    event.preventDefault()
    const content = editor?.draft.trim()
    if (!content || content.length > 2000) {
      setError('Vui lòng nhập nội dung từ 1 đến 2000 ký tự.')
      return
    }
    return run(async () => {
      if (editor.mode === 'reply') await blogService.replyComment(shopId, blogId, editor.id, content)
      else await blogService.updateComment(blogId, editor.id, content)
      setEditor(null)
      setRevision(value => value + 1)
    }, editor.mode === 'reply' ? 'Đã trả lời bình luận.' : 'Đã cập nhật bình luận.')
  }

  return {
    engagement, interactions, comments, commentDraft, setCommentDraft,
    page, setPage, loading, busy, error, notice, isCustomer, isManager,
    editor, startEdit, startReply, submitEditor,
    setEditorDraft: draft => setEditor(current => current ? { ...current, draft } : null),
    cancelEditor: () => setEditor(null),
    toggleLike, toggleSaved, submitComment, deleteComment,
  }
}
