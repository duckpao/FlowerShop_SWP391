package com.example.flowershop;

import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.exception.ApiException;
import com.example.flowershop.repository.OrderRepository;
import com.example.flowershop.repository.PaymentRepository;
import com.example.flowershop.repository.RefundRepository;
import com.example.flowershop.service.CloudinaryService;
import com.example.flowershop.service.CustomerRefundService;
import com.example.flowershop.service.CustomerRefundService.RefundInput;
import com.example.flowershop.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** SRS 5.2: khách gửi / hủy yêu cầu hoàn tiền. */
class CustomerRefundServiceTests {
    private final OrderRepository orders = mock(OrderRepository.class);
    private final PaymentRepository payments = mock(PaymentRepository.class);
    private final RefundRepository refunds = mock(RefundRepository.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private final CustomerRefundService service = new CustomerRefundService(orders, payments, refunds,
            mock(CloudinaryService.class), notifications);
    private final List<Refund> existing = new ArrayList<>();
    private Order order;

    @BeforeEach
    void setUp() {
        order = Order.builder().id("o-1").customer(User.builder().id("c-1").build()).status(OrderStatus.COMPLETED)
                .totalAmount(new BigDecimal("300000")).build();
        when(orders.findById("o-1")).thenReturn(Optional.of(order));
        when(payments.findByOrderId("o-1")).thenReturn(List.of(
                Payment.builder().id("p-1").amount(new BigDecimal("300000")).status(PaymentStatus.SUCCESS).build(),
                Payment.builder().id("p-2").amount(new BigDecimal("999999")).status(PaymentStatus.FAILED).build()));
        when(refunds.findByOrderIdOrderByCreatedDateAsc("o-1")).thenReturn(existing);
        when(refunds.save(any(Refund.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void amountIsSuccessfulPaymentsMinusProcessedRefunds() {
        existing.add(Refund.builder().id("old").order(order).amount(new BigDecimal("100000")).reason("x").status(RefundStatus.PROCESSED).build());
        var result = service.request("c-1", "o-1", new RefundInput("Giao hàng trễ", "", List.of()));
        assertThat(result.amount()).isEqualByComparingTo("200000");
        assertThat(result.status()).isEqualTo("REQUESTED");
        verify(notifications).notifyRefundRequested(any());
    }

    @Test
    void rejectsWhenAnotherRequestIsPendingOrEvidenceMissingOrOrderNotRefundable() {
        assertThatThrownBy(() -> service.request("c-1", "o-1", new RefundInput("Hoa bị dập, héo", "", List.of())))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.request("c-1", "o-1", new RefundInput("Khác", " ", List.of())))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.request("other", "o-1", new RefundInput("Đổi ý", "", List.of())))
                .isInstanceOf(ApiException.class);
        existing.add(Refund.builder().id("open").order(order).amount(BigDecimal.TEN).reason("x").status(RefundStatus.REQUESTED).build());
        assertThatThrownBy(() -> service.request("c-1", "o-1", new RefundInput("Đổi ý", "", List.of())))
                .isInstanceOf(ApiException.class);
        existing.clear();
        order.setStatus(OrderStatus.PROCESSING);
        assertThatThrownBy(() -> service.request("c-1", "o-1", new RefundInput("Đổi ý", "", List.of())))
                .isInstanceOf(ApiException.class);
        verify(refunds, never()).save(any());
    }

    @Test
    void customerCancelsOnlyPendingRequest() {
        Refund pending = Refund.builder().id("r-1").order(order).amount(BigDecimal.TEN).reason("x").status(RefundStatus.REQUESTED).build();
        when(refunds.findById("r-1")).thenReturn(Optional.of(pending));
        assertThat(service.cancel("c-1", "o-1", "r-1").status()).isEqualTo("CANCELLED");
        assertThatThrownBy(() -> service.cancel("c-1", "o-1", "r-1")).isInstanceOf(ApiException.class);
    }
}
