package com.example.flowershop.scheduler;

import com.example.flowershop.entity.Order;
import com.example.flowershop.entity.enums.OrderStatus;
import com.example.flowershop.repository.OrderRepository;
import com.example.flowershop.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentTimeoutScheduler {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    /**
     * Chạy định kỳ mỗi 30 giây để kiểm tra và tự động hủy các đơn hàng trực tuyến
     * đã quá 10 phút kể từ lúc tạo mà khách hàng chưa hoàn tất thanh toán.
     */
    @Scheduled(fixedDelay = 30000)
    public void scanAndCancelExpiredOrders() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(10);
        List<Order> expiredOrders = orderRepository.findByStatusAndCreatedDateBefore(OrderStatus.PENDING, threshold);

        if (!expiredOrders.isEmpty()) {
            log.info("Tìm thấy {} đơn hàng PENDING quá hạn 10 phút, tiến hành hủy tự động...", expiredOrders.size());
            for (Order order : expiredOrders) {
                try {
                    orderService.cancelExpiredOrder(order.getId(), "Quá hạn 10 phút không quét mã thanh toán");
                } catch (Exception e) {
                    log.error("Lỗi khi hủy đơn hàng hết hạn #{}: {}", order.getId(), e.getMessage(), e);
                }
            }
        }
    }
}
