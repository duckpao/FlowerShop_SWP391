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
     * Cronjob chạy định kỳ mỗi 30 giây (fixedDelay = 30000ms) để kiểm tra và tự động hủy các đơn hàng online:
     * - Trạng thái PENDING: quá hạn thanh toán 10 phút (SePayGatewayService.PAYMENT_WINDOW_MINUTES).
     * - Trạng thái AWAITING_DEPOSIT: quá hạn chuyển cọc 24 giờ (SePayGatewayService.DEPOSIT_DEADLINE_HOURS).
     * 
     * Luồng thực thi:
     * 1. Tính mốc thời gian threshold timeout.
     * 2. Quét cơ sở dữ liệu lấy danh sách đơn PENDING và AWAITING_DEPOSIT quá hạn.
     * 3. Với từng đơn quá hạn, gọi sePayGatewayService.expireUnpaidOrder():
     *    - Đối soát giao dịch với SePay (nếu khách chuyển tiền sát nút thì cập nhật PROCESSING).
     *    - Đóng/Hủy invoice trên SePay nếu chưa thanh toán.
     *    - Cập nhật Order status -> CANCELLED, Payment status -> EXPIRED.
     *    - Hoàn trả lại tồn kho sản phẩm cho Shop (orderService.restoreCancelledOrderItems).
     */
    @Scheduled(fixedDelay = 30000)
    public void scanAndCancelExpiredOrders() {
        // Mốc thời gian timeout cho đơn thanh toán toàn bộ (10 phút trước)
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(SePayGatewayService.PAYMENT_WINDOW_MINUTES);
        
        // Quét lấy danh sách đơn PENDING quá hạn thanh toán 10 phút
        List<Order> expiredOrders = new ArrayList<>(
                orderRepository.findByStatusAndCreatedDateBefore(OrderStatus.PENDING, threshold));
                
        // Quét tiếp lấy danh sách đơn AWAITING_DEPOSIT quá hạn đặt cọc 24 giờ
        expiredOrders.addAll(orderRepository.findByStatusAndCreatedDateBefore(OrderStatus.AWAITING_DEPOSIT,
                LocalDateTime.now().minusHours(SePayGatewayService.DEPOSIT_DEADLINE_HOURS)));

        if (!expiredOrders.isEmpty()) {
            log.info("Tìm thấy {} đơn hàng chờ thanh toán/chờ cọc quá hạn, tiến hành hủy tự động...", expiredOrders.size());
            for (Order order : expiredOrders) {
                try {
                    // Tiến hành hủy đơn, hủy cổng SePay và hoàn lại tồn kho
                    sePayGatewayService.expireUnpaidOrder(order.getId());
                } catch (Exception e) {
                    log.error("Lỗi khi hủy đơn hàng hết hạn #{}: {}", order.getId(), e.getMessage(), e);
                }
            }
        }
    }
}
