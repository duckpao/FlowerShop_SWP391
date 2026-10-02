import { useEffect, useState } from 'react'
import { catalogService } from '../services/catalogService'
import { emptyProductFilter } from '../models/productModel'

// Controller FE đang được ProductCatalogView dùng (không phải useProductCatalogController).
// filter là bộ lọc đã áp dụng; draft/priceDraft chỉ là dữ liệu đang gõ, chờ nhấn Tìm sản phẩm.
export function useCatalogController(initialShops) {
  const [filter, setFilter] = useState(emptyProductFilter)
  const [draft, setDraft] = useState('')
  const [priceDraft, setPriceDraft] = useState({ minPrice: '', maxPrice: '' })
  const [data, setData] = useState(null)
  const [categories, setCategories] = useState([])
  const [shops, setShops] = useState(Array.isArray(initialShops) ? initialShops : [])
  const [busy, setBusy] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    // GET /api/public/categories -> PublicCatalogController.categories -> CatalogService.categories.
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
    // GET /api/public/shops -> PublicShopController.search: dữ liệu cho ô lọc Cửa hàng.
    catalogService.shops()
      .then(list => { if (active) setShops(Array.isArray(list) ? list : []) })
      .catch(() => { if (active) setShops([]) })
    return () => { active = false }
  }, [initialShops])

  useEffect(() => {
    let active = true
    setBusy(true); setError('')
    // Mỗi lần filter đổi, gửi GET /api/public/products?...; BE trả content + thông tin phân trang.
    // active chỉ ngăn response cũ ghi đè state sau cleanup; không hủy request HTTP đã gửi.
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

  // Chặn submit tải lại trang, kiểm tra khoảng giá rồi áp dụng từ khóa/giá; quay về page=0.
  const search = event => {
    event.preventDefault()
    const minPrice = priceDraft.minPrice.trim()
    const maxPrice = priceDraft.maxPrice.trim()
    if (minPrice && maxPrice && Number(minPrice) > Number(maxPrice)) {
      setError('Giá tối thiểu không được lớn hơn giá tối đa.')
      return
    }
    setFilter(current => ({ ...current, q: draft.trim(), minPrice, maxPrice, page: 0 }))
  }

  return {
    filter, draft, setDraft, priceDraft, setPriceDraft, data, categories, shops, busy, error, search,
    // Các setter đổi filter -> effect browse chạy lại; trang đánh số từ 0, UI hiển thị cộng 1.
    setCategory: categoryId => setFilter(current => ({ ...current, categoryId, page: 0 })),
    setShop: shopId => setFilter(current => ({ ...current, shopId, page: 0 })),
    setType: type => setFilter(current => ({ ...current, type, page: 0 })),
    setSort: sort => setFilter(current => ({ ...current, sort, page: 0 })),
    changePage: delta => setFilter(current => ({ ...current, page: current.page + delta })),
    retry: () => setFilter(current => ({ ...current })),
  }
}
