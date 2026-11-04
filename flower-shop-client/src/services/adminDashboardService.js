import { authService } from './authService'

export const adminDashboardService = {
  get: (granularity, range) => {
    const params = new URLSearchParams({ granularity })
    if (range?.from && range?.to) { params.set('from', range.from); params.set('to', range.to) }
    return authService.authenticatedRequest(`/api/admin/dashboard?${params}`)
  },
}
