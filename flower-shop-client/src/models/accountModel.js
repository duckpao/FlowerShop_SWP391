export const emptyAddress = () => ({ addressLine: '', city: '', district: '', ward: '', ghnWardCode: '', ghnDistrictId: null, isDefault: false })
export const profileInput = profile => ({ fullName: profile.fullName || '', phone: profile.phone || '' })
