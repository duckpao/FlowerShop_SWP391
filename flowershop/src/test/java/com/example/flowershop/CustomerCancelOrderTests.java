package com.example.flowershop;

import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.OrderStatus;
import com.example.flowershop.entity.enums.PaymentStatus;
import com.example.flowershop.entity.enums.ProductType;
import com.example.flowershop.exception.ApiException;
import com.example.flowershop.repository.*;
import com.example.flowershop.service.GhnService;
import com.example.flowershop.service.NotificationService;
import com.example.flowershop.service.OrderService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/** Khách tự hủy đơn: chỉ chủ đơn, chỉ trước khi giao; trả kho; đã trả tiền thì tạo yêu cầu hoàn tiền. */
class CustomerCancelOrderTests {
    private final OrderRepository orders = mock(OrderRepository.class);
    private final OrderDetailRepository details = mock(OrderDetailRepository.class);
    private final ProductRepository products = mock(ProductRepository.class);
    private final PaymentRepository payments = mock(PaymentRepository.class);
    private final RefundRepository refunds = mock(RefundRepository.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private final OrderService service = new OrderService(orders, details, mock(CartItemRepository.class),
            mock(UserRepository.class), mock(AddressRepository.class), products, payments,
            mock(CouponRepository.class), mock(ShopRepository.class), mock(GhnService.class), notifications,
            mock(org.springframework.context.ApplicationEventPublisher.class), refunds);

    private final User customer = User.builder().id("c-1").build();
    private final Product rose = Product.builder().id("p-1").type(ProductType.READY_MADE).stock(5).build();

    private Order order(OrderStatus status) {
        Order o = Order.builder().id("o-1").customer(customer).status(status).build();
        when(orders.findByIdForUpdate("o-1")).thenReturn(Optional.of(o));
        when(details.findByOrderId("o-1")).thenReturn(List.of(OrderDetail.builder().product(rose).quantity(2).build()));
        when(products.findByIdForUpdate("p-1")).thenReturn(Optional.of(rose));
        return o;
    }

    @Test
    void unpaidOrderIsCancelledAndStockReturnedWithoutRefund() {
        Order o = order(OrderStatus.PENDING);
        when(payments.findByOrderId("o-1")).thenReturn(List.of());

        service.cancelByCustomer("c-1", "o-1", "Đặt nhầm");

        assertThat(o.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(o.getCancelReason()).isEqualTo("Khách hủy đơn: Đặt nhầm");
        assertThat(rose.getStock()).isEqualTo(7);
        verify(payments).failPendingByOrderId("o-1", "c-1");
        verifyNoInteractions(refunds);
    }

    @Test
    void paidOrderCreatesRefundForPaidAmount() {
        order(OrderStatus.PROCESSING);
        when(payments.findByOrderId("o-1")).thenReturn(List.of(
                Payment.builder().amount(new BigDecimal("250000")).status(PaymentStatus.SUCCESS).build(),
                Payment.builder().amount(new BigDecimal("99000")).status(PaymentStatus.FAILED).build()));

        service.cancelByCustomer("c-1", "o-1", null);

        ArgumentCaptor<Refund> saved = ArgumentCaptor.forClass(Refund.class);
        verify(refunds).save(saved.capture());
        assertThat(saved.getValue().getAmount()).isEqualByComparingTo("250000");
        verify(notifications).notifyRefundRequested(saved.getValue());
    }

    @Test
    void shippedOrderCannotBeCancelled() {
        Order o = order(OrderStatus.DELIVERING);

        assertThatThrownBy(() -> service.cancelByCustomer("c-1", "o-1", null)).isInstanceOf(ApiException.class);
        assertThat(o.getStatus()).isEqualTo(OrderStatus.DELIVERING);
        assertThat(rose.getStock()).isEqualTo(5);
    }

    @Test
    void otherCustomersOrderIsNotFound() {
        order(OrderStatus.PENDING);

        assertThatThrownBy(() -> service.cancelByCustomer("c-2", "o-1", null)).isInstanceOf(ApiException.class);
        verify(orders, never()).save(any());
    }
}
