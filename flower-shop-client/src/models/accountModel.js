export const emptyAddress = (city = '') => ({ addressLine: '', city, ward: '', ghnProvinceId: null, ghnWardId: null, isDefault: false, recipientName: '', recipientPhone: '', isPickup: false, isReturn: false, addressType: 'HOME' })
export const profileInput = profile => ({ fullName: profile.fullName || '', phone: profile.phone || '' })
