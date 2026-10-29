import { Link } from 'react-router'
import PageMeta from '../components/common/PageMeta'
import PageBreadCrumb from '../components/common/PageBreadCrumb'
import { useBlogsController } from '../controllers/useBlogsController'

const statusLabel = { DRAFT: 'Bản nháp', PUBLISHED: 'Đã đăng', HIDDEN: 'Đã ẩn' }

function RemoveButton({ onClick }) {
  return <button type="button" onClick={onClick} aria-label="Gỡ ảnh" className="absolute right-1.5 top-1.5 flex h-7 w-7 items-center justify-center rounded-full bg-black/65 text-lg leading-none text-white hover:bg-error-500">×</button>
}

export default function ManagerBlogsView({ shop }) {
  const c = useBlogsController(shop?.id)
  const update = (key, value) => c.setForm(form => ({ ...form, [key]: value }))
  const imageCount = c.form.retainedImages.length + c.form.imageFiles.length

  return <div className="space-y-6">
    <PageMeta title="Bài chăm sóc hoa | FlowerShop" description="Quản lý nội dung chăm sóc hoa và quảng bá" />
    <PageBreadCrumb pageTitle="Bài chăm sóc hoa" />

    <div className="flex flex-wrap items-center justify-between gap-3">
      <p className="text-sm text-gray-500 dark:text-gray-400">Chia sẻ hướng dẫn chăm hoa và nội dung quảng bá của {shop?.name}.</p>
      <button type="button" onClick={c.openCreate} className="rounded-lg bg-brand-500 px-4 py-2.5 text-sm font-medium text-white hover:bg-brand-600">Viết bài mới</button>
    </div>

    {c.error && <p role="alert" className="rounded-lg bg-error-50 p-3 text-sm text-error-600 dark:bg-error-500/10">{c.error}</p>}
    {c.notice && <p role="status" className="rounded-lg bg-success-50 p-3 text-sm text-success-600 dark:bg-success-500/10">{c.notice}</p>}

    {c.editing !== null && <form onSubmit={c.save} className="space-y-4 rounded-xl border border-gray-200 bg-white p-5 shadow-sm dark:border-gray-800 dark:bg-white/3">
      <div className="flex items-center justify-between"><h2 className="font-semibold text-gray-900 dark:text-white">{c.editing ? 'Sửa bài viết' : 'Bài viết mới'}</h2><button type="button" onClick={c.close} className="text-sm text-gray-500">Đóng</button></div>
      <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">Tiêu đề
        <input required maxLength={255} value={c.form.title} onChange={e => update('title', e.target.value)} className="mt-1 w-full rounded-lg border border-gray-300 bg-transparent px-3 py-2 dark:border-gray-700" />
      </label>
      <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">Nội dung
        <textarea required maxLength={50000} rows={10} value={c.form.content} onChange={e => update('content', e.target.value)} className="mt-1 w-full rounded-lg border border-gray-300 bg-transparent px-3 py-2 dark:border-gray-700" placeholder="Hướng dẫn chăm sóc, câu chuyện về hoa hoặc nội dung khuyến mãi…" />
      </label>

      <div className="space-y-3">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div><p className="text-sm font-medium text-gray-700 dark:text-gray-300">Ảnh minh họa</p><p className="text-xs text-gray-500">JPEG, PNG hoặc WebP · tối đa 5MB/ảnh · tối đa 10 ảnh</p></div>
          <span className="text-xs text-gray-500">{imageCount}/10 ảnh</span>
        </div>
        <label className="flex min-h-28 cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed border-gray-300 bg-gray-50 px-4 py-5 text-center transition hover:border-brand-400 hover:bg-brand-50 dark:border-gray-700 dark:bg-gray-900/50 dark:hover:border-brand-500">
          <svg className="mb-2 h-7 w-7 text-brand-500" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16l4-4a2 2 0 012.8 0l1.2 1.2 2.2-2.2a2 2 0 012.8 0l2 2M8 8h.01M5 20h14a2 2 0 002-2V6a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" /></svg>
          <span className="text-sm font-semibold text-brand-500">Chọn ảnh từ thiết bị</span>
          <span className="mt-1 text-xs text-gray-500">Có thể chọn nhiều ảnh cùng lúc</span>
          <input type="file" accept="image/jpeg,image/png,image/webp" multiple disabled={c.busy || imageCount >= 10} className="sr-only" onChange={e => { c.selectImages(e.target.files); e.target.value = '' }} />
        </label>

        {imageCount > 0 && <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-5">
          {c.form.retainedImages.map((url, index) => <div key={url} className="relative overflow-hidden rounded-lg border border-gray-200 bg-gray-100 dark:border-gray-700"><img src={url} alt={`Ảnh hiện tại ${index + 1}`} className="aspect-square w-full object-cover" /><RemoveButton onClick={() => c.removeRetainedImage(url)} /><span className="absolute bottom-1 left-1 rounded bg-black/60 px-1.5 py-0.5 text-[10px] text-white">Đã lưu</span></div>)}
          {c.imagePreviews.map((preview, index) => <div key={`${preview.file.name}-${preview.file.lastModified}`} className="relative overflow-hidden rounded-lg border border-brand-200 bg-gray-100 dark:border-brand-500/30"><img src={preview.url} alt={preview.file.name} className="aspect-square w-full object-cover" /><RemoveButton onClick={() => c.removeNewImage(index)} /><span className="absolute bottom-1 left-1 max-w-[90%] truncate rounded bg-brand-500/85 px-1.5 py-0.5 text-[10px] text-white">Ảnh mới</span></div>)}
        </div>}
      </div>

      <div className="flex flex-wrap items-end justify-between gap-3 border-t border-gray-100 pt-4 dark:border-gray-800">
        <label className="text-sm font-medium text-gray-700 dark:text-gray-300">Trạng thái
          <select value={c.form.status} onChange={e => update('status', e.target.value)} className="ml-2 rounded-lg border border-gray-300 bg-white px-3 py-2 dark:border-gray-700 dark:bg-gray-900"><option value="DRAFT">Lưu nháp</option><option value="PUBLISHED">Đăng công khai</option></select>
        </label>
        <button disabled={c.busy} className="rounded-lg bg-brand-500 px-5 py-2.5 text-sm font-medium text-white disabled:opacity-50">{c.busy ? 'Đang tải ảnh và lưu…' : 'Lưu bài viết'}</button>
      </div>
    </form>}

    <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
      {c.data?.content.map(post => <article key={post.id} className="overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm dark:border-gray-800 dark:bg-white/3">
        {post.images[0] && <img src={post.images[0]} alt="" className="h-40 w-full object-cover" />}
        <div className="space-y-3 p-5">
          <div className="flex items-start justify-between gap-2"><h2 className="font-semibold text-gray-900 dark:text-white">{post.title}</h2><span className="shrink-0 rounded-full bg-gray-100 px-2 py-1 text-xs dark:bg-gray-800">{statusLabel[post.status]}</span></div>
          <p className="line-clamp-3 whitespace-pre-line text-sm text-gray-500 dark:text-gray-400">{post.content}</p>
          <div className="flex flex-wrap gap-3 text-sm"><button onClick={() => c.openEdit(post)} className="font-medium text-brand-500">Chỉnh sửa</button>{post.status !== 'HIDDEN' && <button onClick={() => c.hide(post)} className="font-medium text-error-500">Ẩn bài</button>}{post.status === 'PUBLISHED' && <Link to={`/floral-care/${post.id}`} className="font-medium text-gray-600 dark:text-gray-300">Xem công khai</Link>}</div>
        </div>
      </article>)}
    </div>
    {!c.busy && !c.data?.content?.length && <p className="rounded-xl border border-dashed border-gray-300 p-10 text-center text-gray-500">Chưa có bài viết nào.</p>}
    {c.busy && !c.data && <p role="status" className="text-gray-500">Đang tải bài viết…</p>}
    {(c.data?.totalPages || 0) > 1 && <div className="flex justify-center gap-3"><button disabled={c.page === 0} onClick={() => c.setPage(p => p - 1)} className="rounded border px-3 py-2 disabled:opacity-40">Trước</button><span className="py-2 text-sm">Trang {c.page + 1}/{c.data.totalPages}</span><button disabled={c.page + 1 >= c.data.totalPages} onClick={() => c.setPage(p => p + 1)} className="rounded border px-3 py-2 disabled:opacity-40">Sau</button></div>}
  </div>
}
