import { authService } from './authService'

// Dịp đặc biệt của khách: /api/customer/occasions (OccasionController, quyền ROLE_CUSTOMER)
const base = '/api/customer/occasions'

export const occasionService = {
    // GET: các dịp của khách, sắp theo lần diễn ra gần nhất
    mine: () => authService.authenticatedRequest(base),
    // POST: { title, occasionDate, notifyAt?, recurringYearly }
    create: data => authService.authenticatedRequest(base, { method: 'POST', body: data }),
    // PUT /{id}: cùng dạng dữ liệu như create
    update: (id, data) => authService.authenticatedRequest(`${base}/${encodeURIComponent(id)}`, { method: 'PUT', body: data }),
    // DELETE /{id}
    remove: id => authService.authenticatedRequest(`${base}/${encodeURIComponent(id)}`, { method: 'DELETE' }),
}
