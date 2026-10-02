import { authService } from './authService'

const request = (path, options) => authService.authenticatedRequest(path, options)

// AdminCategoriesView -> hook -> file này -> authService.authenticatedRequest -> AdminCatalogController.
// list/create/update thao tác Categories; products/setHidden thao tác kiểm duyệt Products.
export const adminCategoryService = {
  list: () => request('/api/admin/categories'),
  create: body => request('/api/admin/categories', { method: 'POST', body }),
  update: (id, body) => request(`/api/admin/categories/${encodeURIComponent(id)}`, { method: 'PUT', body }),
  products: params => request(`/api/admin/products?${new URLSearchParams(params)}`),
  setHidden: (id, hidden) => request(`/api/admin/products/${encodeURIComponent(id)}/hidden`, { method: 'PUT', body: { hidden } }),
}
