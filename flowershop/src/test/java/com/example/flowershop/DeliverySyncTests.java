package com.example.flowershop;

import com.example.flowershop.entity.Delivery;
import com.example.flowershop.entity.Order;
import com.example.flowershop.entity.Payment;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.PaymentRepository;
import com.example.flowershop.service.DeliverySync;
import com.example.flowershop.service.GhnService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DeliverySyncTests {

    @Test
    void deliveredDepositOrderSettlesOnlyTheCodRemainder() {
        PaymentRepository payments = mock(PaymentRepository.class);
        Order order = Order.builder().id("o-1").status(OrderStatus.DELIVERING).build();
        Payment deposit = Payment.builder().paymentMethod(PaymentMethod.ONLINE).paymentType(PaymentType.DEPOSIT).status(PaymentStatus.SUCCESS).build();
        Payment failedRetry = Payment.builder().paymentMethod(PaymentMethod.ONLINE).paymentType(PaymentType.DEPOSIT).status(PaymentStatus.FAILED).build();
        Payment cod = Payment.builder().paymentMethod(PaymentMethod.COD).paymentType(PaymentType.FINAL).status(PaymentStatus.PENDING).build();
        when(payments.findByOrderId("o-1")).thenReturn(List.of(deposit, failedRetry, cod));

        new DeliverySync(payments).apply(order, Delivery.builder().build(),
                new GhnService.Tracking("delivered", DeliveryStatus.DELIVERED), "ghn");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(cod.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(failedRetry.getStatus()).isEqualTo(PaymentStatus.FAILED);   // không được cộng vào số đã trả
    }
}
