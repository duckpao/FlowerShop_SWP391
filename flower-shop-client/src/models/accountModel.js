export const emptyAddress = (city = '') => ({ addressLine: '', city, district: '', ward: '', ghnWardCode: '', ghnDistrictId: null, isDefault: false, recipientName: '', recipientPhone: '', addressType: 'HOME' })
export const profileInput = profile => ({ fullName: profile.fullName || '', phone: profile.phone || '' })
