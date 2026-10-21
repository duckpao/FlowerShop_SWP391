package com.example.flowershop;

import com.example.flowershop.dto.payment.CreatePaymentRequest;
import com.example.flowershop.entity.Order;
import com.example.flowershop.entity.Payment;
import com.example.flowershop.entity.User;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.exception.ApiException;
import com.example.flowershop.repository.OrderRepository;
import com.example.flowershop.repository.PaymentRepository;
import com.example.flowershop.repository.RefundRepository;
import com.example.flowershop.service.OrderService;
import com.example.flowershop.service.PaymentService;
import com.example.flowershop.service.SePayGatewayService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class PaymentBusinessTests {
    private final OrderRepository orders = mock(OrderRepository.class);
    private final PaymentRepository payments = mock(PaymentRepository.class);
    private final RefundRepository refunds = mock(RefundRepository.class);
    private final OrderService orderService = mock(OrderService.class);

    static SePayGatewayService newGateway(OrderRepository orders, PaymentRepository payments,
                                          RefundRepository refunds, OrderService orderService) {
        TransactionTemplate tx = mock(TransactionTemplate.class);
        when(tx.execute(any())).thenAnswer(inv -> inv.<TransactionCallback<?>>getArgument(0).doInTransaction(null));
        SePayGatewayService service = new SePayGatewayService(orders, payments, refunds, orderService, tx);
        ReflectionTestUtils.setField(service, "entityManager", mock(EntityManager.class));
        return service;
    }

    private SePayGatewayService gatewayWithCheckoutConfig() {
        SePayGatewayService service = newGateway(orders, payments, refunds, orderService);
        ReflectionTestUtils.setField(service, "merchantId", "merchant-test");
        ReflectionTestUtils.setField(service, "secretKey", "secret-test");
        return service;
    }

    private Order pendingOrder() {
        return Order.builder().id("12345678-0000-0000-0000-000000000000")
                .customer(User.builder().id("customer-1").build())
                .status(OrderStatus.PENDING).totalAmount(new BigDecimal("100000"))
                .createdDate(LocalDateTime.now())
                .build();
    }

    @Test
    void retryCreatesNewInvoiceAndRetiresOldOne() {
        SePayGatewayService service = gatewayWithCheckoutConfig();
        Order order = pendingOrder();
        Payment pending = Payment.builder().id("payment-1").order(order).paymentMethod(PaymentMethod.ONLINE)
                .paymentType(PaymentType.FULL).status(PaymentStatus.PENDING)
                .amount(new BigDecimal("100000")).invoiceNumber("invoice-1").build();
        when(orders.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
        when(payments.findByOrderId(order.getId())).thenReturn(List.of(pending));

        var result = service.createPayment(CreatePaymentRequest.builder().orderId(order.getId()).build(), "customer-1");

        assertThat(result.getInvoiceNumber()).isNotEqualTo("invoice-1");
        assertThat(pending.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(payments, times(2)).save(any());
    }

    private Order pendingOrder(String id, String total) {
        return Order.builder().id(id).customer(User.builder().id("customer-1").build())
                .orderType(OrderType.STANDARD).status(OrderStatus.PENDING).totalAmount(new BigDecimal(total))
                .createdDate(LocalDateTime.now()).build();
    }

    @Test
    void groupPaymentSendsOneInvoiceWithSummedAmountAndOnePaymentPerOrder() {
        SePayGatewayService service = gatewayWithCheckoutConfig();
        Order a = pendingOrder("aaaaaaaa-0000-0000-0000-000000000000", "100000");
        Order b = pendingOrder("bbbbbbbb-0000-0000-0000-000000000000", "250000");
        when(orders.findByIdForUpdate(a.getId())).thenReturn(Optional.of(a));
        when(orders.findByIdForUpdate(b.getId())).thenReturn(Optional.of(b));
        when(payments.findByOrderId(any())).thenReturn(List.of());
        var request = new com.example.flowershop.dto.payment.CreateGroupPaymentRequest();
        request.setOrderIds(List.of(b.getId(), a.getId()));

        var result = service.createGroupPayment(request, "customer-1");

        assertThat(result.getInvoiceNumber()).startsWith("GRP-");
        assertThat(result.getAmount()).isEqualByComparingTo("350000");
        assertThat(result.getFields().get("order_amount")).isEqualTo("350000");
        assertThat(result.getFields().get("order_invoice_number")).isEqualTo(result.getInvoiceNumber());
        var saved = org.mockito.ArgumentCaptor.forClass(Payment.class);
        verify(payments, times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).allSatisfy(p -> assertThat(p.getGroupInvoice()).isEqualTo(result.getInvoiceNumber()));
        assertThat(saved.getAllValues()).extracting(Payment::getAmount)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactlyInAnyOrder(new BigDecimal("100000"), new BigDecimal("250000"));
    }

    @Test
    void groupPaymentRejectsAnotherCustomersOrder() {
        SePayGatewayService service = gatewayWithCheckoutConfig();
        Order a = pendingOrder("aaaaaaaa-0000-0000-0000-000000000000", "100000");
        when(orders.findByIdForUpdate(a.getId())).thenReturn(Optional.of(a));
        var request = new com.example.flowershop.dto.payment.CreateGroupPaymentRequest();
        request.setOrderIds(List.of(a.getId()));

        assertThatThrownBy(() -> service.createGroupPayment(request, "someone-else")).isInstanceOf(ApiException.class);
        verify(payments, never()).save(any());
    }

    @Test
    void checkoutFieldsArePostedInSigningOrder() {
        SePayGatewayService service = gatewayWithCheckoutConfig();
        Order order = pendingOrder();
        when(orders.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
        when(payments.findByOrderId(order.getId())).thenReturn(List.of());

        var result = service.createPayment(CreatePaymentRequest.builder().orderId(order.getId())
                .paymentMethod("bank_transfer").build(), "customer-1");

        assertThat(result.getFields().keySet()).containsExactly("merchant", "operation", "payment_method",
                "order_amount", "currency", "order_invoice_number", "order_description", "customer_id",
                "success_url", "error_url", "cancel_url", "signature");
        assertThat(result.getFields().get("payment_method")).isEqualTo("BANK_TRANSFER");
    }

    @Test
    void unknownPaymentMethodIsRejected() {
        SePayGatewayService service = gatewayWithCheckoutConfig();

        assertThatThrownBy(() -> service.createPayment(CreatePaymentRequest.builder().orderId("order-1")
                .paymentMethod("CREDIT_CARD").build(), "customer-1"))
                .isInstanceOf(ApiException.class);
        verifyNoInteractions(orders);
    }

    @Test
    void codOrderInProcessingIsNotReportedAsPaid() {
        SePayGatewayService gateway = mock(SePayGatewayService.class);
        PaymentService service = new PaymentService(orders, payments, gateway, mock(com.example.flowershop.service.NotificationService.class));
        Order order = Order.builder().id("order-1").customer(User.builder().id("customer-1").build())
                .status(OrderStatus.PROCESSING).build();
        Payment cod = Payment.builder().order(order).paymentMethod(PaymentMethod.COD)
                .status(PaymentStatus.PENDING).amount(new BigDecimal("100000")).build();
        when(orders.findById("order-1")).thenReturn(Optional.of(order));
        when(payments.findTopByOrderIdOrderByCreatedDateDesc("order-1")).thenReturn(Optional.of(cod));

        var result = service.checkPaymentStatus("order-1", "customer-1");

        assertThat(result.isPaid()).isFalse();
        assertThat(result.getPaidAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        verifyNoInteractions(gateway);
    }

    @Test
    void expiredOrderPaidAtSePayIsNotCancelled() {
        SePayGatewayService service = spy(newGateway(orders, payments, refunds, orderService));
        Payment payment = Payment.builder().paymentMethod(PaymentMethod.ONLINE).invoiceNumber("invoice-1")
                .status(PaymentStatus.PENDING).build();
        when(payments.findByOrderId("order-1")).thenReturn(List.of(payment));
        doReturn(true).when(service).reconcileWithSePayGateway("invoice-1");

        service.expireUnpaidOrder("order-1");

        verify(orderService, never()).cancelExpiredOrder(anyString(), anyString());
    }

    @Test
    void expiredUnpaidOrderIsCancelled() {
        SePayGatewayService service = spy(newGateway(orders, payments, refunds, orderService));
        Payment payment = Payment.builder().paymentMethod(PaymentMethod.ONLINE).invoiceNumber("invoice-1")
                .status(PaymentStatus.PENDING).build();
        when(payments.findByOrderId("order-1")).thenReturn(List.of(payment));
        doReturn(false).when(service).reconcileWithSePayGateway("invoice-1");

        service.expireUnpaidOrder("order-1");

        verify(orderService).cancelExpiredOrder(eq("order-1"), anyString());
    }

    @Test
    void statusCheckDoesNotCancelExpiredOrder() {
        SePayGatewayService gateway = mock(SePayGatewayService.class);
        PaymentService service = new PaymentService(orders, payments, gateway, mock(com.example.flowershop.service.NotificationService.class));
        Order order = Order.builder().id("order-expired")
                .customer(User.builder().id("customer-1").build())
                .status(OrderStatus.PENDING)
                .createdDate(LocalDateTime.now().minusMinutes(11))
                .build();
        when(orders.findById("order-expired")).thenReturn(Optional.of(order));

        var result = service.checkPaymentStatus("order-expired", "customer-1");

        verify(orderService, never()).cancelExpiredOrder(anyString(), anyString());
        assertThat(result.isPaid()).isFalse();
    }

    // ===== Đặt cọc (DEPOSIT / FINAL) =====

    private Order customOrder(OrderStatus status, String total, String deposit, LocalDateTime createdDate) {
        return Order.builder().id("12345678-0000-0000-0000-000000000000")
                .customer(User.builder().id("customer-1").build())
                .orderType(OrderType.CUSTOM).status(status)
                .totalAmount(new BigDecimal(total)).depositAmount(new BigDecimal(deposit))
                .createdDate(createdDate).build();
    }

    private com.example.flowershop.dto.payment.CreatePaymentResponse pay(SePayGatewayService service, Order order,
                                                                       PaymentType type) {
        when(orders.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
        when(payments.findByOrderId(order.getId())).thenReturn(List.of());
        return service.createPayment(CreatePaymentRequest.builder().orderId(order.getId()).paymentType(type).build(),
                "customer-1");
    }

    @Test
    void depositDefaultsToHalfOfTotalWhenNotSet() {
        var result = pay(gatewayWithCheckoutConfig(),
                customOrder(OrderStatus.AWAITING_DEPOSIT, "400000", "0", LocalDateTime.now()), PaymentType.DEPOSIT);

        assertThat(result.getAmount()).isEqualByComparingTo("200000");
    }

    @Test
    void depositUsesRequiredDepositAmountWhenSet() {
        var result = pay(gatewayWithCheckoutConfig(),
                customOrder(OrderStatus.AWAITING_DEPOSIT, "400000", "100000", LocalDateTime.now()), PaymentType.DEPOSIT);

        assertThat(result.getAmount()).isEqualByComparingTo("100000");
    }

    @Test
    void depositOnStandardOrderIsRejected() {
        assertThatThrownBy(() -> pay(gatewayWithCheckoutConfig(), pendingOrder(), PaymentType.DEPOSIT))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void finalBeforeDepositIsRejected() {
        assertThatThrownBy(() -> pay(gatewayWithCheckoutConfig(),
                customOrder(OrderStatus.AWAITING_DEPOSIT, "400000", "0", LocalDateTime.now()), PaymentType.FINAL))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void awaitingDepositOlderThan24hCannotBePaid() {
        Order old = customOrder(OrderStatus.AWAITING_DEPOSIT, "400000", "0", LocalDateTime.now().minusHours(25));
        Order fresh = customOrder(OrderStatus.AWAITING_DEPOSIT, "400000", "0", LocalDateTime.now().minusHours(23));

        assertThatThrownBy(() -> pay(gatewayWithCheckoutConfig(), old, PaymentType.DEPOSIT))
                .isInstanceOf(ApiException.class);
        assertThat(pay(gatewayWithCheckoutConfig(), fresh, PaymentType.DEPOSIT).getAmount()).isEqualByComparingTo("200000");
    }

    @Test
    void schedulerExpiresAwaitingDepositUsing24hThreshold() {
        SePayGatewayService gateway = mock(SePayGatewayService.class);
        var scheduler = new com.example.flowershop.scheduler.PaymentTimeoutScheduler(orders, gateway);
        Order old = customOrder(OrderStatus.AWAITING_DEPOSIT, "400000", "0", LocalDateTime.now().minusHours(25));
        old.setId("old-order");
        when(orders.findByStatusAndCreatedDateBefore(eq(OrderStatus.PENDING), any())).thenReturn(List.of());
        when(orders.findByStatusAndCreatedDateBefore(eq(OrderStatus.AWAITING_DEPOSIT), any())).thenAnswer(inv -> {
            LocalDateTime threshold = inv.getArgument(1);
            // ngưỡng phải khoảng 24 giờ trước: đơn 25h tuổi bị bắt, đơn 23h tuổi thì không
            assertThat(threshold).isBefore(LocalDateTime.now().minusHours(23));
            assertThat(threshold).isAfter(LocalDateTime.now().minusHours(25));
            return List.of(old);
        });

        scheduler.scanAndCancelExpiredOrders();

        verify(gateway).expireUnpaidOrder("old-order");
    }

    @Test
    void expiringAwaitingDepositOrderUsesDepositReason() {
        SePayGatewayService service = newGateway(orders, payments, refunds, orderService);
        when(payments.findByOrderId("order-1")).thenReturn(List.of());
        when(orders.findById("order-1")).thenReturn(Optional.of(
                customOrder(OrderStatus.AWAITING_DEPOSIT, "400000", "0", LocalDateTime.now().minusHours(25))));

        service.expireUnpaidOrder("order-1");

        verify(orderService).cancelExpiredOrder(eq("order-1"), contains("24 giờ"));
    }

    @Test
    void cancelPendingOrderAlsoCancelsAwaitingDepositOrderWithoutDetails() {
        var orderRepo = mock(OrderRepository.class);
        var service = new OrderService(orderRepo, mock(com.example.flowershop.repository.OrderDetailRepository.class),
                mock(com.example.flowershop.repository.CartItemRepository.class),
                mock(com.example.flowershop.repository.UserRepository.class),
                mock(com.example.flowershop.repository.AddressRepository.class),
                mock(com.example.flowershop.repository.ProductRepository.class), payments,
                mock(com.example.flowershop.repository.CouponRepository.class), mock(com.example.flowershop.repository.ShopRepository.class),
                mock(com.example.flowershop.service.GhnService.class),
                mock(com.example.flowershop.service.NotificationService.class));
        ReflectionTestUtils.setField(service, "entityManager", mock(EntityManager.class));
        Order order = customOrder(OrderStatus.AWAITING_DEPOSIT, "400000", "0", LocalDateTime.now().minusHours(25));
        when(orderRepo.findById(order.getId())).thenReturn(Optional.of(order));

        assertThat(service.cancelPendingOrder(order.getId(), "test", "SYSTEM")).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        Order paid = customOrder(OrderStatus.DEPOSIT_PAID, "400000", "0", LocalDateTime.now().minusHours(25));
        paid.setId("paid-order");
        when(orderRepo.findById("paid-order")).thenReturn(Optional.of(paid));
        assertThat(service.cancelPendingOrder("paid-order", "test", "SYSTEM")).isFalse();
        assertThat(paid.getStatus()).isEqualTo(OrderStatus.DEPOSIT_PAID);
    }

    @Test
    void fullPaymentDoesNotOverwriteDepositAmount() {
        SePayGatewayService service = newGateway(orders, payments, refunds, orderService);
        Order order = pendingOrder();
        order.setDepositAmount(BigDecimal.ZERO);
        Payment payment = Payment.builder().id("p1").order(order).paymentMethod(PaymentMethod.ONLINE)
                .paymentType(PaymentType.FULL).status(PaymentStatus.PENDING)
                .amount(new BigDecimal("100000")).invoiceNumber("inv-full").build();
        when(payments.findByInvoiceNumber("inv-full")).thenReturn(Optional.of(payment));

        Object outcome = ReflectionTestUtils.invokeMethod(service, "applyPaid", "inv-full",
                new BigDecimal("100000"), "tx-1", "{}", "TEST");

        assertThat(outcome).hasToString("PAID");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PROCESSING);
        assertThat(order.getDepositAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void statusTellsFrontendWhatToPayNext() {
        SePayGatewayService gateway = mock(SePayGatewayService.class);
        PaymentService service = new PaymentService(orders, payments, gateway, mock(com.example.flowershop.service.NotificationService.class));
        Order awaiting = customOrder(OrderStatus.AWAITING_DEPOSIT, "400000", "0", LocalDateTime.now());
        when(orders.findById(awaiting.getId())).thenReturn(Optional.of(awaiting));
        when(payments.findByOrderId(awaiting.getId())).thenReturn(List.of());

        var first = service.checkPaymentStatus(awaiting.getId(), "customer-1");

        assertThat(first.getNextPaymentType()).isEqualTo("DEPOSIT");
        assertThat(first.getDepositAmount()).isEqualByComparingTo("200000");
        assertThat(first.getTotalPaid()).isEqualByComparingTo("0");
        assertThat(first.getRemainingAmount()).isEqualByComparingTo("400000");

        Order depositPaid = customOrder(OrderStatus.DEPOSIT_PAID, "400000", "0", LocalDateTime.now());
        depositPaid.setId("order-2");
        Payment deposit = Payment.builder().order(depositPaid).paymentMethod(PaymentMethod.ONLINE)
                .paymentType(PaymentType.DEPOSIT).status(PaymentStatus.SUCCESS).amount(new BigDecimal("200000")).build();
        when(orders.findById("order-2")).thenReturn(Optional.of(depositPaid));
        when(payments.findByOrderId("order-2")).thenReturn(List.of(deposit));
        when(payments.findTopByOrderIdOrderByCreatedDateDesc("order-2")).thenReturn(Optional.of(deposit));

        var second = service.checkPaymentStatus("order-2", "customer-1");

        assertThat(second.getNextPaymentType()).isEqualTo("FINAL");
        assertThat(second.getTotalPaid()).isEqualByComparingTo("200000");
        assertThat(second.getRemainingAmount()).isEqualByComparingTo("200000");
    }
}
