import { useEffect, useState } from 'react'
import { productService as api } from '../services/productService'
import { useConfirm } from '../components/ui/confirm/ConfirmProvider'

const empty = () => ({ name: '', description: '', categoryId: '', price: '', stock: 0, status: 'ACTIVE', type: 'READY_MADE', imageFiles: [], primaryImageIndex: 0, primaryImageId: null, videoFiles: [] })

export function useProductsController(shopId, manage) {
  const { confirm } = useConfirm()
  const [data, setData] = useState(null)
  const [categories, setCategories] = useState([])
  const [form, setForm] = useState(empty)
  const [imagePreviews, setImagePreviews] = useState([])
  const [videoPreviews, setVideoPreviews] = useState([])
  const [editing, setEditing] = useState(null)
  const [mediaImages, setMediaImages] = useState([])
  const [mediaVideos, setMediaVideos] = useState([])
  const [page, setPage] = useState(0)
  const [revision, setRevision] = useState(0)
  const [busy, setBusy] = useState(false)
  const [uploadingImages, setUploadingImages] = useState(false)
  const [uploadingImageName, setUploadingImageName] = useState('')
  const [uploadingVideos, setUploadingVideos] = useState(false)
  const [uploadingVideoName, setUploadingVideoName] = useState('')
  const [imageSelectionError, setImageSelectionError] = useState('')
  const [videoSelectionError, setVideoSelectionError] = useState('')
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
    const previews = form.videoFiles.map(file => ({
      file,
      url: URL.createObjectURL(file),
    }))
    setVideoPreviews(previews)
    return () => previews.forEach(preview => URL.revokeObjectURL(preview.url))
  }, [form.videoFiles])

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
    try {
      const result = await action()
      setRevision(x => x + 1)
      return result ?? true
    } catch (e) {
      setError(e.message)
      return false
    } finally { setBusy(false) }
  }

  const save = e => {
    e.preventDefault()
    return run(async () => {
      if (form.imageFiles.length > 10) {
        throw new Error('Không được chọn quá 10 ảnh. Vui lòng bỏ bớt ảnh trước khi lưu.')
      }
      const oversizedImage = form.imageFiles.find(file => file.size > 5 * 1024 * 1024)
      if (oversizedImage) {
        throw new Error(`Ảnh ${oversizedImage.name} không được vượt quá 5MB.`)
      }

      if (form.videoFiles.length > 5) {
        throw new Error('Không được chọn quá 5 video. Vui lòng bỏ bớt video trước khi lưu.')
      }
      const oversizedVideo = form.videoFiles.find(file => file.size > 50 * 1024 * 1024)
      if (oversizedVideo) {
        throw new Error(`Video ${oversizedVideo.name} không được vượt quá 50MB.`)
      }

      setUploadingImages(true)
      const formData = new FormData()
      formData.append('product', new Blob([JSON.stringify({
        name: form.name,
        description: form.description,
        categoryId: form.categoryId,
        price: Number(form.price),
        stock: form.type === 'CUSTOM' ? (Number(form.stock) || 0) : Number(form.stock),
        status: form.status,
        type: form.type,
        primaryImageId: form.primaryImageId,
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

        for (const file of form.videoFiles) {
          setUploadingVideoName(file.name)
          formData.append('videos', file)
        }

        const saved = await api.save(shopId, editing, formData)
        setEditing(null); setForm(empty()); setMediaImages([]); setMediaVideos([])
        setImageSelectionError(''); setVideoSelectionError(''); setNotice('Đã lưu sản phẩm của shop.')
        return saved
      } catch (uploadError) {
        throw new Error(uploadError.message || 'Không thể lưu sản phẩm. Vui lòng thử lại.')
      } finally {
        setUploadingImages(false)
        setUploadingImageName('')
        setUploadingVideos(false)
        setUploadingVideoName('')
      }
    })
  }

  const hide = async id => {
    if (await confirm({ title: 'Ẩn sản phẩm', message: 'Ẩn sản phẩm khỏi cửa hàng?', confirmLabel: 'Ẩn' })) {
      return run(async () => { await api.hide(shopId, id); setNotice('Đã ẩn sản phẩm.') })
    }
  }

  const unhide = id => {
    if (window.confirm('Hiện lại sản phẩm này trên cửa hàng?')) {
      return run(async () => { await api.unhide(shopId, id); setNotice('Đã bỏ ẩn sản phẩm.') })
    }
  }

  const edit = async p => {
    setEditing(p.id)
    setForm({
      name: p.name, description: p.description || '', categoryId: p.categoryId,
      price: p.price, stock: p.stock, status: p.status, type: p.type || 'READY_MADE',
      imageFiles: [], primaryImageIndex: 0, primaryImageId: null,
      videoFiles: [],
    })
    setMediaImages([])
    setMediaVideos([])
    try {
      const [images, videos] = await Promise.all([
        api.images(shopId, p.id).catch(() => []),
        api.videos(shopId, p.id).catch(() => []),
      ])
      const loadedImages = Array.isArray(images) ? images : []
      const loadedVideos = Array.isArray(videos) ? videos : []
      setMediaImages(loadedImages)
      setMediaVideos(loadedVideos)
      setForm(current => ({
        ...current,
        primaryImageId: loadedImages.find(image => image.isPrimary)?.id || null,
      }))
    } catch (e) {
      setError(e.message || 'Không tải được dữ liệu ảnh/video của sản phẩm.')
    }
  }

  const setImageFiles = files => {
    if (files.length > 10) {
      setImageSelectionError('Không được chọn quá 10 ảnh. Đã giữ lại 10 ảnh đầu tiên.')
      files = files.slice(0, 10)
    } else {
      setImageSelectionError('')
    }
    setForm(f => ({ ...f, imageFiles: files, primaryImageIndex: 0, primaryImageId: null }))
  }
  const setPrimaryImage = index => setForm(f => ({ ...f, primaryImageIndex: index, primaryImageId: null }))
  const setPrimaryExistingImage = imageId => setForm(f => ({ ...f, primaryImageId: imageId }))

  const deleteImage = async (productId, imageId) => {
    await api.deleteImage(shopId, productId, imageId)
    setMediaImages(current => current.filter(image => image.id !== imageId))
    return reload()
  }

  const setVideoFiles = files => {
    if (files.length > 5) {
      setVideoSelectionError('Không được chọn quá 5 video. Đã giữ lại 5 video đầu tiên.')
      files = files.slice(0, 5)
    } else {
      setVideoSelectionError('')
    }
    setForm(f => ({ ...f, videoFiles: files }))
  }

  const deleteVideo = async (productId, videoId) => {
    await api.deleteVideo(shopId, productId, videoId)
    setMediaVideos(current => current.filter(video => video.id !== videoId))
    return reload()
  }

  return {
    data, categories, form, setForm, editing, page, setPage, busy, error, notice, save, hide, unhide, edit,
    mediaImages, imagePreviews, uploadingImages, uploadingImageName, imageSelectionError, setImageFiles, setPrimaryImage, setPrimaryExistingImage,
    deleteImage,
    mediaVideos, videoPreviews, uploadingVideos, uploadingVideoName, videoSelectionError, setVideoFiles, deleteVideo,
    cancel: () => { setEditing(null); setForm(empty()); setMediaImages([]); setMediaVideos([]); setImageSelectionError(''); setVideoSelectionError(''); setError(''); setNotice(''); setUploadingImageName(''); setUploadingVideoName('') },
    reload: () => setRevision(x => x + 1),
  }
}
