package com.example.flowershop.service;

import com.example.flowershop.entity.Delivery;
import com.example.flowershop.entity.Order;
import com.example.flowershop.entity.enums.OrderStatus;
import com.example.flowershop.entity.enums.PaymentMethod;
import com.example.flowershop.entity.enums.PaymentStatus;
import com.example.flowershop.repository.PaymentRepository;
import org.springframework.stereotype.Component;

/**
 * Đồng bộ trạng thái GHN báo về vào Delivery và, khi vận đơn kết thúc, vào Order.
 * delivered -> COMPLETED (và đánh dấu khoản COD SUCCESS), returned -> REFUNDED, cancel -> CANCELLED.
 */
@Component
public class DeliverySync {
    private final PaymentRepository payments;

    public DeliverySync(PaymentRepository payments) { this.payments = payments; }

    /** Cập nhật Delivery theo tracking; chỉ đổi trạng thái Order khi đơn đang DELIVERING. */
    public void apply(Order order, Delivery delivery, GhnService.Tracking tracking, String actor) {
        // Luôn lưu trạng thái thô của GHN và trạng thái rút gọn của hệ thống
        delivery.setStatus(tracking.status());
        delivery.setGhnStatus(tracking.ghnStatus());
        delivery.setLastModifyBy(actor);
        if (order.getStatus() != OrderStatus.DELIVERING) return;
        switch (tracking.ghnStatus()) {
            case "delivered" -> {
                order.setStatus(OrderStatus.COMPLETED);
                // Chỉ tiền GHN thu hộ; lần thanh toán online FAILED/hủy không được thành SUCCESS
                payments.findByOrderId(order.getId()).stream()
                        .filter(p -> p.getPaymentMethod() == PaymentMethod.COD).forEach(p -> {
                    p.setStatus(PaymentStatus.SUCCESS);
                    p.setLastModifyBy(actor);
                });
            }
            case "returned" -> order.setStatus(OrderStatus.REFUNDED);
            case "cancel" -> {
                order.setStatus(OrderStatus.CANCELLED);
                order.setCancelReason("Vận đơn bị hủy trên GHN.");
            }
            default -> { return; }
        }
        order.setLastModifyBy(actor);
    }
}
