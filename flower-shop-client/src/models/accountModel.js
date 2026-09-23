export const emptyAddress = (city = '') => ({ addressLine: '', city, district: '', ward: '', isDefault: false })
export const profileInput = profile => ({ fullName: profile.fullName || '', phone: profile.phone || '' })
