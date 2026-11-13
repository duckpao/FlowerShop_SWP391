import { useEffect, useState } from 'react'
import { catalogService } from '../services/catalogService'
import { catalogPriceRanges, emptyProductFilter, normalizeCatalogPriceInput, validateCatalogPrices } from '../models/productModel'

export function useCatalogController(initialShops) {
  const [filter, setFilter] = useState(emptyProductFilter)
  const [draft, setDraft] = useState('')
  const [priceDraft, setPriceDraft] = useState({ minPrice: '', maxPrice: '' })
  const [priceErrors, setPriceErrors] = useState({})
  const [priceRange, setPriceRange] = useState('all')
  const [data, setData] = useState(null)
  const [categories, setCategories] = useState([])
  const [shops, setShops] = useState(Array.isArray(initialShops) ? initialShops : [])
  const [busy, setBusy] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    catalogService.categories()
      .then(list => { if (active) setCategories(Array.isArray(list) ? list : []) })
      .catch(() => { if (active) setCategories([]) })
    return () => { active = false }
  }, [])

  useEffect(() => {
    let active = true
    if (initialShops !== undefined) {
      setShops(Array.isArray(initialShops) ? initialShops : [])
      return () => { active = false }
    }
    catalogService.shops()
      .then(list => { if (active) setShops(Array.isArray(list) ? list : []) })
      .catch(() => { if (active) setShops([]) })
    return () => { active = false }
  }, [initialShops])

  useEffect(() => {
    let active = true
    setBusy(true); setError('')
    catalogService.browse(filter)
      .then(value => {
        if (!active) return
        if (!value || !Array.isArray(value.content)) {
          setData(null)
          setError('Dữ liệu sản phẩm không hợp lệ hoặc dịch vụ đang tạm thời không khả dụng.')
          return
        }
        setData(value)
      })
      .catch(e => { if (active) setError(e.message) })
      .finally(() => { if (active) setBusy(false) })
    return () => { active = false }
  }, [filter])

  const selectPriceRange = id => {
    if (id === 'custom') {
      setPriceRange(id)
      setPriceDraft({ minPrice: '', maxPrice: '' })
      setPriceErrors({})
      return
    }
    const range = catalogPriceRanges.find(option => option.id === id)
    if (!range) return
    const { minPrice, maxPrice } = range
    setPriceRange(id)
    setPriceDraft({ minPrice, maxPrice })
    setPriceErrors({})
    setError('')
    setFilter(current => ({ ...current, minPrice, maxPrice, page: 0 }))
  }

  const updatePriceDraft = (field, value) => {
    const next = { ...priceDraft, [field]: normalizeCatalogPriceInput(value) }
    setPriceDraft(next)
    setPriceErrors(validateCatalogPrices(next))
    setPriceRange('custom')
  }

  const search = event => {
    if (event) event.preventDefault()
    const errors = priceRange === 'custom' ? validateCatalogPrices(priceDraft) : {}
    setPriceErrors(errors)
    if (Object.keys(errors).length) {
      event?.currentTarget?.elements.namedItem(Object.keys(errors)[0])?.focus()
      return
    }
    const { minPrice, maxPrice } = priceDraft
    setError('')
    setFilter(current => ({ ...current, q: draft.trim(), minPrice, maxPrice, page: 0 }))
  }

  const resetFilters = () => {
    setDraft('')
    setPriceDraft({ minPrice: '', maxPrice: '' })
    setPriceErrors({})
    setPriceRange('all')
    setError('')
    setFilter(emptyProductFilter())
  }

  return {
    filter, draft, setDraft, priceDraft, priceErrors, priceRange, selectPriceRange, updatePriceDraft, data, categories, shops, busy, error, search, resetFilters,
    setCategory: categoryId => setFilter(current => ({ ...current, categoryId, page: 0 })),
    setShop: shopId => setFilter(current => ({ ...current, shopId, page: 0 })),
    setType: type => setFilter(current => ({ ...current, type, page: 0 })),
    setSort: sort => setFilter(current => ({ ...current, sort, page: 0 })),
    // Applies category and sort from the URL; keeps the same filter object when nothing changed.
    applyUrl: (categoryId, sort) => setFilter(current =>
      current.categoryId === categoryId && current.sort === sort ? current : { ...current, categoryId, sort, page: 0 }),
    changePage: delta => setFilter(current => ({ ...current, page: current.page + delta })),
    retry: () => setFilter(current => ({ ...current })),
  }
}
