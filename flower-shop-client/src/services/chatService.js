import { authService } from './authService'

const request = (path, options) => authService.authenticatedRequest(path, options)

/**
 * API chat theo phía người dùng: scope là { kind: 'customer' } hoặc { kind: 'shop', shopId }.
 * Khách gọi /api/customer/chat, chủ shop gọi /api/shop/mine/{shopId}/chat; hai bên trả cùng dạng dữ liệu.
 */
export function chatApi(scope) {
    const base = scope.kind === 'shop'
        ? `/api/shop/mine/${encodeURIComponent(scope.shopId)}/chat`
        : '/api/customer/chat'
    const session = id => `${base}/sessions/${encodeURIComponent(id)}`
    return {
        sessions: () => request(`${base}/sessions`),
        // Khách mở theo shopId, chủ shop mở theo customOrderId (yêu cầu custom của khách)
        open: body => request(`${base}/sessions`, { method: 'POST', body }),
        unread: () => request(`${base}/unread`),
        messages: (id, before) => request(`${session(id)}/messages?${new URLSearchParams({ size: '30', ...(before ? { before } : {}) })}`),
        send: (id, body) => request(`${session(id)}/messages`, { method: 'POST', body }),
        read: id => request(`${session(id)}/read`, { method: 'POST' }),
        uploadImage: (id, file) => {
            const body = new FormData()
            body.append('file', file)
            return request(`${session(id)}/images`, { method: 'POST', body })
        },
    }
}
