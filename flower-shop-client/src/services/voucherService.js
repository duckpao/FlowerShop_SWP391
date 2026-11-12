import { authService } from './authService'

const base = shopId => shopId ? `/api/shop/mine/${encodeURIComponent(shopId)}/vouchers` : '/api/admin/vouchers'
export const voucherService = {
  list: shopId => authService.authenticatedRequest(base(shopId)),
  save: (shopId, id, body) => authService.authenticatedRequest(`${base(shopId)}${id ? `/${encodeURIComponent(id)}` : ''}`, { method: id ? 'PUT' : 'POST', body }),
  remove: (shopId, id) => authService.authenticatedRequest(`${base(shopId)}/${encodeURIComponent(id)}`, { method: 'DELETE' }),
  preview: ({ mode, userId, orderId, requestId, revision: _revision, ...body }) => {
    const path = mode === 'cart' ? `/api/orders/${encodeURIComponent(userId)}/voucher-preview`
      : mode === 'deposit' ? `/api/orders/${encodeURIComponent(userId)}/custom/${encodeURIComponent(requestId)}/voucher-preview`
      : mode === 'custom' ? `/api/customer/orders/${encodeURIComponent(orderId)}/voucher-preview`
        : '/api/customer/orders/voucher-preview'
    if (mode === 'cart') { body.couponId = body.couponCode; delete body.couponCode }
    return authService.authenticatedRequest(path, { method: 'POST', body })
  },
}
