package com.example.flowershop;

import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.OrderStatus;
import com.example.flowershop.repository.*;
import com.example.flowershop.service.CustomerOrderService;
import com.example.flowershop.service.DeliverySync;
import com.example.flowershop.service.GhnService;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/** Khách đổi địa chỉ nhận: chỉ chủ đơn, chỉ địa chỉ của mình, chỉ trước khi chuyển sang giao hàng. */
class CustomerChangeAddressTests {
    private final AddressRepository addresses = mock(AddressRepository.class);
    private final OrderRepository orders = mock(OrderRepository.class);
    private final CustomerOrderService service = new CustomerOrderService(addresses, orders,
            mock(OrderDetailRepository.class), mock(PaymentRepository.class), mock(DeliveryRepository.class),
            mock(GhnService.class), mock(ProductImageRepository.class), mock(DeliverySync.class), mock(RefundRepository.class));

    private final User customer = User.builder().id("c-1").build();

    private Order order(OrderStatus status) {
        Order o = Order.builder().id("o-1").customer(customer).status(status).build();
        when(orders.findById("o-1")).thenReturn(Optional.of(o));
        return o;
    }

    private Address address(User owner) {
        Address a = Address.builder().id("a-2").user(owner).addressLine("12 Lê Lợi").ghnDistrictId(1442).ghnWardCode("20308")
                .recipientName("Người nhận mới").recipientPhone("0912345678").build();
        when(addresses.findById("a-2")).thenReturn(Optional.of(a));
        return a;
    }

    @Test
    void processingOrderTakesTheNewAddressAndRecipient() {
        Order o = order(OrderStatus.PROCESSING);
        Address a = address(customer);

        service.changeDeliveryAddress("c-1", "o-1", "a-2");

        assertThat(o.getDeliveryAddress()).isSameAs(a);
        assertThat(o.getRecipientName()).isEqualTo("Người nhận mới");
        assertThat(o.getRecipientPhone()).isEqualTo("0912345678");
        verify(orders).save(o);
    }

    @Test
    void deliveringOrderCannotChangeAddress() {
        order(OrderStatus.DELIVERING);
        address(customer);
        assertThatThrownBy(() -> service.changeDeliveryAddress("c-1", "o-1", "a-2"))
                .isInstanceOf(ResponseStatusException.class);
        verify(orders, never()).save(any());
    }

    @Test
    void addressOfAnotherUserIsRejected() {
        order(OrderStatus.PENDING);
        address(User.builder().id("c-2").build());
        assertThatThrownBy(() -> service.changeDeliveryAddress("c-1", "o-1", "a-2"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void anotherCustomersOrderIsNotFound() {
        order(OrderStatus.PENDING);
        address(customer);
        assertThatThrownBy(() -> service.changeDeliveryAddress("c-9", "o-1", "a-2"))
                .isInstanceOf(ResponseStatusException.class);
    }
}
