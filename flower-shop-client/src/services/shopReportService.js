import { authService } from './authService'

export const shopReportService = {
  get: (shopId, period = 'MONTH', value = '') => {
    const params = new URLSearchParams({ period })
    if (value) params.set('value', value)
    return authService.authenticatedRequest(
      `/api/shop/mine/${encodeURIComponent(shopId)}/reports?${params}`,
    )
  },
}
