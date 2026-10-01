import { authService } from './authService'

const base = shopId => `/api/shop/mine/${encodeURIComponent(shopId)}/orders`

export const managerOrderService = {
    list: (shopId, page = 0, status = null) => {
        const params = new URLSearchParams()
        if (page !== undefined && page !== null && page !== '') params.set('page', page)
        if (status) params.set('status', status)
        return authService.authenticatedRequest(`${base(shopId)}?${params.toString()}`)
    },
    getQueue: (shopId, page = 0, status = null) => {
        const params = new URLSearchParams()
        if (page !== undefined && page !== null && page !== '') params.set('page', page)
        if (status) params.set('status', status)
        return authService.authenticatedRequest(`${base(shopId)}/queue?${params.toString()}`)
    },
    getQueueCount: (shopId) => authService.authenticatedRequest(`${base(shopId)}/queue/count`),
    cancelOrder: (shopId, orderId, reason) => authService.authenticatedRequest(`${base(shopId)}/${encodeURIComponent(orderId)}/cancel`, {
        method: 'POST',
        body: { reason }
    }),
    detail: (shopId, id) => authService.authenticatedRequest(`${base(shopId)}/${encodeURIComponent(id)}`),
    confirm: (shopId, id) => authService.authenticatedRequest(`${base(shopId)}/${encodeURIComponent(id)}/confirm`, { method: 'POST' }),
    ship: (shopId, id) => authService.authenticatedRequest(`${base(shopId)}/${encodeURIComponent(id)}/ship`, { method: 'POST' }),
    refreshStatus: (shopId, id) => authService.authenticatedRequest(`${base(shopId)}/${encodeURIComponent(id)}/refresh-status`, { method: 'POST' }),
    simulateDelivered: (shopId, id) => authService.authenticatedRequest(`${base(shopId)}/${encodeURIComponent(id)}/simulate-delivered`, { method: 'POST' }),
}