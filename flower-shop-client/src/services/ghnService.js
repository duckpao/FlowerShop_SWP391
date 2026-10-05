import { API_BASE } from '../apiBase'

const get = async path => {
    const res = await fetch(`${API_BASE}${path}`)
    if (!res.ok) throw new Error(`Không tải được dữ liệu GHN ${path}.`)
    return res.json()
}

export const ghnService = {
    provinces: () => get('/api/public/ghn/provinces'),
    districts: id => get(`/api/public/ghn/provinces/${encodeURIComponent(id)}/districts`),
    wards: id => get(`/api/public/ghn/districts/${encodeURIComponent(id)}/wards`),
}