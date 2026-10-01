package com.example.flowershop;

import com.example.flowershop.dto.payment.SePayIpnPayload;
import com.example.flowershop.dto.payment.CreatePaymentRequest;
import com.example.flowershop.entity.Order;
import com.example.flowershop.entity.Payment;
import com.example.flowershop.entity.User;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.OrderRepository;
import com.example.flowershop.repository.PaymentRepository;
import com.example.flowershop.service.OrderService;
import com.example.flowershop.service.PaymentService;
import com.example.flowershop.service.SePayGatewayService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class PaymentBusinessTests {
    @Test
    void repeatedCheckoutReusesPendingInvoice() {
        OrderRepository orders = mock(OrderRepository.class);
        PaymentRepository payments = mock(PaymentRepository.class);
        OrderService orderService = mock(OrderService.class);
        SePayGatewayService service = new SePayGatewayService(orders, payments, orderService);
        ReflectionTestUtils.setField(service, "merchantId", "merchant-test");
        ReflectionTestUtils.setField(service, "secretKey", "secret-test");
        Order order = Order.builder().id("12345678-0000-0000-0000-000000000000")
                .customer(User.builder().id("customer-1").build())
                .status(OrderStatus.PENDING).totalAmount(new BigDecimal("100000"))
                .createdDate(LocalDateTime.now())
                .build();
        Payment pending = Payment.builder().id("payment-1").order(order).paymentMethod(PaymentMethod.ONLINE)
                .paymentType(PaymentType.FULL).status(PaymentStatus.PENDING)
                .amount(new BigDecimal("100000")).invoiceNumber("invoice-1").build();
        when(orders.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
        when(payments.findByOrderId(order.getId())).thenReturn(List.of(pending));

        var result = service.createPayment(CreatePaymentRequest.builder().orderId(order.getId()).build(), "customer-1");

        assertThat(result.getInvoiceNumber()).isEqualTo("invoice-1");
        assertThat(result.getPaymentId()).isEqualTo("payment-1");
        verify(payments, never()).save(any());
    }

    @Test
    void codOrderInProcessingIsNotReportedAsPaid() {
        OrderRepository orders = mock(OrderRepository.class);
        PaymentRepository payments = mock(PaymentRepository.class);
        SePayGatewayService gateway = mock(SePayGatewayService.class);
        OrderService orderService = mock(OrderService.class);
        PaymentService service = new PaymentService(orders, payments, gateway, orderService);
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
    void ipnWithWrongAmountCannotConfirmOrder() {
        OrderRepository orders = mock(OrderRepository.class);
        PaymentRepository payments = mock(PaymentRepository.class);
        OrderService orderService = mock(OrderService.class);
        SePayGatewayService service = new SePayGatewayService(orders, payments, orderService);
        ReflectionTestUtils.setField(service, "ipnSecret", "test-secret");
        Order order = Order.builder().id("order-1").status(OrderStatus.PENDING)
                .createdDate(LocalDateTime.now())
                .build();
        Payment payment = Payment.builder().order(order).amount(new BigDecimal("100000"))
                .status(PaymentStatus.PENDING).paymentType(PaymentType.FULL).build();
        when(payments.findByInvoiceNumberForUpdate("invoice-1")).thenReturn(Optional.of(payment));
        SePayIpnPayload.IpnOrder ipnOrder = new SePayIpnPayload.IpnOrder();
        ipnOrder.setOrderInvoiceNumber("invoice-1");
        SePayIpnPayload.IpnTransaction transaction = new SePayIpnPayload.IpnTransaction();
        transaction.setTransactionStatus("APPROVED");
        transaction.setTransactionAmount(new BigDecimal("1000"));
        SePayIpnPayload ipn = new SePayIpnPayload();
        ipn.setNotificationType("ORDER_PAID");
        ipn.setOrder(ipnOrder);
        ipn.setTransaction(transaction);

        var result = service.handleIpn(ipn, "test-secret");

        assertThat(result.get("success")).isEqualTo(false);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(orders, never()).save(any());
    }

    @Test
    void orderPendingAfter10MinutesIsCancelledWhenCheckingStatus() {
        OrderRepository orders = mock(OrderRepository.class);
        PaymentRepository payments = mock(PaymentRepository.class);
        SePayGatewayService gateway = mock(SePayGatewayService.class);
        OrderService orderService = mock(OrderService.class);
        PaymentService service = new PaymentService(orders, payments, gateway, orderService);

        Order order = Order.builder().id("order-expired")
                .customer(User.builder().id("customer-1").build())
                .status(OrderStatus.PENDING)
                .createdDate(LocalDateTime.now().minusMinutes(11))
                .build();
        when(orders.findById("order-expired")).thenReturn(Optional.of(order));

        var result = service.checkPaymentStatus("order-expired", "customer-1");

        verify(orderService).cancelExpiredOrder(eq("order-expired"), anyString());
        assertThat(result.isPaid()).isFalse();
    }
}

