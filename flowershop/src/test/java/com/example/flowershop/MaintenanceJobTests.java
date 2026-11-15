package com.example.flowershop;

import com.example.flowershop.entity.Coupon;
import com.example.flowershop.entity.Delivery;
import com.example.flowershop.entity.Order;
import com.example.flowershop.entity.Payment;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.CartItemRepository;
import com.example.flowershop.repository.CouponRepository;
import com.example.flowershop.repository.DeliveryRepository;
import com.example.flowershop.repository.PaymentRepository;
import com.example.flowershop.scheduler.MaintenanceJob;
import com.example.flowershop.scheduler.PaymentTimeoutScheduler;
import com.example.flowershop.service.DeliverySync;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Ba job định kỳ SRS 5.3: hết hạn voucher, tự hoàn tất đơn đã giao, dọn giỏ hàng bỏ quên. */
class MaintenanceJobTests {
    private final CouponRepository coupons = mock(CouponRepository.class);
    private final DeliveryRepository deliveries = mock(DeliveryRepository.class);
    private final CartItemRepository carts = mock(CartItemRepository.class);
    private final PaymentRepository payments = mock(PaymentRepository.class);
    private final MaintenanceJob job = new MaintenanceJob(coupons, deliveries, carts, new DeliverySync(payments),
            new TransactionTemplate(mock(PlatformTransactionManager.class)), 3, 30);

    @Test
    void endedActiveCouponsBecomeExpired() {
        Coupon ended = Coupon.builder().id("c-1").code("OLD").build();
        when(coupons.activeEndedBefore(any())).thenReturn(List.of(ended));

        job.expireCoupons();

        assertThat(ended.getStatus()).isEqualTo(CouponStatus.EXPIRED);
    }

    @Test
    void deliveredOrderIsCompletedAndOnlyCodPaymentMarkedSuccess() {
        Order order = Order.builder().id("o-1").status(OrderStatus.DELIVERING).build();
        Delivery delivery = Delivery.builder().id("d-1").order(order).ghnStatus("delivered").build();
        Payment cod = Payment.builder().id("p-cod").order(order).paymentMethod(PaymentMethod.COD).build();
        Payment online = Payment.builder().id("p-online").order(order).paymentMethod(PaymentMethod.ONLINE)
                .status(PaymentStatus.FAILED).build();
        when(deliveries.deliveredBeforeWithoutRefund(any())).thenReturn(List.of(delivery));
        when(payments.findByOrderId("o-1")).thenReturn(List.of(cod, online));

        job.autoCompleteDeliveredOrders();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(cod.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(online.getStatus()).isEqualTo(PaymentStatus.FAILED);
        var cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(deliveries).deliveredBeforeWithoutRefund(cutoff.capture());
        assertThat(cutoff.getValue()).isBefore(LocalDateTime.now().minusDays(3).plusMinutes(1));
        assertThat(cutoff.getValue()).isAfter(LocalDateTime.now().minusDays(3).minusMinutes(1));
    }

    @Test
    void ordersNotDeliveringAreLeftAlone() {
        Order shipped = Order.builder().id("o-2").status(OrderStatus.PROCESSING).build();
        when(deliveries.deliveredBeforeWithoutRefund(any()))
                .thenReturn(List.of(Delivery.builder().id("d-2").order(shipped).ghnStatus("delivered").build()));

        job.autoCompleteDeliveredOrders();

        assertThat(shipped.getStatus()).isEqualTo(OrderStatus.PROCESSING);
    }

    @Test
    void oneFailingOrderDoesNotStopTheOthers() {
        Order bad = Order.builder().id("o-bad").status(OrderStatus.DELIVERING).build();
        Order good = Order.builder().id("o-good").status(OrderStatus.DELIVERING).build();
        when(deliveries.deliveredBeforeWithoutRefund(any())).thenReturn(List.of(
                Delivery.builder().id("d-bad").order(bad).build(), Delivery.builder().id("d-good").order(good).build()));
        when(payments.findByOrderId("o-bad")).thenThrow(new IllegalStateException("boom"));
        when(payments.findByOrderId("o-good")).thenReturn(List.of());

        job.autoCompleteDeliveredOrders();

        assertThat(good.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    void cartItemsIdleFor30DaysAreDeleted() {
        job.cleanUpAbandonedCartItems();

        var cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(carts).deleteNotModifiedSince(cutoff.capture());
        assertThat(cutoff.getValue()).isBefore(LocalDateTime.now().minusDays(29));
        assertThat(cutoff.getValue()).isAfter(LocalDateTime.now().minusDays(31));
    }

    @Test
    void paymentTimeoutSchedulerRunsEvery5SecondsByDefault() throws Exception {
        Scheduled scheduled = PaymentTimeoutScheduler.class.getMethod("scanAndCancelExpiredOrders")
                .getAnnotation(Scheduled.class);
        assertThat(scheduled.fixedDelayString()).endsWith(":5000}");
    }
}
