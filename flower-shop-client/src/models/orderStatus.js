export const orderStatusLabels = {
    AWAITING_QUOTE: 'Chờ shop báo giá',
    QUOTED: 'Chờ khách đồng ý báo giá',
    PENDING: 'Chờ xác nhận',
    AWAITING_DEPOSIT: 'Chờ đặt cọc',
    DEPOSIT_PAID: 'Đã cọc',
    PROCESSING: 'Chờ giao hàng',
    DELIVERING: 'Đang giao',
    COMPLETED: 'Giao thành công',
    CANCELLED: 'Đã hủy',
    REFUNDED: 'Hoàn hàng',
}

export const orderStatusColors = {
    AWAITING_QUOTE: 'warning',
    QUOTED: 'info',
    PENDING: 'warning',
    AWAITING_DEPOSIT: 'warning',
    DEPOSIT_PAID: 'info',
    PROCESSING: 'info',
    DELIVERING: 'primary',
    COMPLETED: 'success',
    CANCELLED: 'error',
    REFUNDED: 'error',
}

export const orderTabs = [
    { id: '', label: 'Tất cả' },
    { id: 'AWAITING_QUOTE', label: orderStatusLabels.AWAITING_QUOTE },
    { id: 'QUOTED', label: orderStatusLabels.QUOTED },
    { id: 'PENDING', label: orderStatusLabels.PENDING },
    { id: 'PROCESSING', label: orderStatusLabels.PROCESSING },
    { id: 'DELIVERING', label: orderStatusLabels.DELIVERING },
    { id: 'COMPLETED', label: orderStatusLabels.COMPLETED },
    { id: 'CANCELLED', label: orderStatusLabels.CANCELLED },
    { id: 'REFUNDED', label: orderStatusLabels.REFUNDED },
]

// Status codes as GHN reports them: https://developer.ghn.vn/en/docs/order/update
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

// Actions the shop can send to GHN (switch-status), and the GHN statuses each is allowed from.
export const ghnActions = {
    cancel: { label: 'Hủy vận đơn', from: ['ready_to_pick', 'picking'], danger: true },
    storing: { label: 'Yêu cầu giao lại', from: ['storing', 'waiting_to_return'] },
    return: { label: 'Yêu cầu trả hàng', from: ['delivery_fail', 'storing', 'waiting_to_return', 'return'], danger: true },
}

export const ghnStages = [
    { label: 'Lấy hàng', statuses: ['ready_to_pick', 'picking', 'picked'] },
    { label: 'Luân chuyển', statuses: ['storing', 'sorting', 'transporting'] },
    { label: 'Giao hàng', statuses: ['delivering', 'delivery_fail', 'delivered'] },
    { label: 'Hoàn hàng', statuses: ['waiting_to_return', 'returning', 'returned'] },
]
