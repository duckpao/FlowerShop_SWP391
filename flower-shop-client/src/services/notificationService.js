import { authService } from './authService'

const base = '/api/account/notifications'

export const notificationService = {
    // Lấy danh sách thông báo của user hiện tại
    mine: () =>
        authService.authenticatedRequest(base),

    // Số thông báo chưa đọc
    unreadCount: () =>
        authService.authenticatedRequest(`${base}/unread-count`),

    // Đánh dấu tất cả đã đọc
    markAllAsRead: () =>
        authService.authenticatedRequest(`${base}/read-all`, {
            method: 'PUT',
        }),
}
