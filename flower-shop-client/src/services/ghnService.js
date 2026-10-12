import { API_BASE } from '../apiBase'

const get = async path => {
    const res = await fetch(`${API_BASE}${path}`)
    if (!res.ok) throw new Error(`Không tải được dữ liệu GHN ${path}.`)
    return res.json()
}

export const ghnService = {
    // Two-level model since 01/07/2025: province -> ward, no district.
    provinces: () => get('/api/public/ghn/provinces'),
    wards: id => get(`/api/public/ghn/provinces/${encodeURIComponent(id)}/wards`),
}