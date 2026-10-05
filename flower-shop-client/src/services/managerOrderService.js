import { authService } from './authService'

/**
 * Đường dẫn gốc API quản lý đơn hàng của shop
 * @param {string} shopId - ID cửa hàng
 */
const base = shopId => `/api/shop/mine/${encodeURIComponent(shopId)}/orders`

/**
 * Service giao tiếp với Backend phục vụ chức năng Quản lý đơn hàng (Shop Manager).
 * Đính kèm JWT token và tự động xử lý CSRF token khi gửi request.
 */
export const managerOrderService = {
    /**
     * Lấy danh sách tất cả các đơn hàng của shop (hỗ trợ phân trang và lọc theo trạng thái).
     * @param {string} shopId - ID của shop
     * @param {number} page - Số trang (mặc định 0)
     * @param {string|null} status - Bộ lọc trạng thái (PENDING, PROCESSING, DELIVERING, COMPLETED, CANCELLED...)
     */
    list: (shopId, page = 0, status = null) => {
        const params = new URLSearchParams()
        if (page !== undefined && page !== null && page !== '') params.set('page', page)
        if (status) params.set('status', status)
        return authService.authenticatedRequest(`${base(shopId)}?${params.toString()}`)
    },

    /**
     * Lấy danh sách đơn hàng trong hàng đợi cần xử lý (PENDING, AWAITING_DEPOSIT, PROCESSING).
     * @param {string} shopId - ID của shop
     * @param {number} page - Số trang
     * @param {string|null} status - Trạng thái lọc
     */
    getQueue: (shopId, page = 0, status = null) => {
        const params = new URLSearchParams()
        if (page !== undefined && page !== null && page !== '') params.set('page', page)
        if (status) params.set('status', status)
        return authService.authenticatedRequest(`${base(shopId)}/queue?${params.toString()}`)
    },

    /**
     * Lấy số lượng đơn hàng đang chờ xác nhận để hiển thị badge thông báo đỏ.
     * @param {string} shopId - ID của shop
     */
    getQueueCount: (shopId) => authService.authenticatedRequest(`${base(shopId)}/queue/count`),

    /**
     * Hủy đơn hàng bởi chủ shop kèm theo lý do hủy.
     * @param {string} shopId - ID của shop
     * @param {string} orderId - ID đơn hàng cần hủy
     * @param {string} reason - Lý do hủy
     */
    cancelOrder: (shopId, orderId, reason) => authService.authenticatedRequest(`${base(shopId)}/${encodeURIComponent(orderId)}/cancel`, {
        method: 'POST',
        body: { reason }
    }),

    /**
     * Lấy thông tin chi tiết một đơn hàng (sản phẩm, địa chỉ gửi/nhận, vận đơn GHN, thanh toán).
     * @param {string} shopId - ID của shop
     * @param {string} id - ID đơn hàng
     */
    detail: (shopId, id) => authService.authenticatedRequest(`${base(shopId)}/${encodeURIComponent(id)}`),

    /**
     * Xác nhận đơn hàng (PENDING -> PROCESSING: Đang chuẩn bị hoa).
     * @param {string} shopId - ID của shop
     * @param {string} id - ID đơn hàng
     */
    confirm: (shopId, id) => authService.authenticatedRequest(`${base(shopId)}/${encodeURIComponent(id)}/confirm`, { method: 'POST' }),

    /**
     * Tạo mã vận đơn trên hệ thống Giao Hàng Nhanh (GHN) và chuyển đơn sang DELIVERING.
     * @param {string} shopId - ID của shop
     * @param {string} id - ID đơn hàng
     */
    ship: (shopId, id) => authService.authenticatedRequest(`${base(shopId)}/${encodeURIComponent(id)}/ship`, { method: 'POST' }),

    /**
     * Đồng bộ / Cập nhật trạng thái giao hàng mới nhất từ GHN về hệ thống.
     * @param {string} shopId - ID của shop
     * @param {string} id - ID đơn hàng
     */
    refreshStatus: (shopId, id) => authService.authenticatedRequest(`${base(shopId)}/${encodeURIComponent(id)}/refresh-status`, { method: 'POST' }),

    /**
     * Giả lập giao hàng thành công trên môi trường thử nghiệm (Sandbox GHN).
     * @param {string} shopId - ID của shop
     * @param {string} id - ID đơn hàng
     */
    simulateDelivered: (shopId, id) => authService.authenticatedRequest(`${base(shopId)}/${encodeURIComponent(id)}/simulate-delivered`, { method: 'POST' }),
    
    /**
     * Cập nhật thông tin vận đơn GHN cho đơn hàng.
     * @param {string} shopId - ID của shop
     * @param {string} id - ID đơn hàng
     * @param {object} payload - Các trường cần cập nhật (weight, note, to_name, to_phone...)
     */
    updateGhnOrder: (shopId, id, payload) => authService.authenticatedRequest(`${base(shopId)}/${encodeURIComponent(id)}/update-ghn`, { method: 'POST', body: payload }),
}
