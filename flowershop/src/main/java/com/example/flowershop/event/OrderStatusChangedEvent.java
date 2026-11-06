package com.example.flowershop.event;

import com.example.flowershop.entity.enums.OrderStatus;

/** Trạng thái đơn hàng đã đổi; phát tự động từ {@code OrderStatusListener} mỗi khi cập nhật Order. */
public record OrderStatusChangedEvent(String orderId, OrderStatus from, OrderStatus to) {
}
