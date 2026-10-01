export const productStatusLabels = { ACTIVE: 'Đang bán', INACTIVE: 'Đã ẩn', OUT_OF_STOCK: 'Hết hàng' }

export const sortOptions = [
  ['newest', 'Mới nhất'],
  ['price_asc', 'Giá tăng dần'],
  ['price_desc', 'Giá giảm dần'],
]

export const formatPrice = value => `${Number(value).toLocaleString('vi-VN')} đ`

export const emptyProductFilter = () => ({ q: '', categoryId: '', sort: 'newest', page: 0 })

export const ratingLabel = card =>
  card.reviewCount ? `★ ${card.rating} (${card.reviewCount})` : 'Chưa có đánh giá'
