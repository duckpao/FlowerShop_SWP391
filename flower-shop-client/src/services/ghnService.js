import { API_BASE } from '../apiBase'

// GET không cần CSRF/JWT; lỗi HTTP được đổi thành Error có thông báo tiếng Việt.
// Backend trả 502 kèm lý do khi GHN lỗi: hiện nguyên lý do đó để người dùng biết lỗi nằm ở phía GHN.
const get = async path => {
    let res
    try {
        res = await fetch(`${API_BASE}${path}`)
    } catch {
        throw new Error('Mất kết nối tới máy chủ. Vui lòng kiểm tra đường truyền.')
    }
    if (!res.ok) {
        const data = await res.json().catch(() => ({}))
        throw new Error(data.message || (res.status === 502
            ? 'GHN đang gặp sự cố, chưa tải được danh mục địa chỉ. Vui lòng thử lại sau ít phút.'
            : 'Không tải được dữ liệu địa chỉ GHN.'))
    }
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