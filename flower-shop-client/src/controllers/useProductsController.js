import { useEffect, useState } from 'react'
import { productService as api } from '../services/productService'
const empty = () => ({ name: '', description: '', categoryId: '', price: '', stock: 0, status: 'ACTIVE' })
export function useProductsController(shopId, manage) {
  const [data, setData] = useState(null)
  const [categories, setCategories] = useState([])
  const [form, setForm] = useState(empty)
  const [editing, setEditing] = useState(null)
  const [images, setImages] = useState([])
  const [videos, setVideos] = useState([])
  const [page, setPage] = useState(0)
  const [revision, setRevision] = useState(0)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  useEffect(() => {
    let active = true
    setBusy(true)
    Promise.all([manage ? api.list(shopId, page) : api.published(shopId, page), manage ? api.categories(shopId) : Promise.resolve([])])
      .then(([data, categories]) => { if (active) { setData(data); setCategories(categories); setError('') } })
      .catch(e => { if (active) setError(e.message) }).finally(() => { if (active) setBusy(false) })
    return () => { active = false }
  }, [shopId, manage, page, revision])
  useEffect(() => {
    if (!manage || !editing) { setImages([]); setVideos([]); return }
    let active = true
    Promise.all([api.images(shopId, editing), api.videos(shopId, editing)])
      .then(([imgs, vids]) => { if (active) { setImages(imgs); setVideos(vids) } })
      .catch(e => { if (active) setError(e.message) })
    return () => { active = false }
  }, [shopId, manage, editing])
  async function run(action) {
    setBusy(true); setError(''); setNotice('')
    try { await action(); setRevision(x => x + 1) } catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  const save = e => { e.preventDefault(); return run(async () => {
    const saved = await api.save(shopId, editing, { ...form, price: Number(form.price), stock: Number(form.stock) })
    setEditing(saved.id); setNotice('Đã lưu sản phẩm của shop. Bạn có thể tải ảnh lên ngay bên dưới.')
  }) }
  const hide = id => { if (window.confirm('Ẩn sản phẩm khỏi cửa hàng?')) return run(async () => { await api.hide(shopId, id); setNotice('Đã ẩn sản phẩm.'); }) }
  const edit = p => { setEditing(p.id); setForm({ name: p.name, description: p.description || '', categoryId: p.categoryId, price: p.price, stock: p.stock, status: p.status }) }
  const uploadImage = file => run(async () => { setImages(await api.uploadImage(shopId, editing, file)); setNotice('Đã tải ảnh lên.'); })
  const deleteImage = imageId => run(async () => { await api.deleteImage(shopId, editing, imageId); setImages(await api.images(shopId, editing)); setNotice('Đã xoá ảnh.'); })
  const uploadVideo = (file, title, description) => run(async () => { setVideos(await api.uploadVideo(shopId, editing, file, title, description)); setNotice('Đã tải video lên.'); })
  const deleteVideo = videoId => run(async () => { await api.deleteVideo(shopId, editing, videoId); setVideos(await api.videos(shopId, editing)); setNotice('Đã xoá video.'); })
  return { data, categories, form, setForm, editing, images, uploadImage, deleteImage, videos, uploadVideo, deleteVideo, page, setPage, busy, error, notice, save, hide, edit, cancel: () => { setEditing(null); setForm(empty()) }, reload: () => setRevision(x => x + 1) }
}
