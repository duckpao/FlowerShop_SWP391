export const productStatusLabels = { ACTIVE: 'Đang bán', INACTIVE: 'Đã ẩn', OUT_OF_STOCK: 'Hết hàng' }
export const productTypeLabels = { READY_MADE: 'Có sẵn', CUSTOM: 'Hàng đặt trước' }
export const productTypeOptions = [
  ['', 'Tất cả loại hoa'],
  ['READY_MADE', 'Có sẵn'],
  ['CUSTOM', 'Hàng đặt trước'],
]

export const sortOptions = [
  ['newest', 'Mới nhất'],
  ['price_asc', 'Giá tăng dần'],
  ['price_desc', 'Giá giảm dần'],
]

export const formatPrice = value => `${Number(value).toLocaleString('vi-VN')} đ`

// Product prices have 10 integer digits (DECIMAL(12, 2)); catalog budgets use whole dong.
export const CATALOG_PRICE_MAX = 9999999999

export const catalogPriceRanges = [
  { id: 'all', label: 'Tất cả mức giá', minPrice: '', maxPrice: '' },
  { id: 'under-150k', label: 'Giá dưới 150.000₫', minPrice: '', maxPrice: '149999.99' },
  { id: '150k-350k', label: '150.000₫ - 350.000₫', minPrice: '150000', maxPrice: '350000' },
  { id: '350k-500k', label: '350.000₫ - 500.000₫', minPrice: '350000', maxPrice: '500000' },
  { id: '500k-700k', label: '500.000₫ - 700.000₫', minPrice: '500000', maxPrice: '700000' },
  { id: '700k-1m', label: '700.000₫ - 1.000.000₫', minPrice: '700000', maxPrice: '1000000' },
  { id: '1m-2m', label: '1.000.000₫ - 2.000.000₫', minPrice: '1000000', maxPrice: '2000000' },
  { id: '2m-3m', label: '2.000.000₫ - 3.000.000₫', minPrice: '2000000', maxPrice: '3000000' },
  { id: '3m-5m', label: '3.000.000₫ - 5.000.000₫', minPrice: '3000000', maxPrice: '5000000' },
  { id: 'over-5m', label: 'Giá trên 5.000.000₫', minPrice: '5000000.01', maxPrice: '' },
]

export function normalizeCatalogPriceInput(value) {
  const text = String(value ?? '').trim()
  if (!/^\d+$/.test(text) && !/^\d{1,3}(?:\.\d{3})+$/.test(text)) return text
  return text.replaceAll('.', '').replace(/^0+(?=\d)/, '')
}

export function formatCatalogPriceInput(value) {
  const text = normalizeCatalogPriceInput(value)
  return /^\d+$/.test(text) ? text.replace(/\B(?=(\d{3})+(?!\d))/g, '.') : text
}

export function validateCatalogPrices(prices) {
  const errors = {}
  for (const field of ['minPrice', 'maxPrice']) {
    const value = String(prices[field] ?? '')
    if (!value) continue
    if (!/^\d+$/.test(value)) {
      errors[field] = 'Vui lòng nhập số tiền nguyên, không âm (ví dụ: 100000).'
    } else if (Number(value) > CATALOG_PRICE_MAX) {
      errors[field] = `Giá không được vượt quá ${formatCatalogPriceInput(CATALOG_PRICE_MAX)} ₫.`
    }
  }
  if (!Object.keys(errors).length && prices.minPrice !== '' && prices.maxPrice !== ''
      && Number(prices.minPrice) > Number(prices.maxPrice)) {
    errors.maxPrice = 'Đến giá phải lớn hơn hoặc bằng Giá từ.'
  }
  return errors
}

export const emptyProductFilter = () => ({
  q: '', categoryId: '', shopId: '', type: '', minPrice: '', maxPrice: '', sort: 'newest', page: 0,
})

export const ratingLabel = card =>
  card.reviewCount ? `★ ${card.rating} (${card.reviewCount})` : 'Chưa có đánh giá'
