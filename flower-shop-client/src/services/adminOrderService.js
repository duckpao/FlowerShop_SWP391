import { authService } from './authService'

const qs = params => new URLSearchParams(Object.entries(params).filter(([, v]) => v !== '' && v != null)).toString()

/** Admin xem đơn hàng toàn hệ thống (chỉ đọc). Luồng: AdminOrdersView -> adminOrderService -> AdminOrderController. */
export const adminOrderService = {
    list: params => authService.authenticatedRequest(`/api/admin/orders?${qs(params)}`),
    detail: id => authService.authenticatedRequest(`/api/admin/orders/${encodeURIComponent(id)}`),
}
