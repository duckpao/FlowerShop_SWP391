package com.example.flowershop.scheduler;

import com.example.flowershop.entity.Order;
import com.example.flowershop.entity.enums.OrderStatus;
import com.example.flowershop.repository.OrderRepository;
import com.example.flowershop.service.SePayGatewayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentTimeoutScheduler {

    private final OrderRepository orderRepository;
    private final SePayGatewayService sePayGatewayService;

    /**
     * Chạy định kỳ mỗi 30 giây để kiểm tra và tự động hủy các đơn hàng trực tuyến
     * đã quá 10 phút (PENDING) hoặc 24 giờ (AWAITING_DEPOSIT) kể từ lúc tạo mà khách hàng chưa thanh toán.
     */
    @Scheduled(fixedDelay = 30000)
    public void scanAndCancelExpiredOrders() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(SePayGatewayService.PAYMENT_WINDOW_MINUTES);
        List<Order> expiredOrders = new ArrayList<>(
                orderRepository.findByStatusAndCreatedDateBefore(OrderStatus.PENDING, threshold));
        // Đơn đặt cọc (AWAITING_DEPOSIT) quá hạn cọc thì cũng hủy.
        expiredOrders.addAll(orderRepository.findByStatusAndCreatedDateBefore(OrderStatus.AWAITING_DEPOSIT,
                LocalDateTime.now().minusHours(SePayGatewayService.DEPOSIT_DEADLINE_HOURS)));

        if (!expiredOrders.isEmpty()) {
            log.info("Tìm thấy {} đơn hàng chờ thanh toán/chờ cọc quá hạn, tiến hành hủy tự động...", expiredOrders.size());
            for (Order order : expiredOrders) {
                try {
                    sePayGatewayService.expireUnpaidOrder(order.getId());
                } catch (Exception e) {
                    log.error("Lỗi khi hủy đơn hàng hết hạn #{}: {}", order.getId(), e.getMessage(), e);
                }
            }
        }
    }
}
