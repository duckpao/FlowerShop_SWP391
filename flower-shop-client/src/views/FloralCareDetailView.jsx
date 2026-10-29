import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router'
import PageMeta from '../components/common/PageMeta'
import { blogService } from '../services/blogService'

export default function FloralCareDetailView() {
  const { id } = useParams()
  const [post, setPost] = useState(null)
  const [error, setError] = useState('')
  useEffect(() => {
    let active = true
    blogService.publicDetail(id).then(value => { if (active) setPost(value) }).catch(e => { if (active) setError(e.message) })
    return () => { active = false }
  }, [id])
  if (error) return <main className="mx-auto max-w-3xl px-4 py-12"><p role="alert" className="text-error-600">{error}</p><Link to="/floral-care" className="mt-4 inline-block text-brand-500">Quay lại cẩm nang</Link></main>
  if (!post) return <p role="status" className="p-10 text-center">Đang tải bài viết…</p>
  return <main className="mx-auto max-w-3xl px-4 py-10">
    <PageMeta title={`${post.title} | FlowerShop`} description={post.content.slice(0, 150)} />
    <Link to="/floral-care" className="text-sm font-medium text-brand-500">← Cẩm nang chăm hoa</Link>
    <article className="mt-6"><Link to={`/shops/${post.shop.id}`} className="text-sm font-semibold text-brand-500">{post.shop.name}</Link><h1 className="mt-2 text-3xl font-bold leading-tight text-gray-900 dark:text-white">{post.title}</h1><time className="mt-3 block text-sm text-gray-500">{new Date(post.createdDate).toLocaleDateString('vi-VN')}</time>
      {post.images[0] && <img src={post.images[0]} alt="" className="mt-8 max-h-[34rem] w-full rounded-2xl object-cover" />}
      <div className="mt-8 whitespace-pre-wrap text-base leading-8 text-gray-700 dark:text-gray-300">{post.content}</div>
      {post.images.slice(1).length > 0 && <div className="mt-8 grid gap-4 sm:grid-cols-2">{post.images.slice(1).map((url, index) => <img key={`${url}-${index}`} src={url} alt="" className="h-64 w-full rounded-xl object-cover" />)}</div>}
    </article>
  </main>
}
