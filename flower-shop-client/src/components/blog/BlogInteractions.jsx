import { Bookmark, Heart, MessageCircle, Trash2 } from 'lucide-react'
import { Link } from 'react-router'
import { useBlogInteractionsController } from '../../controllers/useBlogInteractionsController'

export default function BlogInteractions({ blogId, auth }) {
  const user = auth?.user
  const c = useBlogInteractionsController(blogId, user)
  const loginUrl = `/login?next=${encodeURIComponent(`/floral-care/${blogId}`)}`

  return <section className="mt-10 border-t border-gray-200 pt-8 dark:border-gray-800" aria-labelledby="blog-interactions-heading">
    <h2 id="blog-interactions-heading" className="text-xl font-bold text-gray-900 dark:text-white">Tương tác với bài viết</h2>

    <div className="mt-4 flex flex-wrap items-center gap-3">
      {c.isCustomer ? <>
        <button
          type="button"
          disabled={c.busy}
          onClick={c.toggleLike}
          aria-pressed={c.interactions.liked}
          className={`inline-flex items-center gap-2 rounded-lg border px-4 py-2 text-sm font-medium transition disabled:opacity-50 ${c.interactions.liked ? 'border-rose-200 bg-rose-50 text-rose-600 dark:border-rose-500/30 dark:bg-rose-500/10' : 'border-gray-200 text-gray-700 hover:bg-gray-50 dark:border-gray-700 dark:text-gray-300 dark:hover:bg-gray-800'}`}
        >
          <Heart size={18} fill={c.interactions.liked ? 'currentColor' : 'none'} />
          {c.interactions.liked ? 'Đã thích' : 'Thích'} · {c.engagement.likeCount}
        </button>
        <button
          type="button"
          disabled={c.busy}
          onClick={c.toggleSaved}
          aria-pressed={c.interactions.saved}
          className={`inline-flex items-center gap-2 rounded-lg border px-4 py-2 text-sm font-medium transition disabled:opacity-50 ${c.interactions.saved ? 'border-brand-200 bg-brand-50 text-brand-600 dark:border-brand-500/30 dark:bg-brand-500/10' : 'border-gray-200 text-gray-700 hover:bg-gray-50 dark:border-gray-700 dark:text-gray-300 dark:hover:bg-gray-800'}`}
        >
          <Bookmark size={18} fill={c.interactions.saved ? 'currentColor' : 'none'} />
          {c.interactions.saved ? 'Đã lưu' : 'Lưu bài viết'}
        </button>
      </> : <>
        <span className="inline-flex items-center gap-2 rounded-lg border border-gray-200 px-4 py-2 text-sm text-gray-600 dark:border-gray-700 dark:text-gray-300">
          <Heart size={18} /> {c.engagement.likeCount} lượt thích
        </span>
        {!user && <Link to={loginUrl} className="text-sm font-medium text-brand-500 hover:underline">Đăng nhập để thích và lưu bài viết</Link>}
      </>}
      <span className="inline-flex items-center gap-2 text-sm text-gray-500">
        <MessageCircle size={18} /> {c.engagement.commentCount} bình luận
      </span>
    </div>

    {c.error && <p role="alert" className="mt-4 rounded-lg bg-error-50 p-3 text-sm text-error-600 dark:bg-error-500/10">{c.error}</p>}
    {c.notice && <p role="status" className="mt-4 rounded-lg bg-success-50 p-3 text-sm text-success-600 dark:bg-success-500/10">{c.notice}</p>}

    <div className="mt-8">
      <h3 className="text-lg font-semibold text-gray-900 dark:text-white">Bình luận</h3>
      {c.isCustomer && <form onSubmit={c.submitComment} className="mt-4">
        <label htmlFor="blog-comment" className="sr-only">Nội dung bình luận</label>
        <textarea
          id="blog-comment"
          maxLength={2000}
          rows={3}
          value={c.commentDraft}
          onChange={event => c.setCommentDraft(event.target.value)}
          placeholder="Chia sẻ cảm nhận của bạn về bài viết..."
          className="w-full rounded-xl border border-gray-300 px-4 py-3 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-gray-700 dark:bg-gray-900 dark:text-white"
        />
        <div className="mt-2 flex items-center justify-between gap-3">
          <span className="text-xs text-gray-400">{c.commentDraft.length}/2000</span>
          <button type="submit" disabled={c.busy || !c.commentDraft.trim()} className="rounded-lg bg-brand-500 px-4 py-2 text-sm font-medium text-white hover:bg-brand-600 disabled:cursor-not-allowed disabled:opacity-50">
            Đăng bình luận
          </button>
        </div>
      </form>}
      {!user && <p className="mt-4 text-sm text-gray-500"><Link to={loginUrl} className="font-medium text-brand-500 hover:underline">Đăng nhập</Link> để bình luận.</p>}
      {user && !c.isCustomer && <p className="mt-4 text-sm text-gray-500">Chỉ tài khoản Khách hàng mới có thể thích, lưu và bình luận bài viết.</p>}

      {c.loading && <p role="status" className="mt-6 text-sm text-gray-500">Đang tải bình luận…</p>}
      {!c.loading && c.comments?.content.length === 0 && <p className="mt-6 rounded-xl bg-gray-50 p-5 text-center text-sm text-gray-500 dark:bg-gray-900">Chưa có bình luận nào. Hãy là người đầu tiên bình luận.</p>}
      <ul className="mt-6 space-y-4">
        {c.comments?.content.map(comment => <li key={comment.id} className="rounded-xl border border-gray-200 p-4 dark:border-gray-800">
          <div className="flex items-start gap-3">
            {comment.authorAvatarUrl
              ? <img src={comment.authorAvatarUrl} alt="" className="h-10 w-10 rounded-full object-cover" />
              : <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-brand-50 font-semibold text-brand-600 dark:bg-brand-500/10">{comment.authorName.charAt(0).toUpperCase()}</span>}
            <div className="min-w-0 flex-1">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div>
                  <p className="font-medium text-gray-900 dark:text-white">{comment.authorName}</p>
                  <time className="text-xs text-gray-400">{new Date(comment.createdDate).toLocaleString('vi-VN')}</time>
                </div>
                {c.isCustomer && user.id === comment.authorId && <button type="button" disabled={c.busy} onClick={() => c.deleteComment(comment.id)} className="rounded-lg p-2 text-gray-400 hover:bg-error-50 hover:text-error-500 disabled:opacity-50" aria-label="Xóa bình luận">
                  <Trash2 size={17} />
                </button>}
              </div>
              <p className="mt-3 whitespace-pre-wrap text-sm leading-6 text-gray-700 dark:text-gray-300">{comment.content}</p>
            </div>
          </div>
        </li>)}
      </ul>

      {c.comments && c.comments.totalPages > 1 && <div className="mt-5 flex items-center justify-between gap-3">
        <button type="button" disabled={c.loading || c.page === 0} onClick={() => c.setPage(value => value - 1)} className="rounded-lg border border-gray-200 px-3 py-2 text-sm disabled:opacity-40 dark:border-gray-700">Trang trước</button>
        <span className="text-sm text-gray-500">Trang {c.page + 1}/{c.comments.totalPages}</span>
        <button type="button" disabled={c.loading || c.page + 1 >= c.comments.totalPages} onClick={() => c.setPage(value => value + 1)} className="rounded-lg border border-gray-200 px-3 py-2 text-sm disabled:opacity-40 dark:border-gray-700">Trang sau</button>
      </div>}
    </div>
  </section>
}
