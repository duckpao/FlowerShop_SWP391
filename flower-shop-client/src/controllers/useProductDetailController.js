import { useEffect, useState } from 'react'
import { catalogService } from '../services/catalogService'

// productId đổi hoặc reload tăng revision -> effect gọi lại catalogService.detail và cập nhật product.
export function useProductDetailController(productId) {
  const [product, setProduct] = useState(null)
  const [busy, setBusy] = useState(true)
  const [error, setError] = useState('')
  const [revision, setRevision] = useState(0)

  useEffect(() => {
    let active = true
    setBusy(true); setError('')
    // GET /api/public/products/{id} -> CatalogService.detail; 404 gồm cả không tồn tại và không được công khai.
    catalogService.detail(productId)
      .then(value => { if (active) setProduct(value) })
      .catch(e => {
        if (!active) return
        setProduct(null)
        setError(e.status === 404 ? 'Sản phẩm không còn được bán.' : e.message)
      })
      .finally(() => { if (active) setBusy(false) })
    return () => { active = false }
  }, [productId, revision])

  return { product, busy, error, reload: () => setRevision(x => x + 1) }
}
