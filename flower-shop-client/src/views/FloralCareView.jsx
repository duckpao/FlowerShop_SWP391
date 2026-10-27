import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import PageMeta from '../components/common/PageMeta'
import { blogService } from '../services/blogService'

export default function FloralCareView() {
  const [params, setParams] = useSearchParams()
  const [query, setQuery] = useState(params.get('q') || '')
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  const page = Math.max(0, Number(params.get('page')) || 0)
  const q = params.get('q') || ''
  const shopId = params.get('shopId') || ''

  useEffect(() => {
    let active = true
    blogService.publicList({ shopId, q, page }).then(value => { if (active) { setData(value); setError('') } }).catch(e => { if (active) setError(e.message) })
    return () => { active = false }
  }, [shopId, q, page])

  const search = event => { event.preventDefault(); const next = {}; if (shopId) next.shopId = shopId; if (query.trim()) next.q = query.trim(); setParams(next) }
  const go = nextPage => { const next = {}; if (shopId) next.shopId = shopId; if (q) next.q = q; if (nextPage) next.page = nextPage; setParams(next) }

  return <main className="mx-auto w-full max-w-6xl space-y-7 px-4 py-8">
    <PageMeta title="Chăm sóc hoa | FlowerShop" description="Mẹo chăm sóc hoa và câu chuyện từ các cửa hàng" />
    <header className="rounded-2xl bg-linear-to-br from-pink-50 to-green-50 p-8 dark:from-pink-500/10 dark:to-green-500/10">
      <p className="text-sm font-semibold uppercase tracking-widest text-brand-500">Cẩm nang FlowerShop</p>
      <h1 className="mt-2 text-3xl font-bold text-gray-900 dark:text-white">Chăm hoa đẹp lâu, trao yêu thương dài lâu</h1>
      <p className="mt-3 max-w-2xl text-gray-600 dark:text-gray-300">Khám phá hướng dẫn chăm sóc hoa, ý tưởng quà tặng và thông tin mới từ các cửa hàng.</p>
      <form onSubmit={search} className="mt-6 flex max-w-xl gap-2"><input value={query} onChange={e => setQuery(e.target.value)} maxLength={100} placeholder="Tìm bài viết…" className="min-w-0 flex-1 rounded-lg border border-white bg-white px-4 py-2.5 shadow-sm dark:border-gray-700 dark:bg-gray-900" /><button className="rounded-lg bg-brand-500 px-5 py-2.5 font-medium text-white">Tìm</button></form>
    </header>
    {error && <p role="alert" className="rounded-lg bg-error-50 p-3 text-error-600">{error}</p>}
    {!data && !error && <p role="status">Đang tải bài viết…</p>}
    <section className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
      {data?.content.map(post => <article key={post.id} className="overflow-hidden rounded-2xl border border-gray-200 bg-white shadow-sm transition hover:-translate-y-0.5 hover:shadow-md dark:border-gray-800 dark:bg-white/3">
        {post.images[0] ? <img src={post.images[0]} alt="" className="h-48 w-full object-cover" /> : <div className="flex h-48 items-center justify-center bg-pink-50 text-5xl dark:bg-pink-500/10">✿</div>}
        <div className="space-y-3 p-5"><Link to={`/shops/${post.shop.id}`} className="text-xs font-semibold uppercase tracking-wide text-brand-500">{post.shop.name}</Link><h2 className="text-lg font-bold text-gray-900 dark:text-white"><Link to={`/floral-care/${post.id}`}>{post.title}</Link></h2><p className="line-clamp-3 whitespace-pre-line text-sm text-gray-500 dark:text-gray-400">{post.content}</p><Link to={`/floral-care/${post.id}`} className="inline-block text-sm font-semibold text-brand-500">Đọc tiếp →</Link></div>
      </article>)}
    </section>
    {data && !data.content.length && <p className="py-12 text-center text-gray-500">Chưa có bài viết phù hợp.</p>}
    {(data?.totalPages || 0) > 1 && <nav className="flex justify-center gap-3"><button disabled={page === 0} onClick={() => go(page - 1)} className="rounded-lg border px-4 py-2 disabled:opacity-40">Trước</button><span className="py-2 text-sm">Trang {page + 1}/{data.totalPages}</span><button disabled={page + 1 >= data.totalPages} onClick={() => go(page + 1)} className="rounded-lg border px-4 py-2 disabled:opacity-40">Sau</button></nav>}
  </main>
}
