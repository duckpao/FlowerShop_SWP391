package com.example.flowershop.service;

import com.example.flowershop.entity.Delivery;
import com.example.flowershop.entity.Order;
import com.example.flowershop.entity.enums.OrderStatus;
import com.example.flowershop.entity.enums.PaymentStatus;
import com.example.flowershop.repository.PaymentRepository;
import org.springframework.stereotype.Component;

/** Applies a status reported by GHN to the delivery and, when it ends the shipment, to the order. */
@Component
public class DeliverySync {
    private final PaymentRepository payments;

    public DeliverySync(PaymentRepository payments) { this.payments = payments; }

    public void apply(Order order, Delivery delivery, GhnService.Tracking tracking, String actor) {
        delivery.setStatus(tracking.status());
        delivery.setGhnStatus(tracking.ghnStatus());
        delivery.setLastModifyBy(actor);
        if (order.getStatus() != OrderStatus.DELIVERING) return;
        switch (tracking.ghnStatus()) {
            case "delivered" -> {
                order.setStatus(OrderStatus.COMPLETED);
                payments.findByOrderId(order.getId()).forEach(p -> {
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
