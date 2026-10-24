import { authService } from './authService'

/**
 * Đường dẫn gốc API quản lý đơn hàng của shop
 * @param {string} shopId - ID cửa hàng
 */
const base = shopId => `/api/shop/mine/${encodeURIComponent(shopId)}/orders`

/**
 * Service giao tiếp với Backend phục vụ chức năng Quản lý đơn hàng (Shop Manager).
 * Đính kèm JWT token và tự động xử lý CSRF token khi gửi request.
 *
 * Luồng gọi: View/Component -> managerOrderService -> authService.authenticatedRequest (fetch)
 *   -> ManagerOrderController (Spring Boot, quyền ROLE_SHOP) -> ManagerOrderService
 *   -> (với các thao tác vận đơn) GhnService -> API Giao Hàng Nhanh.
 * Được dùng bởi: OrderQueueView, GhnSimulator.
 */
export const managerOrderService = {
    /**
     * Lấy danh sách tất cả các đơn hàng của shop (hỗ trợ phân trang và lọc theo trạng thái).
     * Gọi: GET /api/shop/mine/{shopId}/orders?page=&status= -> ManagerOrderController.list
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
     * Gọi: GET /api/shop/mine/{shopId}/orders/queue?page=&status= -> ManagerOrderController.queue
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
     * Gọi: GET /api/shop/mine/{shopId}/orders/queue/count -> ManagerOrderController.queueCount
     * @param {string} shopId - ID của shop
     */
    getQueueCount: (shopId) => authService.authenticatedRequest(`${base(shopId)}/queue/count`),

    /**
     * Hủy đơn hàng bởi chủ shop kèm theo lý do hủy.
     * Gọi: POST /api/shop/mine/{shopId}/orders/{id}/cancel -> ManagerOrderController.cancel
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
     * Gọi: GET /api/shop/mine/{shopId}/orders/{id} -> ManagerOrderController.detail
     * @param {string} shopId - ID của shop
     * @param {string} id - ID đơn hàng
     */
    detail: (shopId, id) => authService.authenticatedRequest(`${base(shopId)}/${encodeURIComponent(id)}`),

    /**
     * Xác nhận đơn hàng (PENDING -> PROCESSING: Đang chuẩn bị hoa).
     * Gọi: POST /api/shop/mine/{shopId}/orders/{id}/confirm -> ManagerOrderController.confirm
     * @param {string} shopId - ID của shop
     * @param {string} id - ID đơn hàng
     */
    confirm: (shopId, id) => authService.authenticatedRequest(`${base(shopId)}/${encodeURIComponent(id)}/confirm`, { method: 'POST' }),

    /**
     * Tạo mã vận đơn trên hệ thống Giao Hàng Nhanh (GHN) và chuyển đơn sang DELIVERING.
     * Gọi: POST /api/shop/mine/{shopId}/orders/{id}/ship -> ManagerOrderController.ship
     *   -> ManagerOrderService.ship -> GhnService.createOrder -> GHN POST /v2/shipping-order/create.
     * Trả về { trackingCode }.
     * @param {string} shopId - ID của shop
     * @param {string} id - ID đơn hàng
     */
    ship: (shopId, id) => authService.authenticatedRequest(`${base(shopId)}/${encodeURIComponent(id)}/ship`, { method: 'POST' }),

    /**
     * Đồng bộ / Cập nhật trạng thái giao hàng mới nhất từ GHN về hệ thống.
     * Gọi: POST /api/shop/mine/{shopId}/orders/{id}/refresh-status -> ManagerOrderController.refreshStatus
     *   -> GhnService.track -> GHN GET /v2/shipping-order/detail, rồi DeliverySync cập nhật Delivery/Order.
     * @param {string} shopId - ID của shop
     * @param {string} id - ID đơn hàng
     */
    refreshStatus: (shopId, id) => authService.authenticatedRequest(`${base(shopId)}/${encodeURIComponent(id)}/refresh-status`, { method: 'POST' }),

    /**
     * Gửi một thao tác trạng thái sang GHN (switch-status) và nhận lại trạng thái GHN báo về.
     * Gọi: POST /api/shop/mine/{shopId}/orders/{id}/ghn-status -> ManagerOrderController.pushGhnStatus
     *   -> GhnService.switchStatus (GHN POST /v2/switch-status/{action}) rồi GhnService.track để đọc lại.
     * @param {string} shopId - ID của shop
     * @param {string} id - ID đơn hàng
     * @param {string} status - Thao tác gửi GHN: 'cancel' | 'return' | 'storing' (xem ghnActions trong orderStatus.js)
     */
    pushGhnStatus: (shopId, id, status) => authService.authenticatedRequest(`${base(shopId)}/${encodeURIComponent(id)}/ghn-status`, { method: 'POST', body: { status } }),

    /**
     * Đổi trạng thái đơn trực tiếp trong DB, không gọi GHN. Trả về { status, warning }.
     * Gọi: POST /api/shop/mine/{shopId}/orders/{id}/set-status -> ManagerOrderController.setStatus
     * @param {string} shopId - ID của shop
     * @param {string} id - ID đơn hàng
     * @param {string} status - Trạng thái mới (PENDING, PROCESSING, DELIVERING, COMPLETED, CANCELLED, REFUNDED...)
     */
    setStatus: (shopId, id, status) => authService.authenticatedRequest(`${base(shopId)}/${encodeURIComponent(id)}/set-status`, { method: 'POST', body: { status } }),

    /**
     * Cập nhật thông tin vận đơn GHN cho đơn hàng.
     * Gọi: POST /api/shop/mine/{shopId}/orders/{id}/update-ghn -> ManagerOrderController.updateGhn
     *   -> GhnService.updateOrder -> GHN POST /v2/shipping-order/update.
     * @param {string} shopId - ID của shop
     * @param {string} id - ID đơn hàng
     * @param {object} payload - Các trường cần cập nhật (weight, note, to_name, to_phone...)
     */
    updateGhnOrder: (shopId, id, payload) => authService.authenticatedRequest(`${base(shopId)}/${encodeURIComponent(id)}/update-ghn`, { method: 'POST', body: payload }),
}
