import { useEffect, useState } from 'react'
import { catalogService } from '../services/catalogService'
import { emptyProductFilter } from '../models/productModel'

export function useCatalogController() {
  const [filter, setFilter] = useState(emptyProductFilter)
  const [draft, setDraft] = useState('')
  const [data, setData] = useState(null)
  const [categories, setCategories] = useState([])
  const [busy, setBusy] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    catalogService.categories()
      .then(list => { if (active) setCategories(list) })
      .catch(() => { if (active) setCategories([]) })
    return () => { active = false }
  }, [])

  useEffect(() => {
    let active = true
    setBusy(true); setError('')
    catalogService.browse(filter)
      .then(value => { if (active) setData(value) })
      .catch(e => { if (active) setError(e.message) })
      .finally(() => { if (active) setBusy(false) })
    return () => { active = false }
  }, [filter])

  const search = event => {
    event.preventDefault()
    setFilter(current => ({ ...current, q: draft.trim(), page: 0 }))
  }

  return {
    filter, draft, setDraft, data, categories, busy, error, search,
    setCategory: categoryId => setFilter(current => ({ ...current, categoryId, page: 0 })),
    setSort: sort => setFilter(current => ({ ...current, sort, page: 0 })),
    changePage: delta => setFilter(current => ({ ...current, page: current.page + delta })),
    retry: () => setFilter(current => ({ ...current })),
  }
}
