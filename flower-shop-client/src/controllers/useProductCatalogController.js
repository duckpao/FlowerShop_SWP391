import { useEffect, useState } from 'react'
import { productService as api } from '../services/productService'
export function useProductCatalogController() {
  const [data, setData] = useState(null)
  const [categories, setCategories] = useState([])
  const [categoryId, setCategoryId] = useState('')
  const [q, setQ] = useState('')
  const [term, setTerm] = useState('')
  const [page, setPage] = useState(0)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  useEffect(() => { api.catalogCategories().then(setCategories).catch(() => {}) }, [])
  useEffect(() => {
    let active = true
    setBusy(true)
    api.catalog(page, categoryId, term).then(d => { if (active) { setData(d); setError('') } })
      .catch(e => { if (active) setError(e.message) })
      .finally(() => { if (active) setBusy(false) })
    return () => { active = false }
  }, [page, categoryId, term])
  return {
    data, categories, categoryId,
    setCategoryId: value => { setCategoryId(value); setPage(0) },
    q, setQ,
    search: e => { e.preventDefault(); setPage(0); setTerm(q) },
    page, setPage, busy, error,
  }
}
