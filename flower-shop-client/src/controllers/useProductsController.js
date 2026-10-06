import { useEffect, useState } from 'react'
import { productService as api } from '../services/productService'

const empty = () => ({ name: '', description: '', categoryId: '', price: '', stock: 0, status: 'ACTIVE', type: 'READY_MADE', images: [], imageFiles: [], primaryImageIndex: 0 })

export function useProductsController(shopId, manage) {
  const [data, setData] = useState(null)
  const [categories, setCategories] = useState([])
  const [form, setForm] = useState(empty)
  const [imagePreviews, setImagePreviews] = useState([])
  const [editing, setEditing] = useState(null)
  const [mediaImages, setMediaImages] = useState([])
  const [page, setPage] = useState(0)
  const [revision, setRevision] = useState(0)
  const [busy, setBusy] = useState(false)
  const [uploadingImages, setUploadingImages] = useState(false)
  const [uploadingImageName, setUploadingImageName] = useState('')
  const [imageSelectionError, setImageSelectionError] = useState('')
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  useEffect(() => {
    const previews = form.imageFiles.map(file => ({
      file,
      url: URL.createObjectURL(file),
    }))
    setImagePreviews(previews)
    return () => previews.forEach(preview => URL.revokeObjectURL(preview.url))
  }, [form.imageFiles])

  useEffect(() => {
    let active = true
    setBusy(true)
    Promise.all([manage ? api.list(shopId, page) : api.published(shopId, page), manage ? api.categories(shopId) : Promise.resolve([])])
      .then(([data, categories]) => { if (active) { setData(data); setCategories(categories); setError('') } })
      .catch(e => { if (active) setError(e.message) }).finally(() => { if (active) setBusy(false) })
    return () => { active = false }
  }, [shopId, manage, page, revision])

  async function run(action) {
    setBusy(true); setError(''); setNotice('')
    try { await action(); setRevision(x => x + 1) } catch (e) { setError(e.message) } finally { setBusy(false) }
  }

  const save = e => {
    e.preventDefault()
    return run(async () => {
      if (form.imageFiles.length > 10) {
        throw new Error('Không được chọn quá 10 ảnh. Vui lòng bỏ một ảnh trước khi lưu.')
      }

      setUploadingImages(true)
      const formData = new FormData()
      formData.append('product', new Blob([JSON.stringify({
        name: form.name,
        description: form.description,
        categoryId: form.categoryId,
        price: Number(form.price),
        stock: Number(form.stock),
        status: form.status,
        type: form.type,
      })], { type: 'application/json' }))

      try {
        const orderedFiles = [...form.imageFiles]
        if (orderedFiles.length > 1) {
          orderedFiles.unshift(orderedFiles.splice(form.primaryImageIndex, 1)[0])
        }
        for (const file of orderedFiles) {
          setUploadingImageName(file.name)
          formData.append('images', file)
        }

        const saved = await api.save(shopId, editing, formData)
        setEditing(null); setForm(empty()); setMediaImages([]); setImageSelectionError(''); setNotice('Đã lưu sản phẩm của shop.')
        return saved
      } catch (uploadError) {
        throw new Error(uploadError.message || 'Không thể lưu sản phẩm. Vui lòng thử lại.')
      } finally {
        setUploadingImages(false)
        setUploadingImageName('')
      }
    })
  }

  const setPrimary = async (productId, imageId) => {
    await api.setPrimaryImage(shopId, productId, imageId)
    return reload()
  }

  const hide = id => {
    if (window.confirm('Ẩn sản phẩm khỏi cửa hàng?')) {
      return run(async () => { await api.hide(shopId, id); setNotice('Đã ẩn sản phẩm.') })
    }
  }

  const edit = async p => {
    setEditing(p.id)
    setForm({
      name: p.name, description: p.description || '', categoryId: p.categoryId,
      price: p.price, stock: p.stock, status: p.status, type: p.type || 'READY_MADE',
      images: [], imageFiles: [], primaryImageIndex: 0,
    })
    setMediaImages([])
    try {
      const images = await api.images(shopId, p.id)
      setMediaImages(Array.isArray(images) ? images : [])
    } catch (e) {
      setError(e.message || 'Không tải được dữ liệu ảnh của sản phẩm.')
    }
  }

  const setImageFiles = files => {
    if (files.length > 10) {
      setImageSelectionError('Không được chọn quá 10 ảnh. Đã giữ lại 10 ảnh đầu tiên.')
      files = files.slice(0, 10)
    } else {
      setImageSelectionError('')
    }
    setForm(f => ({ ...f, imageFiles: files, primaryImageIndex: 0 }))
  }
  const setPrimaryImage = index => setForm(f => ({ ...f, primaryImageIndex: index }))

  const deleteImage = async (productId, imageId) => {
    await api.deleteImage(shopId, productId, imageId)
    setMediaImages(current => current.filter(image => image.id !== imageId))
    return reload()
  }

  return {
    data, categories, form, setForm, editing, page, setPage, busy, error, notice, save, hide, edit,
    mediaImages, imagePreviews, uploadingImages, uploadingImageName, imageSelectionError, setImageFiles, setPrimaryImage, setPrimary,
    deleteImage,
    cancel: () => { setEditing(null); setForm(empty()); setMediaImages([]); setImageSelectionError(''); setError(''); setNotice(''); setUploadingImageName('') },
    reload: () => setRevision(x => x + 1),
  }
}
