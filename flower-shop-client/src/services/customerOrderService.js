import { authService } from './authService'

// Gọi API đơn hàng của khách: /api/customer/orders (CustomerOrderController, quyền ROLE_CUSTOMER)
const req = (path, options) => authService.authenticatedRequest(`/api/customer/orders${path}`, options)

export const customerOrderService = {
    // GET /by-ids?ids=a,b: tóm tắt các đơn theo id (CodSuccessPage, PaymentPendingPage)
    byIds: ids => req(`/by-ids?ids=${ids.map(encodeURIComponent).join(',')}`),
    // GET ?page=&status=: lịch sử đơn của khách (CustomerOrdersView)
    list: (page, status) => req(`?page=${page}${status ? `&status=${encodeURIComponent(status)}` : ''}`),
    // GET /{id}: chi tiết đơn của khách kèm vận đơn (CustomerOrderDetailView)
    detail: id => req(`/${encodeURIComponent(id)}`),
    // GET /{id}/shipping-fee: phí ship GHN hiện tại của đơn (CustomerOrderDetailView)
    shippingFee: id => req(`/${encodeURIComponent(id)}/shipping-fee`),
    // PUT /{id}/address: đổi địa chỉ nhận sang địa chỉ đã lưu, chỉ khi đơn chưa giao
    changeAddress: (id, addressId) => req(`/${encodeURIComponent(id)}/address`, { method: 'PUT', body: { addressId } }),
    // POST /{id}/refresh-tracking: backend hỏi GHN trạng thái giao hàng mới nhất
    refreshTracking: id => req(`/${encodeURIComponent(id)}/refresh-tracking`, { method: 'POST' }),
    // UC23: khách gửi / hủy yêu cầu hoàn tiền, tải ảnh bằng chứng
    requestRefund: (id, body) => req(`/${encodeURIComponent(id)}/refunds`, { method: 'POST', body }),
    cancelRefund: (id, refundId) => req(`/${encodeURIComponent(id)}/refunds/${encodeURIComponent(refundId)}/cancel`, { method: 'POST' }),
    uploadRefundImage: (id, file) => {
        const body = new FormData()
        body.append('file', file)
        return req(`/${encodeURIComponent(id)}/refunds/images`, { method: 'POST', body })
    },
}
