import { useEffect, useState } from 'react'
import { productService as api } from '../services/productService'

// Giá trị khởi tạo/reset form; editing=null nghĩa là tạo mới, có id nghĩa là cập nhật sản phẩm đó.
const empty = () => ({ name: '', description: '', categoryId: '', price: '', stock: 0, status: 'ACTIVE', type: 'READY_MADE', images: [] })

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
    // Shop: list + categories chạy song song. Khách xem shop: chỉ published, không lấy danh mục quản lý.
    // save/hide tăng revision để effect tải lại danh sách từ BE sau khi ghi thành công.
    Promise.all([manage ? api.list(shopId, page) : api.published(shopId, page), manage ? api.categories(shopId) : Promise.resolve([])])
      .then(([data, categories]) => { if (active) { setData(data); setCategories(categories); setError('') } })
      .catch(e => { if (active) setError(e.message) }).finally(() => { if (active) setBusy(false) })
    return () => { active = false }
  }, [shopId, manage, page, revision])

  // Bọc tác vụ ghi: bật busy, bắt lỗi, tăng revision sau thành công và luôn tắt busy.
  async function run(action) {
    setBusy(true); setError(''); setNotice('')
    try { await action(); setRevision(x => x + 1) } catch (e) { setError(e.message) } finally { setBusy(false) }
  }

  // Nút Lưu -> chuẩn hóa giá/tồn kho thành số, bỏ URL rỗng -> productService.save.
  // editing có id: PUT; chưa có id: POST. images gửi lên sẽ thay toàn bộ danh sách ảnh ở BE.
  const save = e => {
    e.preventDefault()
    return run(async () => {
      await api.save(shopId, editing, {
        ...form,
        price: Number(form.price),
        stock: Number(form.stock),
        images: form.images.filter(i => i.url.trim()).map(i => ({ url: i.url.trim(), primary: i.primary })),
      })
      setEditing(null); setForm(empty()); setNotice('Đã lưu sản phẩm của shop.')
    })
  }

// Xác nhận ẩn -> DELETE API; BE chỉ đổi Products.status=INACTIVE, không xóa hàng trong database.
  const hide = id => {
    if (window.confirm('Ẩn sản phẩm khỏi cửa hàng?')) {
      return run(async () => { await api.hide(shopId, id); setNotice('Đã ẩn sản phẩm.') })
    }
  }

// Nút Sửa chỉ đưa dữ liệu vào form tại FE; tới khi bấm Lưu mới gọi BE.
  const edit = p => {
    setEditing(p.id)
    setForm({
      name: p.name, description: p.description || '', categoryId: p.categoryId,
      price: p.price, stock: p.stock, status: p.status, type: p.type || 'READY_MADE',
      images: (p.images || []).map((url, index) => ({ url, primary: index === 0 })),
    })
  }

  // Ảnh là danh sách URL; đúng một ảnh được đánh dấu chính.
// Các hàm ảnh bên dưới chỉ sửa form trong bộ nhớ; save mới gửi danh sách URL xuống BE.
  const addImage = () => setForm(f => ({ ...f, images: [...f.images, { url: '', primary: f.images.length === 0 }] }))
  const setImageUrl = (index, url) => setForm(f => ({ ...f, images: f.images.map((i, x) => x === index ? { ...i, url } : i) }))
  const setPrimaryImage = index => setForm(f => ({ ...f, images: f.images.map((i, x) => ({ ...i, primary: x === index })) }))
  const removeImage = index => setForm(f => {
    const images = f.images.filter((_, x) => x !== index)
    if (images.length && !images.some(i => i.primary)) images[0] = { ...images[0], primary: true }
    return { ...f, images }
  })

  return {
    data, categories, form, setForm, editing, page, setPage, busy, error, notice, save, hide, edit,
    addImage, setImageUrl, setPrimaryImage, removeImage,
    cancel: () => { setEditing(null); setForm(empty()) },
    reload: () => setRevision(x => x + 1),
  }
}
