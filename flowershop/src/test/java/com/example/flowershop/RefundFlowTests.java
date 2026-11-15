package com.example.flowershop;

import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.*;
import com.example.flowershop.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RefundFlowTests {
    private final OrderRepository orders = mock(OrderRepository.class);
    private final PaymentRepository payments = mock(PaymentRepository.class);
    private final ShopRepository shops = mock(ShopRepository.class);
    private final RefundRepository refunds = mock(RefundRepository.class);
    private final NotificationService notifications = mock(NotificationService.class);

    private final User owner = User.builder().id("owner-1").build();
    private final Shop shop = Shop.builder().id("shop-1").owner(owner).build();
    private final Order order = Order.builder().id("order-1").shop(shop).status(OrderStatus.PROCESSING)
            .customer(User.builder().id("c-1").fullName("Khách").phone("0901234567").build())
            .totalAmount(new BigDecimal("300000")).build();

    private ManagerOrderService managerOrders() {
        when(shops.findById("shop-1")).thenReturn(Optional.of(shop));
        when(orders.findByIdAndShopId("order-1", "shop-1")).thenReturn(Optional.of(order));
        return new ManagerOrderService(orders, mock(OrderDetailRepository.class), mock(DeliveryRepository.class),
                payments, shops, mock(AddressRepository.class), mock(GhnService.class), mock(DeliverySync.class),
                refunds, notifications);
    }

    private Payment payment(PaymentStatus status, String amount) {
        return Payment.builder().id("p-" + status).order(order).status(status).amount(new BigDecimal(amount)).build();
    }

    @Test
    void cancellingPaidOrderCreatesRefundForPaidAmountAndNotifiesStaff() {
        when(payments.findByOrderId("order-1")).thenReturn(List.of(
                payment(PaymentStatus.SUCCESS, "90000"), payment(PaymentStatus.PENDING, "210000")));

        managerOrders().cancel("shop-1", "owner-1", "order-1", "Hết hoa");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(refunds).save(argThat(r -> r.getAmount().compareTo(new BigDecimal("90000")) == 0
                && r.getStatus() == RefundStatus.REQUESTED && r.getReason().contains("Hết hoa")));
        verify(notifications).notifyRefundRequested(any());
    }

    @Test
    void cancellingUnpaidOrderNeedsNoRefund() {
        when(payments.findByOrderId("order-1")).thenReturn(List.of(payment(PaymentStatus.PENDING, "300000")));

        managerOrders().cancel("shop-1", "owner-1", "order-1", "Hết hoa");

        verifyNoInteractions(refunds, notifications);
    }

    private StaffRefundService staffService(Refund refund) {
        when(refunds.findById(refund.getId())).thenReturn(Optional.of(refund));
        return new StaffRefundService(refunds, mock(ShopOperationGuard.class), notifications);
    }

    private Refund refund(RefundStatus status) {
        return Refund.builder().id("r-1").order(order).amount(new BigDecimal("90000")).reason("x").status(status).build();
    }

    @Test
    void staffMovesRefundForwardStepByStep() {
        Refund r = refund(RefundStatus.REQUESTED);
        StaffRefundService service = staffService(r);

        assertThat(service.updateStatus("shop-1", "staff-1", "r-1", "approved").status()).isEqualTo("APPROVED");
        verify(notifications, never()).notifyRefundResolved(any());   // mới liên hệ, chưa báo khách
        assertThat(service.updateStatus("shop-1", "staff-1", "r-1", "PROCESSED").customerPhone()).isEqualTo("0901234567");
        assertThat(r.getLastModifyBy()).isEqualTo("staff-1");
        verify(notifications).notifyRefundResolved(r);                // đã hoàn → báo khách
    }

    @Test
    void staffCannotSkipContactStepOrReopenFinishedRefund() {
        StaffRefundService fresh = staffService(refund(RefundStatus.REQUESTED));
        assertThatThrownBy(() -> fresh.updateStatus("shop-1", "staff-1", "r-1", "PROCESSED"))
                .isInstanceOf(ResponseStatusException.class);

        StaffRefundService done = staffService(refund(RefundStatus.PROCESSED));
        assertThatThrownBy(() -> done.updateStatus("shop-1", "staff-1", "r-1", "REJECTED"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void staffCannotTouchRefundOfAnotherShop() {
        StaffRefundService service = staffService(refund(RefundStatus.REQUESTED));
        assertThatThrownBy(() -> service.updateStatus("shop-2", "staff-1", "r-1", "APPROVED"))
                .isInstanceOf(ResponseStatusException.class);
    }
}
