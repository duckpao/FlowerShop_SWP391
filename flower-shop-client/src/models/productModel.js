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

export const emptyProductFilter = () => ({
  q: '', categoryId: '', shopId: '', type: '', minPrice: '', maxPrice: '', sort: 'newest', page: 0,
})

export const ratingLabel = card =>
  card.reviewCount ? `★ ${card.rating} (${card.reviewCount})` : 'Chưa có đánh giá'
