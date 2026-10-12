export const shopStatusLabels = { PENDING: 'Chờ duyệt', ACTIVE: 'Hoạt động', INACTIVE: 'Ngừng hoạt động', BANNED: 'Bị khóa' }
export const shopActions = { PENDING: ['approve', 'Duyệt shop'], ACTIVE: ['block', 'Khóa shop'], BANNED: ['unblock', 'Mở khóa shop'] }

// Form state for the shop pickup address (province + ward, each with its GHN id).
export const shopAddressForm = a => ({
  addressLine: a?.addressLine || '',
  city: a?.city || '',
  ward: a?.ward || '',
  ghnProvinceId: a?.ghnProvinceId || null,
  ghnWardId: a?.ghnWardId || null,
  isDefault: true,
})
