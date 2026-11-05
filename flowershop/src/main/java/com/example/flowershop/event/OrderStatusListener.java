package com.example.flowershop.event;

import com.example.flowershop.entity.Order;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostUpdate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * JPA entity listener của {@link Order}: nhớ trạng thái lúc nạp từ DB và phát
 * {@link OrderStatusChangedEvent} khi trạng thái thay đổi, để mọi luồng đổi trạng thái
 * (shop, GHN, thanh toán, hết hạn) đều gửi mail mà không phải sửa từng nơi.
 * Đơn mới tạo trong cùng transaction không có ảnh chụp nên không phát event; biên lai do OrderPlacedEvent lo.
 */
@Component
public class OrderStatusListener {

    private final ApplicationEventPublisher publisher;

    public OrderStatusListener(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @PostLoad
    public void remember(Order order) {
        order.setStatusSnapshot(order.getStatus());
    }

    @PostUpdate
    public void publishIfChanged(Order order) {
        var before = order.getStatusSnapshot();
        var after = order.getStatus();
        if (before == null || after == null || before == after) return;
        order.setStatusSnapshot(after);
        publisher.publishEvent(new OrderStatusChangedEvent(order.getId(), before, after));
    }
}
