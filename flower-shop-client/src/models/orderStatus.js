// Nhãn tiếng Việt cho trạng thái đơn hàng của hệ thống
export const orderStatusLabels = {
    PENDING: 'Chờ xác nhận',
    AWAITING_DEPOSIT: 'Chờ đặt cọc',
    DEPOSIT_PAID: 'Đã cọc',
    PROCESSING: 'Chờ giao hàng',
    DELIVERING: 'Đang giao',
    COMPLETED: 'Giao thành công',
    CANCELLED: 'Đã hủy',
    REFUNDED: 'Hoàn hàng',
}

// Màu Badge tương ứng với từng trạng thái đơn
export const orderStatusColors = {
    PENDING: 'warning',
    AWAITING_DEPOSIT: 'warning',
    DEPOSIT_PAID: 'info',
    PROCESSING: 'info',
    DELIVERING: 'primary',
    COMPLETED: 'success',
    CANCELLED: 'error',
    REFUNDED: 'error',
}

// Các tab lọc đơn trong màn hình quản lý đơn hàng ('' = tất cả)
export const orderTabs = [
    { id: '', label: 'Tất cả' },
    { id: 'PENDING', label: orderStatusLabels.PENDING },
    { id: 'PROCESSING', label: orderStatusLabels.PROCESSING },
    { id: 'DELIVERING', label: orderStatusLabels.DELIVERING },
    { id: 'COMPLETED', label: orderStatusLabels.COMPLETED },
    { id: 'CANCELLED', label: orderStatusLabels.CANCELLED },
    { id: 'REFUNDED', label: orderStatusLabels.REFUNDED },
]

// Nhãn tiếng Việt cho mã trạng thái vận đơn GHN. Status codes as GHN reports them: https://developer.ghn.vn/en/docs/order/update
export const ghnStatusLabels = {
    ready_to_pick: 'Mới tạo, chờ lấy hàng',
    picking: 'Đang lấy hàng',
    picked: 'Đã lấy hàng',
    storing: 'Nhập kho',
    sorting: 'Đang phân loại',
    transporting: 'Đang luân chuyển',
    delivering: 'Đang giao hàng',
    delivered: 'Giao thành công',
    delivery_fail: 'Giao thất bại',
    waiting_to_return: 'Chờ trả hàng',
    returning: 'Đang hoàn hàng',
    returned: 'Đã hoàn hàng',
    cancel: 'Đã hủy vận đơn',
}

// Thao tác shop được gửi sang GHN và các trạng thái GHN cho phép thao tác đó. Actions the shop can send to GHN (switch-status), and the GHN statuses each is allowed from.
export const ghnActions = {
    cancel: { label: 'Hủy vận đơn', from: ['ready_to_pick', 'picking'], danger: true },
    storing: { label: 'Yêu cầu giao lại', from: ['storing', 'waiting_to_return'] },
    return: { label: 'Yêu cầu trả hàng', from: ['delivery_fail', 'storing', 'waiting_to_return', 'return'], danger: true },
}

// 4 chặng hiển thị trên thanh tiến trình của GhnSimulator
export const ghnStages = [
    { label: 'Lấy hàng', statuses: ['ready_to_pick', 'picking', 'picked'] },
    { label: 'Luân chuyển', statuses: ['storing', 'sorting', 'transporting'] },
    { label: 'Giao hàng', statuses: ['delivering', 'delivery_fail', 'delivered'] },
    { label: 'Hoàn hàng', statuses: ['waiting_to_return', 'returning', 'returned'] },
]
