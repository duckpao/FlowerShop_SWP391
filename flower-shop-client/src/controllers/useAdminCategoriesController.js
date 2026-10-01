import { useEffect, useState } from 'react'
import { adminCategoryService as api } from '../services/adminCategoryService'

const emptyForm = () => ({ name: '', description: '', status: 'ACTIVE' })

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
    Promise.all([api.list(), api.products(params)])
      .then(([list, page]) => { if (active) { setCategories(list); setProducts(page) } })
      .catch(e => { if (active) setError(e.message) })
      .finally(() => { if (active) setBusy(false) })
    return () => { active = false }
  }, [params, revision])

  async function run(action, message) {
    setBusy(true); setError(''); setNotice('')
    try { await action(); setNotice(message); setRevision(x => x + 1) }
    catch (e) { setError(e.message) } finally { setBusy(false) }
  }

  const save = event => {
    event.preventDefault()
    const body = { name: form.name.trim(), description: form.description.trim(), status: form.status }
    return run(async () => {
      if (editing) await api.update(editing, body); else await api.create(body)
      setEditing(null); setForm(emptyForm())
    }, editing ? 'Đã cập nhật danh mục.' : 'Đã thêm danh mục mới.')
  }

  const edit = category => {
    setEditing(category.id)
    setForm({ name: category.name, description: category.description || '', status: category.status })
  }

  // Chỉ dựa vào adminHidden, không dùng available: available còn false khi shop tự ẩn hoặc shop bị khóa.
  const toggleHidden = product => {
    const question = product.adminHidden
      ? `Bỏ ẩn sản phẩm ${product.name}?`
      : `Ẩn sản phẩm ${product.name} khỏi trang công khai?`
    if (!window.confirm(question)) return
    return run(() => api.setHidden(product.id, !product.adminHidden),
      product.adminHidden ? 'Đã bỏ ẩn sản phẩm.' : 'Đã ẩn sản phẩm khỏi trang công khai.')
  }

  const search = event => { event.preventDefault(); setParams({ q: query.trim(), page: 0 }) }

  return {
    categories, products, form, setForm, editing, query, setQuery, busy, error, notice,
    save, edit, toggleHidden, search,
    cancel: () => { setEditing(null); setForm(emptyForm()) },
    changePage: delta => setParams(p => ({ ...p, page: p.page + delta })),
    reload: () => setRevision(x => x + 1),
  }
}
