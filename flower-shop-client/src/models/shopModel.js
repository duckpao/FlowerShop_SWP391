export const shopStatusLabels = { PENDING: 'Chờ duyệt', ACTIVE: 'Hoạt động', INACTIVE: 'Ngừng hoạt động', BANNED: 'Bị khóa' }
export const shopActions = { PENDING: ['approve', 'Duyệt shop'], ACTIVE: ['block', 'Khóa shop'], BANNED: ['unblock', 'Mở khóa shop'] }

// Form state for the shop pickup address (province, district, ward and the GHN codes they were picked from).
export const shopAddressForm = a => ({
  addressLine: a?.addressLine || '',
  city: a?.city || '',
  district: a?.district || '',
  ward: a?.ward || '',
  ghnDistrictId: a?.ghnDistrictId || null,
  ghnWardCode: a?.ghnWardCode || '',
  isDefault: true,
})
