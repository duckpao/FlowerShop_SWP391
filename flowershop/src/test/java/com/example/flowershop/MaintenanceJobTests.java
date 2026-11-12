package com.example.flowershop;

import com.example.flowershop.entity.Coupon;
import com.example.flowershop.entity.Order;
import com.example.flowershop.entity.Payment;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.CartItemRepository;
import com.example.flowershop.repository.CouponRepository;
import com.example.flowershop.repository.OrderRepository;
import com.example.flowershop.repository.PaymentRepository;
import com.example.flowershop.scheduler.MaintenanceJob;
import com.example.flowershop.service.CouponService;
import com.example.flowershop.service.DeliverySync;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Ba job định kỳ SRS 5.3: hết hạn mã giảm giá, tự hoàn tất đơn đã giao, dọn giỏ hàng bỏ quên. */
class MaintenanceJobTests {
    private final CouponRepository coupons = mock(CouponRepository.class);
    private final OrderRepository orders = mock(OrderRepository.class);
    private final CartItemRepository carts = mock(CartItemRepository.class);
    private final PaymentRepository payments = mock(PaymentRepository.class);
    private final MaintenanceJob job = new MaintenanceJob(coupons, orders, carts,
            new DeliverySync(payments, mock(CouponService.class)));

    @Test
    void endedActiveCouponsBecomeExpired() {
        Coupon ended = Coupon.builder().id("c-1").code("OLD").build();
        when(coupons.activeEndedBefore(any())).thenReturn(List.of(ended));

        job.expireCoupons();

        assertThat(ended.getStatus()).isEqualTo(CouponStatus.EXPIRED);
    }

    @Test
    void deliveredOrderIsCompletedAndCodPaymentMarkedSuccess() {
        Order order = Order.builder().id("o-1").status(OrderStatus.DELIVERING).build();
        Payment cod = Payment.builder().id("p-cod").order(order).paymentMethod(PaymentMethod.COD).build();
        Payment online = Payment.builder().id("p-online").order(order).paymentMethod(PaymentMethod.ONLINE)
                .status(PaymentStatus.FAILED).build();
        when(orders.deliveredBeforeWithoutRefund(any())).thenReturn(List.of(order));
        when(payments.findByOrderId("o-1")).thenReturn(List.of(cod, online));

        job.autoCompleteDeliveredOrders();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(cod.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(online.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void cartItemsIdleFor30DaysAreDeleted() {
        job.cleanUpAbandonedCartItems();

        var cutoff = org.mockito.ArgumentCaptor.forClass(LocalDateTime.class);
        org.mockito.Mockito.verify(carts).deleteNotModifiedSince(cutoff.capture());
        assertThat(cutoff.getValue()).isBefore(LocalDateTime.now().minusDays(29));
        assertThat(cutoff.getValue()).isAfter(LocalDateTime.now().minusDays(31));
    }
}
