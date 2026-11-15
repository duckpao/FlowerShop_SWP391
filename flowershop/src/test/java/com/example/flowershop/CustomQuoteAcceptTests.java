package com.example.flowershop;

import com.example.flowershop.dto.order.MakeCustomOrderRequest;
import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.CustomOrderStatus;
import com.example.flowershop.entity.enums.OrderStatus;
import com.example.flowershop.entity.enums.ShopStatus;
import com.example.flowershop.exception.ApiException;
import com.example.flowershop.repository.*;
import com.example.flowershop.service.CouponService;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Khách chấp nhận báo giá bằng cách đặt cọc: cọc = giá x %, voucher chỉ áp lúc này, yêu cầu chuyển ACCEPTED. */
class CustomQuoteAcceptTests {
    private final OrderRepository orders = mock(OrderRepository.class);
    private final OrderDetailRepository details = mock(OrderDetailRepository.class);
    private final AddressRepository addresses = mock(AddressRepository.class);
    private final GhnService ghn = mock(GhnService.class);
    private final EntityManager em = mock(EntityManager.class);
    private final CouponService coupons = mock(CouponService.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private final OrderService service = new OrderService(orders, details, mock(CartItemRepository.class),
            mock(UserRepository.class), addresses, mock(ProductRepository.class), mock(PaymentRepository.class),
            mock(CouponRepository.class), mock(ShopRepository.class), ghn, notifications,
            mock(org.springframework.context.ApplicationEventPublisher.class),
            mock(com.example.flowershop.repository.RefundRepository.class));

    private final User customer = User.builder().id("c-1").fullName("Lan").phone("0901").build();
    private final Shop shop = Shop.builder().id("s-1").status(ShopStatus.ACTIVE).build();
    private CustomOrderRequest request;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "entityManager", em);
        ReflectionTestUtils.setField(service, "couponService", coupons);
        request = CustomOrderRequest.builder().id("req-1").customer(customer).shop(shop).budget(new BigDecimal("900000"))
                .status(CustomOrderStatus.QUOTED).quotedPrice(new BigDecimal("800000")).depositPercent(30)
                .desiredComponents("{\"quantity\":1}").build();
        when(em.find(CustomOrderRequest.class, "req-1", LockModeType.PESSIMISTIC_WRITE)).thenReturn(request);
        when(addresses.findById("addr-1")).thenReturn(Optional.of(Address.builder().id("addr-1").user(customer)
                .ghnDistrictId(1).ghnWardCode("w1").build()));
        when(addresses.findByShopIdAndUserIsNullOrderByCreatedDateAscIdAsc("s-1")).thenReturn(List.of(Address.builder()
                .id("shop-addr").isDefault(true).ghnDistrictId(2).ghnWardCode("w2").build()));
        when(ghn.calculateFee(anyInt(), anyString(), anyInt(), anyString(), anyInt())).thenReturn(new BigDecimal("30000"));
        when(orders.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(details.save(any(OrderDetail.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private MakeCustomOrderRequest body(String coupon) {
        MakeCustomOrderRequest body = new MakeCustomOrderRequest();
        body.setDeliveryAddressId("addr-1");
        body.setCouponId(coupon);
        return body;
    }

    @Test
    void acceptingQuoteCreatesDepositOrderWithPercentDepositAndMarksRequestAccepted() {
        service.makeCustomOrder("c-1", "req-1", body(null));
        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(orders).save(saved.capture());
        Order order = saved.getValue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.AWAITING_DEPOSIT);
        assertThat(order.getDepositAmount()).isEqualByComparingTo("240000");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("830000");
        assertThat(request.getStatus()).isEqualTo(CustomOrderStatus.ACCEPTED);
        verify(notifications).notifyCustomQuoteAnswered(request, true, null);
    }

    @Test
    void voucherIsAppliedOnlyWhenAcceptingAndDepositFollowsDiscountedPrice() {
        when(coupons.apply("c-1", "s-1", new BigDecimal("800000"), "SALE")).thenReturn(new CouponService.Applied("cp-1", new BigDecimal("100000")));
        service.makeCustomOrder("c-1", "req-1", body("SALE"));
        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(orders).save(saved.capture());
        Order order = saved.getValue();
        assertThat(order.getCouponId()).isEqualTo("cp-1");
        assertThat(order.getDiscountAmount()).isEqualByComparingTo("100000");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("730000");
        assertThat(order.getDepositAmount()).isEqualByComparingTo("210000");
    }

    @Test
    void secondVoucherIsRejectedAndNothingIsCreated() {
        when(coupons.apply(eq("c-1"), anyString(), any(), anyString())).thenThrow(ApiException.badRequest(CouponService.ONE_VOUCHER_ONLY));
        assertThatThrownBy(() -> service.makeCustomOrder("c-1", "req-1", body("SALE2")))
                .isInstanceOf(ApiException.class).hasMessageContaining("1 mã");
        verify(orders, never()).save(any());
        assertThat(request.getStatus()).isEqualTo(CustomOrderStatus.QUOTED);
    }

    @Test
    void expiredQuoteCannotBeAccepted() {
        request.setQuoteValidUntil(LocalDateTime.now().minusHours(1));
        assertThatThrownBy(() -> service.makeCustomOrder("c-1", "req-1", body(null)))
                .isInstanceOf(ApiException.class).hasMessageContaining("hết hạn");
        verify(orders, never()).save(any());
    }

    @Test
    void requestWithoutQuoteOrRejectedCannotBeAccepted() {
        request.setStatus(CustomOrderStatus.NEGOTIATING);
        assertThatThrownBy(() -> service.makeCustomOrder("c-1", "req-1", body(null))).isInstanceOf(ApiException.class);
        request.setStatus(CustomOrderStatus.REJECTED);
        assertThatThrownBy(() -> service.makeCustomOrder("c-1", "req-1", body(null))).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.makeCustomOrder("someone-else", "req-1", body(null))).isInstanceOf(ApiException.class);
        verify(orders, never()).save(any());
    }
}
