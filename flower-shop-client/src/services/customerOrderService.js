import { authService } from './authService'

const req = (path, options) => authService.authenticatedRequest(`/api/customer/orders${path}`, options)

export const customerOrderService = {
    createCustom: data => req('/custom', { method: 'POST', body: data }),
    acceptQuote: (id, paymentMethod, expectedTotal) => req(`/${encodeURIComponent(id)}/accept-quote`, { method: 'POST', body: { paymentMethod, expectedTotal } }),
    customAddress: (id, addressId) => req(`/${encodeURIComponent(id)}/custom-address`, { method: 'PUT', body: { addressId } }),
    fee: data => req('/fee', { method: 'POST', body: data }),
    checkout: data => req('/checkout', { method: 'POST', body: data }),
    list: (page, status) => req(`?page=${page}${status ? `&status=${encodeURIComponent(status)}` : ''}`),
    detail: id => req(`/${encodeURIComponent(id)}`),
    shippingFee: id => req(`/${encodeURIComponent(id)}/shipping-fee`),
    refreshTracking: id => req(`/${encodeURIComponent(id)}/refresh-tracking`, { method: 'POST' }),
}
