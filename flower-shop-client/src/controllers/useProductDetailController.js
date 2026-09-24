import { useEffect, useState } from 'react'
import { productService as api } from '../services/productService'
export function useProductDetailController(id) {
  const [product, setProduct] = useState(null)
  const [busy, setBusy] = useState(true)
  const [error, setError] = useState('')
  useEffect(() => {
    let active = true
    setBusy(true)
    api.detail(id).then(p => { if (active) { setProduct(p); setError('') } })
      .catch(e => { if (active) setError(e.message) })
      .finally(() => { if (active) setBusy(false) })
    return () => { active = false }
  }, [id])
  return { product, busy, error }
}
