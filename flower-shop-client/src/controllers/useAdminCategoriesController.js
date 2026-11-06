import { useEffect, useMemo, useState } from 'react'
import { adminCategoryService as api } from '../services/adminCategoryService'
import { useConfirm } from '../components/ui/confirm/ConfirmProvider'

const emptyForm = () => ({ name: '', description: '', status: 'ACTIVE', groupName: '' })

export function useAdminCategoriesController() {
  const { confirm } = useConfirm()
  const [categories, setCategories] = useState([])
  const [products, setProducts] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [editing, setEditing] = useState(null)
  const [groupEdit, setGroupEdit] = useState(null) // { from, to } khi đang đổi tên một nhóm
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
    try { await action(); setNotice(message); setRevision(x => x + 1); return true }
    catch (e) { setError(e.message); return false } finally { setBusy(false) }
  }

  // Các nhóm đang có, kèm danh mục của từng nhóm; danh mục chưa phân nhóm vào mục riêng (key rỗng)
  const groups = useMemo(() => {
    const map = new Map()
    for (const c of categories) {
      const key = c.groupName || ''
      if (!map.has(key)) map.set(key, [])
      map.get(key).push(c)
    }
    return [...map.entries()]
      .sort(([a], [b]) => (a === '' ? 1 : b === '' ? -1 : a.localeCompare(b, 'vi')))
      .map(([name, items]) => ({ name, items }))
  }, [categories])

  const save = event => {
    event.preventDefault()
    const body = { name: form.name.trim(), description: form.description.trim(), status: form.status, groupName: form.groupName.trim() }
    const wasEditing = editing
    return run(async () => {
      if (wasEditing) await api.update(wasEditing, body); else await api.create(body)
      setEditing(null); setForm(emptyForm())
    }, wasEditing ? 'Đã cập nhật danh mục.' : 'Đã thêm danh mục mới.')
  }

  const edit = category => {
    setEditing(category.id)
    setForm({ name: category.name, description: category.description || '', status: category.status, groupName: category.groupName || '' })
  }

  const cancel = () => { setEditing(null); setForm(emptyForm()) }

  const removeCategory = async category => {
    if (!await confirm({
      title: 'Xóa danh mục',
      message: `Xóa danh mục "${category.name}"? Chỉ xóa được khi chưa có sản phẩm nào thuộc danh mục này.`,
      confirmLabel: 'Xóa', tone: 'danger',
    })) return
    const ok = await run(() => api.remove(category.id), `Đã xóa danh mục ${category.name}.`)
    if (ok && editing === category.id) cancel()
  }

  const startGroupEdit = name => setGroupEdit({ from: name, to: name })

  const saveGroup = async event => {
    event.preventDefault()
    const to = groupEdit.to.trim()
    if (to === groupEdit.from) { setGroupEdit(null); return }
    if (!to && !await confirm({
      title: 'Bỏ phân nhóm',
      message: `Nhóm "${groupEdit.from}" sẽ bị xóa và các danh mục trong nhóm chuyển sang "Chưa phân nhóm". Tiếp tục?`,
      confirmLabel: 'Tiếp tục', tone: 'danger',
    })) return
    const ok = await run(() => api.renameGroup(groupEdit.from, to),
      to ? `Đã đổi tên nhóm thành "${to}".` : `Đã bỏ nhóm "${groupEdit.from}".`)
    if (ok) setGroupEdit(null)
  }

  // Chỉ dựa vào adminHidden, không dùng available: available còn false khi shop tự ẩn hoặc shop bị khóa.
  const toggleHidden = async product => {
    const question = product.adminHidden
      ? `Bỏ ẩn sản phẩm ${product.name}?`
      : `Ẩn sản phẩm ${product.name} khỏi trang công khai?`
    if (!await confirm({ title: product.adminHidden ? 'Bỏ ẩn sản phẩm' : 'Ẩn sản phẩm', message: question })) return
    return run(() => api.setHidden(product.id, !product.adminHidden),
      product.adminHidden ? 'Đã bỏ ẩn sản phẩm.' : 'Đã ẩn sản phẩm khỏi trang công khai.')
  }

  const search = event => { event.preventDefault(); setParams({ q: query.trim(), page: 0 }) }

  return {
    categories, groups, products, form, setForm, editing, groupEdit, setGroupEdit, query, setQuery, busy, error, notice,
    save, edit, cancel, removeCategory, startGroupEdit, saveGroup, toggleHidden, search,
    changePage: delta => setParams(p => ({ ...p, page: p.page + delta })),
    reload: () => setRevision(x => x + 1),
  }
}
