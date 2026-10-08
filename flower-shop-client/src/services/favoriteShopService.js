import { authService } from './authService'

const base = '/api/customer/favorite-shops'

export const favoriteShopService = {
    // Lấy tất cả cửa hàng mà user đang theo dõi
    mine: () =>
        authService.authenticatedRequest(base),

    // Kiểm tra user đã theo dõi cửa hàng chưa
    status: (shopId) =>
        authService.authenticatedRequest(
            `${base}/${encodeURIComponent(shopId)}`
        ),

    // Theo dõi cửa hàng
    add: (shopId) =>
        authService.authenticatedRequest(
            `${base}/${encodeURIComponent(shopId)}`,
            {
                method: 'PUT',
            }
        ),

    // Bỏ theo dõi cửa hàng
    remove: (shopId) =>
        authService.authenticatedRequest(
            `${base}/${encodeURIComponent(shopId)}`,
            {
                method: 'DELETE',
            }
        ),
}