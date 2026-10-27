package com.example.flowershop;

import com.example.flowershop.dto.order.MakeCustomOrderRequest;
import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.CustomOrderStatus;
import com.example.flowershop.entity.enums.OrderStatus;
import com.example.flowershop.entity.enums.OrderType;
import com.example.flowershop.entity.enums.ShopStatus;
import com.example.flowershop.exception.ApiException;
import com.example.flowershop.repository.*;
import com.example.flowershop.service.GhnService;
import com.example.flowershop.service.NotificationService;
import com.example.flowershop.service.OrderService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Đơn đặt cọc tạo từ yêu cầu hoa tùy chỉnh: giá = báo giá + phí GHN, chỉ một đơn đang hoạt động mỗi yêu cầu. */
class CustomDepositOrderTests {
    private final OrderRepository orders = mock(OrderRepository.class);
    private final OrderDetailRepository details = mock(OrderDetailRepository.class);
    private final AddressRepository addresses = mock(AddressRepository.class);
    private final GhnService ghn = mock(GhnService.class);
    private final EntityManager em = mock(EntityManager.class);
    private final OrderService service = new OrderService(orders, details, mock(CartItemRepository.class),
            mock(UserRepository.class), addresses, mock(ProductRepository.class), mock(PaymentRepository.class),
            mock(CouponRepository.class), mock(ShopRepository.class), ghn, mock(NotificationService.class),
            mock(org.springframework.context.ApplicationEventPublisher.class));

    private final User customer = User.builder().id("c-1").fullName("Lan").phone("0901").build();
    private final Shop shop = Shop.builder().id("s-1").status(ShopStatus.ACTIVE).build();
    private CustomOrderRequest request;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "entityManager", em);
        request = CustomOrderRequest.builder().id("req-1").customer(customer).shop(shop).budget(new BigDecimal("900000"))
                .status(CustomOrderStatus.ACCEPTED).desiredComponents("{\"quantity\":1,\"quotedPrice\":800000}").build();
        when(em.find(CustomOrderRequest.class, "req-1", LockModeType.PESSIMISTIC_WRITE)).thenReturn(request);
        when(addresses.findById("addr-1")).thenReturn(Optional.of(Address.builder().id("addr-1").user(customer)
                .ghnDistrictId(1).ghnWardCode("w1").build()));
        when(addresses.findByShopIdAndUserIsNullOrderByCreatedDateAscIdAsc("s-1")).thenReturn(List.of(Address.builder()
                .id("shop-addr").isDefault(true).ghnDistrictId(2).ghnWardCode("w2").build()));
        when(ghn.calculateFee(anyInt(), anyString(), anyInt(), anyString(), anyInt())).thenReturn(new BigDecimal("30000"));
        when(orders.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(details.save(any(OrderDetail.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private MakeCustomOrderRequest body() {
        MakeCustomOrderRequest body = new MakeCustomOrderRequest();
        body.setDeliveryAddressId("addr-1");
        return body;
    }

    @Test
    void acceptedRequestBecomesAwaitingDepositOrderPricedByQuotePlusShipping() {
        var response = service.makeCustomOrder("c-1", "req-1", body());

        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(orders).save(saved.capture());
        Order order = saved.getValue();
        assertThat(order.getOrderType()).isEqualTo(OrderType.CUSTOM);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.AWAITING_DEPOSIT);
        assertThat(order.getCustomRequestId()).isEqualTo("req-1");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("830000");
        assertThat(order.getRecipientName()).isEqualTo("Lan");
        assertThat(response.getTotalAmount()).isEqualByComparingTo("830000");
    }

    @Test
    void secondActiveOrderForSameRequestIsRejected() {
        when(orders.existsByCustomRequestIdAndStatusNot("req-1", OrderStatus.CANCELLED)).thenReturn(true);
        assertThatThrownBy(() -> service.makeCustomOrder("c-1", "req-1", body())).isInstanceOf(ApiException.class);
        verify(orders, never()).save(any());
    }

    @Test
    void requestNotAcceptedOrOfAnotherCustomerIsRejected() {
        assertThatThrownBy(() -> service.makeCustomOrder("someone-else", "req-1", body())).isInstanceOf(ApiException.class);
        request.setStatus(CustomOrderStatus.REQUESTED);
        assertThatThrownBy(() -> service.makeCustomOrder("c-1", "req-1", body())).isInstanceOf(ApiException.class);
        verify(orders, never()).save(any());
    }
}
