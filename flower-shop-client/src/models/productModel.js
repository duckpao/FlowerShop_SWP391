// Quy ước hiển thị dùng chung cho 6 chức năng: enum từ BE -> nhãn tiếng Việt; không gọi API/database.
export const productStatusLabels = { ACTIVE: 'Đang bán', INACTIVE: 'Đã ẩn', OUT_OF_STOCK: 'Hết hàng' }
export const productTypeLabels = { READY_MADE: 'Bó hoa có sẵn', CUSTOM: 'Bó hoa custom' }

export const sortOptions = [
  ['newest', 'Mới nhất'],
  ['price_asc', 'Giá tăng dần'],
  ['price_desc', 'Giá giảm dần'],
]

export const formatPrice = value => `${Number(value).toLocaleString('vi-VN')} đ`

// Tên các trường khớp @RequestParam của PublicCatalogController.browse; rỗng nghĩa là không lọc trường đó.
export const emptyProductFilter = () => ({
  q: '', categoryId: '', shopId: '', type: '', minPrice: '', maxPrice: '', sort: 'newest', page: 0,
})

export const ratingLabel = card =>
  card.reviewCount ? `★ ${card.rating} (${card.reviewCount})` : 'Chưa có đánh giá'
