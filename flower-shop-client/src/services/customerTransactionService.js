import { authService } from './authService'

// GET /api/customer/transactions (CustomerTransactionController, quyền ROLE_CUSTOMER): lịch sử giao dịch của khách
export const customerTransactionService = {
    list: ({ page = 0, status = '', from = '', to = '' } = {}) => {
        const q = new URLSearchParams({ page: String(page) })
        if (status) q.set('status', status)
        if (from) q.set('from', from)
        if (to) q.set('to', to)
        return authService.authenticatedRequest(`/api/customer/transactions?${q}`)
    },
}
