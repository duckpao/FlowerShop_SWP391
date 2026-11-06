package com.example.flowershop.event;

import java.util.List;

/** Một lần đặt hàng vừa tạo xong (mỗi shop một đơn, cùng checkout nhóm thì gộp một biên lai). */
public record OrderPlacedEvent(List<String> orderIds) {
}
