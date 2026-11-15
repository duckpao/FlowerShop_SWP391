import { API_BASE } from '../apiBase'

// GET không cần CSRF/JWT; lỗi HTTP được đổi thành Error có thông báo tiếng Việt
const get = async path => {
    const res = await fetch(`${API_BASE}${path}`)
    if (!res.ok) throw new Error(`Không tải được dữ liệu GHN ${path}.`)
    return res.json()
}

/**
 * Lấy dữ liệu địa danh GHN (công khai, không cần đăng nhập) qua backend /api/public/ghn.
 * Luồng: GhnAddressPicker -> ghnService -> PublicGhnController -> GhnService -> API GHN master-data.
 * Dùng cho GhnAddressPicker (form địa chỉ).
 */
export const ghnService = {
    // Province -> district -> ward; the ward id is a ward code.
    // GET /api/public/ghn/provinces -> PublicGhnController.provinces -> GHN /master-data/province
    provinces: () => get('/api/public/ghn/provinces'),
    // GET /api/public/ghn/provinces/{id}/districts -> PublicGhnController.districts -> GHN /master-data/district
    districts: id => get(`/api/public/ghn/provinces/${encodeURIComponent(id)}/districts`),
    // GET /api/public/ghn/districts/{id}/wards -> PublicGhnController.wards -> GHN /master-data/ward
    wards: id => get(`/api/public/ghn/districts/${encodeURIComponent(id)}/wards`),
}