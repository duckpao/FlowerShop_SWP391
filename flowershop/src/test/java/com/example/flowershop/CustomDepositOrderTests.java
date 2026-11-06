package com.example.flowershop;

import com.example.flowershop.dto.order.MakeCustomOrderRequest;
import com.example.flowershop.repository.*;
import com.example.flowershop.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/** The former deposit-creation path must not create a second, separate custom-order workflow. */
class CustomDepositOrderTests {
    @Test void separateDepositCreationIsGoneAndNeverWritesOrders() {
        var orders=mock(OrderRepository.class);
        var details=mock(OrderDetailRepository.class);
        var service=new OrderService(orders,details,mock(CartItemRepository.class),mock(UserRepository.class),
                mock(AddressRepository.class),mock(ProductRepository.class),mock(PaymentRepository.class),
                mock(CouponService.class),mock(ShopRepository.class),mock(GhnService.class),mock(NotificationService.class),
                mock(org.springframework.context.ApplicationEventPublisher.class),mock(RefundRepository.class));
        var body=new MakeCustomOrderRequest(); body.setDeliveryAddressId("address-1");
        assertThatThrownBy(() -> service.makeCustomOrder("customer-1","request-1",body))
                .isInstanceOfSatisfying(ResponseStatusException.class,e -> org.assertj.core.api.Assertions.assertThat(e.getStatusCode().value()).isEqualTo(410));
        verifyNoInteractions(orders,details);
    }
}
