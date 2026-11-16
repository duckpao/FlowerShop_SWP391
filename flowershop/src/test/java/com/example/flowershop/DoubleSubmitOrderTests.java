package com.example.flowershop;

import com.example.flowershop.dto.order.MakeOrdersRequest;
import com.example.flowershop.entity.User;
import com.example.flowershop.exception.ApiException;
import com.example.flowershop.repository.*;
import com.example.flowershop.service.GhnService;
import com.example.flowershop.service.NotificationService;
import com.example.flowershop.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/** Bấm đặt hàng hai lần cùng lúc: khóa khách để chạy lần lượt, lần sau nhận 409 thay vì lỗi Hibernate 500. */
class DoubleSubmitOrderTests {
    private final UserRepository users = mock(UserRepository.class);
    private final CartItemRepository cart = mock(CartItemRepository.class);
    private final AddressRepository addresses = mock(AddressRepository.class);
    private final OrderService service = new OrderService(mock(OrderRepository.class), mock(OrderDetailRepository.class), cart,
            users, addresses, mock(ProductRepository.class), mock(PaymentRepository.class),
            mock(CouponRepository.class), mock(ShopRepository.class), mock(GhnService.class), mock(NotificationService.class),
            mock(org.springframework.context.ApplicationEventPublisher.class), mock(RefundRepository.class));

    @Test
    void secondSubmitFindsCartAlreadyOrderedAndGetsConflict() {
        User customer = User.builder().id("c-1").fullName("Lan").phone("0901234567").build();
        when(users.findById("c-1")).thenReturn(Optional.of(customer));
        when(addresses.findById("a-1")).thenReturn(Optional.of(com.example.flowershop.entity.Address.builder()
                .id("a-1").user(customer).ghnDistrictId(1).ghnWardCode("w").build()));
        when(cart.findById("ci-1")).thenReturn(Optional.empty());   // lần đặt đầu đã xóa dòng giỏ

        var shop = new MakeOrdersRequest.ShopOrder();
        shop.setShopId("s-1");
        shop.setCartItemIds(List.of("ci-1"));
        var request = new MakeOrdersRequest();
        request.setDeliveryAddressId("a-1");
        request.setPaymentMethod("COD");
        request.setShops(List.of(shop));

        assertThatThrownBy(() -> service.makeOrders("c-1", request))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(users).findByIdForUpdate("c-1");
    }
}
