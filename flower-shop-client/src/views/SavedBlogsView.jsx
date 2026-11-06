import { useEffect, useState } from 'react'
import { BookmarkX } from 'lucide-react'
import { Link } from 'react-router'
import PageMeta from '../components/common/PageMeta'
import { blogService } from '../services/blogService'

export default function SavedBlogsView({ auth }) {
  const [data, setData] = useState(null)
  const [page, setPage] = useState(0)
  const [revision, setRevision] = useState(0)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const user = auth?.user
  const isCustomer = user?.role === 'CUSTOMER'

  useEffect(() => {
    if (!isCustomer) return
    let active = true
    setBusy(true)
    blogService.saved(page)
      .then(result => { if (active) { setData(result); setError('') } })
      .catch(requestError => { if (active) setError(requestError.message) })
      .finally(() => { if (active) setBusy(false) })
    return () => { active = false }
  }, [isCustomer, page, revision])

  const remove = async blogId => {
    setBusy(true)
    setError('')
    try {
      await blogService.setSaved(blogId, false)
      if (data?.content.length === 1 && page > 0) setPage(value => value - 1)
      else setRevision(value => value + 1)
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setBusy(false)
    }
  }

  if (auth?.busy && !user) return <main className="mx-auto max-w-6xl px-4 py-12"><p role="status">Đang kiểm tra đăng nhập…</p></main>

  if (!user) return <main className="mx-auto max-w-3xl px-4 py-12 text-center">
    <PageMeta title="Bài viết đã lưu | FlowerShop" description="Danh sách bài viết chăm sóc hoa đã lưu" />
    <h1 className="text-3xl font-bold text-gray-900 dark:text-white">Bài viết đã lưu</h1>
    <p className="mt-4 text-gray-500">Hãy đăng nhập bằng tài khoản Khách hàng để xem các bài viết bạn đã lưu.</p>
    <Link to="/login?next=%2Fsaved-blogs" className="mt-6 inline-block rounded-lg bg-brand-500 px-5 py-2.5 font-medium text-white">Đăng nhập</Link>
  </main>

  if (!isCustomer) return <main className="mx-auto max-w-3xl px-4 py-12 text-center">
    <h1 className="text-3xl font-bold text-gray-900 dark:text-white">Bài viết đã lưu</h1>
    <p className="mt-4 text-gray-500">Chỉ tài khoản Khách hàng mới có danh sách bài viết đã lưu.</p>
    <Link to="/floral-care" className="mt-5 inline-block font-medium text-brand-500">Về cẩm nang chăm hoa</Link>
  </main>

  return <main className="mx-auto w-full max-w-6xl space-y-7 px-4 py-8">
    <PageMeta title="Bài viết đã lưu | FlowerShop" description="Danh sách bài viết chăm sóc hoa đã lưu" />
    <header className="flex flex-col justify-between gap-4 rounded-2xl bg-linear-to-br from-pink-50 to-green-50 p-8 sm:flex-row sm:items-end dark:from-pink-500/10 dark:to-green-500/10">
      <div>
        <p className="text-sm font-semibold uppercase tracking-widest text-brand-500">Cẩm nang của bạn</p>
        <h1 className="mt-2 text-3xl font-bold text-gray-900 dark:text-white">Bài viết đã lưu</h1>
        <p className="mt-2 text-gray-600 dark:text-gray-300">Xem lại những hướng dẫn và câu chuyện bạn quan tâm.</p>
      </div>
      <Link to="/floral-care" className="shrink-0 rounded-lg border border-brand-200 bg-white px-4 py-2 text-sm font-medium text-brand-600 hover:bg-brand-50 dark:border-brand-500/30 dark:bg-gray-900">Khám phá bài viết</Link>
    </header>

    {error && <div role="alert" className="rounded-lg bg-error-50 p-4 text-sm text-error-600 dark:bg-error-500/10">{error}</div>}
    {busy && !data && <p role="status" className="text-gray-500">Đang tải bài viết đã lưu…</p>}
    {data && <p className="text-sm text-gray-500">{data.totalElements} bài viết đã lưu</p>}

    <section className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
      {data?.content.map(post => <article key={post.id} className="overflow-hidden rounded-2xl border border-gray-200 bg-white shadow-sm transition hover:-translate-y-0.5 hover:shadow-md dark:border-gray-800 dark:bg-white/3">
        <Link to={`/floral-care/${post.id}`} className="block">
          {post.images[0]
            ? <img src={post.images[0]} alt="" className="h-48 w-full object-cover" />
            : <div className="flex h-48 items-center justify-center bg-pink-50 text-5xl dark:bg-pink-500/10">✿</div>}
        </Link>
        <div className="space-y-3 p-5">
          <Link to={`/shops/${post.shop.id}`} className="text-xs font-semibold uppercase tracking-wide text-brand-500">{post.shop.name}</Link>
          <h2 className="text-lg font-bold text-gray-900 dark:text-white"><Link to={`/floral-care/${post.id}`}>{post.title}</Link></h2>
          <p className="line-clamp-3 whitespace-pre-line text-sm text-gray-500 dark:text-gray-400">{post.content}</p>
          <div className="flex items-center justify-between gap-3 pt-2">
            <Link to={`/floral-care/${post.id}`} className="text-sm font-semibold text-brand-500">Đọc tiếp →</Link>
            <button type="button" disabled={busy} onClick={() => remove(post.id)} className="inline-flex items-center gap-1 rounded-lg p-2 text-sm text-gray-500 hover:bg-error-50 hover:text-error-500 disabled:opacity-50" aria-label={`Bỏ lưu ${post.title}`}>
              <BookmarkX size={17} /> Bỏ lưu
            </button>
          </div>
        </div>
      </article>)}
    </section>

    {data && data.content.length === 0 && <div className="rounded-2xl border border-dashed border-gray-300 px-6 py-14 text-center dark:border-gray-700">
      <p className="text-gray-500">Bạn chưa lưu bài viết nào.</p>
      <Link to="/floral-care" className="mt-4 inline-block font-semibold text-brand-500">Khám phá cẩm nang chăm hoa →</Link>
    </div>}

    {data && data.totalPages > 1 && <nav className="flex items-center justify-center gap-3">
      <button type="button" disabled={busy || page === 0} onClick={() => setPage(value => value - 1)} className="rounded-lg border border-gray-200 px-4 py-2 text-sm disabled:opacity-40 dark:border-gray-700">Trang trước</button>
      <span className="text-sm text-gray-500">Trang {page + 1}/{data.totalPages}</span>
      <button type="button" disabled={busy || page + 1 >= data.totalPages} onClick={() => setPage(value => value + 1)} className="rounded-lg border border-gray-200 px-4 py-2 text-sm disabled:opacity-40 dark:border-gray-700">Trang sau</button>
    </nav>}
  </main>
}
