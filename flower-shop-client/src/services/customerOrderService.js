import { authService } from './authService'

const req = (path, options) => authService.authenticatedRequest(`/api/customer/orders${path}`, options)

export const customerOrderService = {
    fee: data => req('/fee', { method: 'POST', body: data }),
    checkout: data => req('/checkout', { method: 'POST', body: data }),
    list: page => req(`?page=${page}`),
    detail: id => req(`/${encodeURIComponent(id)}`),
}
