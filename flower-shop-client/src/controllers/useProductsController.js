import { useEffect, useState } from 'react'
import { productService as api } from '../services/productService'
const empty = () => ({ name: '', description: '', categoryId: '', price: '', stock: 0, status: 'ACTIVE' })
export function useProductsController(shopId, manage) {
  const [data, setData] = useState(null)
  const [categories, setCategories] = useState([])
  const [form, setForm] = useState(empty)
  const [editing, setEditing] = useState(null)
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
  async function run(action) {
    setBusy(true); setError(''); setNotice('')
    try { await action(); setRevision(x => x + 1) } catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  const save = e => { e.preventDefault(); return run(async () => { await api.save(shopId, editing, { ...form, price: Number(form.price), stock: Number(form.stock) }); setEditing(null); setForm(empty()); setNotice('Đã lưu sản phẩm của shop.'); }) }
  const hide = id => { if (window.confirm('Ẩn sản phẩm khỏi cửa hàng?')) return run(async () => { await api.hide(shopId, id); setNotice('Đã ẩn sản phẩm.'); }) }
  const edit = p => { setEditing(p.id); setForm({ name: p.name, description: p.description || '', categoryId: p.categoryId, price: p.price, stock: p.stock, status: p.status }) }
  return { data, categories, form, setForm, editing, page, setPage, busy, error, notice, save, hide, edit, cancel: () => { setEditing(null); setForm(empty()) }, reload: () => setRevision(x => x + 1) }
}
