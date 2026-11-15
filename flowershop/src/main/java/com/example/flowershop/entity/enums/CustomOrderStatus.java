package com.example.flowershop.entity.enums;

/** QUOTED: shop đã báo giá, chờ khách chấp nhận hoặc từ chối. ACCEPTED: khách đã chấp nhận (đã/đang đặt cọc). */
public enum CustomOrderStatus {
    REQUESTED, NEGOTIATING, QUOTED, ACCEPTED, REJECTED
}
