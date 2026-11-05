import { authService } from './authService'

// Gọi API đơn hàng của khách: /api/customer/orders (CustomerOrderController, quyền ROLE_CUSTOMER)
const req = (path, options) => authService.authenticatedRequest(`/api/customer/orders${path}`, options)

export const customerOrderService = {
    // POST /fee: tính phí ship GHN cho 1 sản phẩm + địa chỉ đã lưu (CheckoutView)
    fee: data => req('/fee', { method: 'POST', body: data }),
    // POST /checkout: đặt hàng (CheckoutView)
    checkout: data => req('/checkout', { method: 'POST', body: data }),
    // GET /by-ids?ids=a,b: các đơn vừa đặt, dùng ở CodSuccessPage và PaymentPendingPage
    byIds: ids => req(`/by-ids?ids=${ids.map(encodeURIComponent).join(',')}`),
    // GET ?page=&status=: lịch sử đơn của khách (CustomerOrdersView)
    list: (page, status) => req(`?page=${page}${status ? `&status=${encodeURIComponent(status)}` : ''}`),
    // GET /{id}: chi tiết đơn của khách kèm vận đơn (CustomerOrderDetailView)
    detail: id => req(`/${encodeURIComponent(id)}`),
    // GET /{id}/shipping-fee: phí ship GHN hiện tại của đơn (CustomerOrderDetailView)
    shippingFee: id => req(`/${encodeURIComponent(id)}/shipping-fee`),
    // POST /{id}/refresh-tracking: backend hỏi GHN trạng thái giao hàng mới nhất
    refreshTracking: id => req(`/${encodeURIComponent(id)}/refresh-tracking`, { method: 'POST' }),
}
