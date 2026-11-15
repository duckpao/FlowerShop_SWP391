import { useEffect, useState } from 'react'
import { blogService } from '../services/blogService'
import { useConfirm } from '../components/ui/confirm/ConfirmProvider'

const emptyForm = () => ({ title: '', content: '', status: 'DRAFT', retainedImages: [], imageFiles: [] })

export function useBlogsController(shopId) {
  const { confirm } = useConfirm()
  const [data, setData] = useState(null)
  const [page, setPage] = useState(0)
  const [revision, setRevision] = useState(0)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [imagePreviews, setImagePreviews] = useState([])
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  useEffect(() => {
    const previews = form.imageFiles.map(file => ({ file, url: URL.createObjectURL(file) }))
    setImagePreviews(previews)
    return () => previews.forEach(preview => URL.revokeObjectURL(preview.url))
  }, [form.imageFiles])

  useEffect(() => {
    if (!shopId) return
    let active = true
    setBusy(true)
    blogService.managed(shopId, page)
      .then(value => { if (active) { setData(value); setError('') } })
      .catch(e => { if (active) setError(e.message) })
      .finally(() => { if (active) setBusy(false) })
    return () => { active = false }
  }, [shopId, page, revision])

  const openCreate = () => { setEditing(''); setForm(emptyForm()); setError(''); setNotice('') }
  const openEdit = post => {
    setEditing(post.id)
    setForm({
      title: post.title,
      content: post.content,
      status: post.status === 'HIDDEN' ? 'DRAFT' : post.status,
      retainedImages: post.images,
      imageFiles: [],
    })
    setError(''); setNotice('')
  }
  const close = () => { setEditing(null); setForm(emptyForm()) }

  const selectImages = files => {
    const selected = Array.from(files || [])
    const invalidType = selected.find(file => !['image/jpeg', 'image/png', 'image/webp'].includes(file.type))
    const tooLarge = selected.find(file => file.size > 5 * 1024 * 1024)
    if (invalidType) { setError('Chỉ nhận ảnh JPEG, PNG hoặc WebP.'); return }
    if (tooLarge) { setError(`Ảnh ${tooLarge.name} vượt quá 5MB.`); return }
    if (form.retainedImages.length + selected.length > 10) { setError('Mỗi bài viết chỉ được dùng tối đa 10 ảnh.'); return }
    setError('')
    setForm(current => ({ ...current, imageFiles: selected }))
  }
  const removeNewImage = index => setForm(current => ({
    ...current,
    imageFiles: current.imageFiles.filter((_, itemIndex) => itemIndex !== index),
  }))
  const removeRetainedImage = url => setForm(current => ({
    ...current,
    retainedImages: current.retainedImages.filter(image => image !== url),
  }))

  const save = async event => {
    event.preventDefault()
    setBusy(true); setError(''); setNotice('')
    try {
      if (form.retainedImages.length + form.imageFiles.length > 10) throw new Error('Mỗi bài viết chỉ được dùng tối đa 10 ảnh.')
      await blogService.save(shopId, editing || null, {
        title: form.title,
        content: form.content,
        status: form.status,
        retainedImages: form.retainedImages,
      }, form.imageFiles)
      setNotice(editing ? 'Đã cập nhật bài viết.' : 'Đã tạo bài viết.')
      close(); setRevision(value => value + 1)
    } catch (e) {
      setError(e.message)
    } finally { setBusy(false) }
  }

  const hide = async post => {
    if (!await confirm({ title: 'Ẩn bài viết', message: `Ẩn “${post.title}” khỏi trang công khai?`, tone: 'danger' })) return
    setBusy(true); setError(''); setNotice('')
    try {
      await blogService.hide(shopId, post.id)
      setNotice('Đã ẩn bài viết.'); setRevision(value => value + 1)
    } catch (e) { setError(e.message) } finally { setBusy(false) }
  }

  return {
    data, page, setPage, editing, form, setForm, imagePreviews, busy, error, notice,
    openCreate, openEdit, close, save, hide, selectImages, removeNewImage, removeRetainedImage,
  }
}
