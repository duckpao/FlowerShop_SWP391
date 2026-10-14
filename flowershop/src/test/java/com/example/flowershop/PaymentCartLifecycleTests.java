package com.example.flowershop;

import com.example.flowershop.entity.CartItem;
import com.example.flowershop.entity.Order;
import com.example.flowershop.entity.OrderDetail;
import com.example.flowershop.entity.Payment;
import com.example.flowershop.entity.Product;
import com.example.flowershop.entity.User;
import com.example.flowershop.entity.enums.OrderStatus;
import com.example.flowershop.entity.enums.PaymentMethod;
import com.example.flowershop.entity.enums.PaymentStatus;
import com.example.flowershop.repository.*;
import com.example.flowershop.service.OrderService;
import com.example.flowershop.service.SePayGatewayService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

class PaymentCartLifecycleTests {
    private final OrderRepository orders = mock(OrderRepository.class);
    private final OrderDetailRepository details = mock(OrderDetailRepository.class);
    private final CartItemRepository cartItems = mock(CartItemRepository.class);
    private final PaymentRepository payments = mock(PaymentRepository.class);
    private final OrderService orderService = spy(new OrderService(orders, details, cartItems,
            mock(UserRepository.class), mock(AddressRepository.class), mock(ProductRepository.class),
            payments, mock(CouponRepository.class), mock(com.example.flowershop.repository.ShopRepository.class), mock(com.example.flowershop.service.GhnService.class)));

    private SePayGatewayService newGateway() {
        return PaymentBusinessTests.newGateway(orders, payments, mock(RefundRepository.class), orderService);
    }

    @Test
    void successfulPaymentRemovesOnlyPurchasedQuantity() {
        User customer = User.builder().id("customer-1").build();
        Product flower = Product.builder().id("flower-1").build();
        Order order = Order.builder().id("order-1").customer(customer).build();
        OrderDetail detail = OrderDetail.builder().order(order).product(flower).quantity(2).build();
        CartItem cartItem = CartItem.builder().id("cart-1").user(customer).product(flower).quantity(3).build();
        when(details.findByOrderId("order-1")).thenReturn(List.of(detail));
        when(cartItems.findByUserIdAndProductIdForUpdate("customer-1", "flower-1"))
                .thenReturn(Optional.of(cartItem));

        orderService.clearPaidOrderItems(order);

        assertThat(cartItem.getQuantity()).isEqualTo(1);
        verify(cartItems).save(cartItem);
        verify(cartItems, never()).delete(any());
    }

    @Test
    void cancelledLegacyOrderRestoresMissingCartItemWithoutDuplicatingIt() {
        User customer = User.builder().id("customer-1").build();
        Product flower = Product.builder().id("flower-1").build();
        Order order = Order.builder().id("order-1").customer(customer)
                .status(OrderStatus.CANCELLED).build();
        OrderDetail detail = OrderDetail.builder().order(order).product(flower).quantity(2).build();
        when(details.findByOrderId("order-1")).thenReturn(List.of(detail));
        when(cartItems.findByUserIdAndProductIdForUpdate("customer-1", "flower-1"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(CartItem.builder().user(customer).product(flower).quantity(2).build()));

        orderService.restoreCancelledOrderItems(order);
        orderService.restoreCancelledOrderItems(order);

        ArgumentCaptor<CartItem> saved = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItems, times(1)).save(saved.capture());
        assertThat(saved.getValue().getQuantity()).isEqualTo(2);
        assertThat(saved.getValue().getUser().getId()).isEqualTo("customer-1");
    }

    @Test
    void cancelledOrderKeepsHigherQuantityAlreadyInCart() {
        User customer = User.builder().id("customer-1").build();
        Product flower = Product.builder().id("flower-1").build();
        Order order = Order.builder().id("order-1").customer(customer)
                .status(OrderStatus.CANCELLED).build();
        when(details.findByOrderId("order-1"))
                .thenReturn(List.of(OrderDetail.builder().product(flower).quantity(2).build()));
        when(cartItems.findByUserIdAndProductIdForUpdate("customer-1", "flower-1"))
                .thenReturn(Optional.of(CartItem.builder().user(customer).product(flower).quantity(3).build()));

        orderService.restoreCancelledOrderItems(order);

        verify(cartItems, never()).save(any());
    }

    @Test
    void cancelledCheckoutKeepsCartAndReleasesReservation() {
        Order order = Order.builder().id("order-1").customer(User.builder().id("customer-1").build())
                .status(OrderStatus.PENDING).build();
        Payment payment = Payment.builder().order(order).invoiceNumber("invoice-1")
                .paymentMethod(PaymentMethod.ONLINE).status(PaymentStatus.PENDING).build();
        SePayGatewayService gateway = spy(newGateway());
        when(payments.findByInvoiceNumber("invoice-1")).thenReturn(Optional.of(payment));
        doReturn(false).when(gateway).reconcileWithSePayGateway("invoice-1");
        doReturn(true).when(orderService).cancelPendingOrder(anyString(), anyString(), anyString());

        assertThat(gateway.cancelCheckout("invoice-1", "customer-1")).isFalse();

        verify(orderService).cancelPendingOrder(eq("order-1"), anyString(), eq("customer-1"));
        verifyNoInteractions(cartItems);
    }

    @Test
    void paidCheckoutCannotBeCancelled() {
        Order order = Order.builder().id("order-1").customer(User.builder().id("customer-1").build())
                .status(OrderStatus.PROCESSING).build();
        Payment payment = Payment.builder().order(order).invoiceNumber("invoice-1")
                .paymentMethod(PaymentMethod.ONLINE).amount(new BigDecimal("100000"))
                .status(PaymentStatus.SUCCESS).build();
        SePayGatewayService gateway = newGateway();
        when(payments.findByInvoiceNumber("invoice-1")).thenReturn(Optional.of(payment));

        assertThat(gateway.cancelCheckout("invoice-1", "customer-1")).isTrue();

        verify(orderService, never()).cancelPendingOrder(anyString(), anyString(), anyString());
        verifyNoInteractions(cartItems);
    }

    @Test
    void customerCannotCancelAnotherCustomersCheckout() {
        Order order = Order.builder().id("order-1").customer(User.builder().id("customer-1").build())
                .status(OrderStatus.PENDING).build();
        Payment payment = Payment.builder().order(order).invoiceNumber("invoice-1")
                .paymentMethod(PaymentMethod.ONLINE).status(PaymentStatus.PENDING).build();
        SePayGatewayService gateway = newGateway();
        when(payments.findByInvoiceNumber("invoice-1")).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> gateway.cancelCheckout("invoice-1", "customer-2"))
                .isInstanceOf(com.example.flowershop.exception.ApiException.class);
        verify(orderService, never()).cancelPendingOrder(anyString(), anyString(), anyString());
    }
}
