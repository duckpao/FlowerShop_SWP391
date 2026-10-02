import { useEffect, useState } from 'react'
import { adminCategoryService as api } from '../services/adminCategoryService'

const emptyForm = () => ({ name: '', description: '', status: 'ACTIVE' })

// Hook dùng chung cho hai phần trong AdminCategoriesView: danh mục và kiểm duyệt sản phẩm.
export function useAdminCategoriesController() {
  const [categories, setCategories] = useState([])
  const [products, setProducts] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [editing, setEditing] = useState(null)
  const [query, setQuery] = useState('')
  const [params, setParams] = useState({ q: '', page: 0 })
  const [revision, setRevision] = useState(0)
  const [busy, setBusy] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  useEffect(() => {
    let active = true
    setBusy(true); setError('')
    // GET danh mục + GET sản phẩm quản trị song song; params/revision đổi sẽ tải lại cả hai.
    Promise.all([api.list(), api.products(params)])
      .then(([list, page]) => { if (active) { setCategories(list); setProducts(page) } })
      .catch(e => { if (active) setError(e.message) })
      .finally(() => { if (active) setBusy(false) })
    return () => { active = false }
  }, [params, revision])

  // Chuẩn hóa busy/error/notice; ghi thành công thì tăng revision để lấy dữ liệu mới từ BE.
  async function run(action, message) {
    setBusy(true); setError(''); setNotice('')
    try { await action(); setNotice(message); setRevision(x => x + 1) }
    catch (e) { setError(e.message) } finally { setBusy(false) }
  }

  // Chỉ lưu DANH MỤC: editing quyết định POST tạo hay PUT sửa; status=INACTIVE thay cho xóa.
  const save = event => {
    event.preventDefault()
    const body = { name: form.name.trim(), description: form.description.trim(), status: form.status }
    return run(async () => {
      if (editing) await api.update(editing, body); else await api.create(body)
      setEditing(null); setForm(emptyForm())
    }, editing ? 'Đã cập nhật danh mục.' : 'Đã thêm danh mục mới.')
  }

  // Đổ danh mục đã chọn vào form, chưa gửi request cho tới khi nhấn Lưu.
  const edit = category => {
    setEditing(category.id)
    setForm({ name: category.name, description: category.description || '', status: category.status })
  }

  // Chỉ dựa vào adminHidden, không dùng available: available còn false khi shop tự ẩn hoặc shop bị khóa.
  // Nút kiểm duyệt -> PUT /api/admin/products/{id}/hidden với JSON {hidden: true/false}.
  const toggleHidden = product => {
    const question = product.adminHidden
      ? `Bỏ ẩn sản phẩm ${product.name}?`
      : `Ẩn sản phẩm ${product.name} khỏi trang công khai?`
    if (!window.confirm(question)) return
    return run(() => api.setHidden(product.id, !product.adminHidden),
      product.adminHidden ? 'Đã bỏ ẩn sản phẩm.' : 'Đã ẩn sản phẩm khỏi trang công khai.')
  }

  // Tìm sản phẩm kiểm duyệt theo tên và quay lại trang đầu; effect phụ thuộc params gửi GET mới.
  const search = event => { event.preventDefault(); setParams({ q: query.trim(), page: 0 }) }

  return {
    categories, products, form, setForm, editing, query, setQuery, busy, error, notice,
    save, edit, toggleHidden, search,
    cancel: () => { setEditing(null); setForm(emptyForm()) },
    changePage: delta => setParams(p => ({ ...p, page: p.page + delta })),
    reload: () => setRevision(x => x + 1),
  }
}
