import { authService } from './authService'

const base = shopId => `/api/shop/mine/${encodeURIComponent(shopId)}/orders`

export const managerOrderService = {
    list: (shopId, page, status) => {
        const params = new URLSearchParams({ page })
        if (status) params.set('status', status)
        return authService.authenticatedRequest(`${base(shopId)}?${params}`)
    },
    getQueue: (shopId, page = 0, status = null) => {
        const params = new URLSearchParams({ page })
        if (status) params.set('status', status)
        return authService.authenticatedRequest(`${base(shopId)}/queue?${params}`)
    },
    getQueueCount: (shopId) => authService.authenticatedRequest(`${base(shopId)}/queue/count`),
    cancelOrder: (shopId, orderId, reason) => authService.authenticatedRequest(`${base(shopId)}/${encodeURIComponent(orderId)}/cancel`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ reason })
    }),
    detail: (shopId, id) => authService.authenticatedRequest(`${base(shopId)}/${encodeURIComponent(id)}`),
    ship: (shopId, id) => authService.authenticatedRequest(`${base(shopId)}/${encodeURIComponent(id)}/ship`, { method: 'POST' }),
    refreshStatus: (shopId, id) => authService.authenticatedRequest(`${base(shopId)}/${encodeURIComponent(id)}/refresh-status`, { method: 'POST' }),
}