package com.example.flowershop;

import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.exception.ApiException;
import com.example.flowershop.repository.*;
import com.example.flowershop.service.CouponService;
import com.example.flowershop.service.GhnService;
import com.example.flowershop.service.NotificationService;
import com.example.flowershop.service.OrderService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Voucher: CRUD theo quyền, xem trước, áp mã, luật 1 mã/khách và trả mã khi hủy đơn. */
class CouponTests {
    private final CouponRepository coupons = mock(CouponRepository.class);
    private final ShopRepository shops = mock(ShopRepository.class);
    private final OrderRepository orders = mock(OrderRepository.class);
    private final CartItemRepository cartItems = mock(CartItemRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final CouponService service = new CouponService(coupons, shops, orders, cartItems, users);

    private final User owner = User.builder().id("owner-1").build();
    private final User stranger = User.builder().id("owner-2").build();
    private final Shop shop = Shop.builder().id("shop-1").owner(owner).status(ShopStatus.ACTIVE).build();
    private final Shop otherShop = Shop.builder().id("shop-2").owner(stranger).status(ShopStatus.ACTIVE).build();

    private static LocalDateTime now() { return LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")); }

    private CouponService.Input input(String code, DiscountType type, String value, String min, String max) {
        return new CouponService.Input(code, type, new BigDecimal(value), new BigDecimal(min),
                max == null ? null : new BigDecimal(max), now().minusDays(1), now().plusDays(10), 5, CouponStatus.ACTIVE);
    }

    private Coupon coupon(String id, String code, Shop s, DiscountType type, String value, String min, String max) {
        return Coupon.builder().id(id).code(code).shop(s).discountType(type).discountValue(new BigDecimal(value))
                .minOrderValue(new BigDecimal(min)).maxDiscountValue(max == null ? null : new BigDecimal(max))
                .startDate(now().minusDays(1)).endDate(now().plusDays(5)).usageLimit(3).usedCount(0)
                .status(CouponStatus.ACTIVE).build();
    }

    private void cartOf(String userId, Shop s, String price, int qty) {
        Product p = Product.builder().id("p-1").name("Hoa hồng").shop(s).price(new BigDecimal(price)).build();
        when(cartItems.findById("ci-1")).thenReturn(Optional.of(CartItem.builder().id("ci-1")
                .user(User.builder().id(userId).build()).product(p).quantity(qty).build()));
    }

    @Test
    void adminCreatesPlatformVoucherWithUppercaseCode() {
        when(coupons.saveAndFlush(any(Coupon.class))).thenAnswer(i -> i.getArgument(0));

        var view = service.save("admin-1", null, null, input("tet2026", DiscountType.PERCENTAGE, "10", "100000", "50000"));

        assertThat(view.code()).isEqualTo("TET2026");
        assertThat(view.shopId()).isNull();
    }

    @Test
    void percentageVoucherNeedsCapAndValidDates() {
        assertThatThrownBy(() -> service.save("admin-1", null, null, input("ABC", DiscountType.PERCENTAGE, "10", "0", null)))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.save("admin-1", null, null, input("ABC", DiscountType.PERCENTAGE, "101", "0", "5000")))
                .isInstanceOf(ApiException.class);
        var reversed = new CouponService.Input("ABC", DiscountType.FIXED_AMOUNT, BigDecimal.TEN, BigDecimal.ZERO, null,
                now().plusDays(2), now().plusDays(1), 1, CouponStatus.ACTIVE);
        assertThatThrownBy(() -> service.save("admin-1", null, null, reversed)).isInstanceOf(ApiException.class);
    }

    @Test
    void duplicateCodeIsRejectedButSameVoucherMayKeepItsCode() {
        when(coupons.findByCodeIgnoreCase("DUP")).thenReturn(Optional.of(coupon("c-9", "DUP", null, DiscountType.FIXED_AMOUNT, "5", "0", null)));

        assertThatThrownBy(() -> service.save("admin-1", null, null, input("dup", DiscountType.FIXED_AMOUNT, "5", "0", null)))
                .isInstanceOf(ApiException.class).hasMessageContaining("đã tồn tại");
    }

    @Test
    void shopOwnerCreatesOwnVoucherButNotForSomeoneElsesShop() {
        when(shops.findById("shop-1")).thenReturn(Optional.of(shop));
        when(shops.findById("shop-2")).thenReturn(Optional.of(otherShop));
        when(coupons.saveAndFlush(any(Coupon.class))).thenAnswer(i -> i.getArgument(0));

        var view = service.save("owner-1", "shop-1", null, input("SHOP10", DiscountType.FIXED_AMOUNT, "20000", "0", null));
        assertThat(view.shopId()).isEqualTo("shop-1");

        assertThatThrownBy(() -> service.save("owner-1", "shop-2", null, input("SHOP11", DiscountType.FIXED_AMOUNT, "20000", "0", null)))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.list("owner-1", "shop-2")).isInstanceOf(ApiException.class);
    }

    @Test
    void inactiveShopCannotCreateVoucher() {
        shop.setStatus(ShopStatus.PENDING);
        when(shops.findById("shop-1")).thenReturn(Optional.of(shop));

        assertThatThrownBy(() -> service.save("owner-1", "shop-1", null, input("SHOP10", DiscountType.FIXED_AMOUNT, "20000", "0", null)))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void shopCannotEditOrDeletePlatformVoucher() {
        when(shops.findById("shop-1")).thenReturn(Optional.of(shop));
        when(coupons.findLocked("c-1")).thenReturn(Optional.of(coupon("c-1", "SAN", null, DiscountType.FIXED_AMOUNT, "5", "0", null)));

        assertThatThrownBy(() -> service.delete("owner-1", "shop-1", "c-1")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.save("owner-1", "shop-1", "c-1", input("SAN", DiscountType.FIXED_AMOUNT, "5", "0", null)))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void adminCannotDeleteShopVoucherThroughPlatformRoute() {
        when(coupons.findLocked("c-2")).thenReturn(Optional.of(coupon("c-2", "SHOPX", shop, DiscountType.FIXED_AMOUNT, "5", "0", null)));

        assertThatThrownBy(() -> service.delete("admin-1", null, "c-2")).isInstanceOf(ApiException.class);
    }

    @Test
    void deleteIsSoft() {
        Coupon c = coupon("c-1", "SAN", null, DiscountType.FIXED_AMOUNT, "5", "0", null);
        when(coupons.findLocked("c-1")).thenReturn(Optional.of(c));

        service.delete("admin-1", null, "c-1");

        assertThat(c.isDeleted()).isTrue();
        assertThat(c.getStatus()).isEqualTo(CouponStatus.DISABLED);
        verify(coupons, never()).delete(any(Coupon.class));
    }

    @Test
    void usageLimitCannotDropBelowUsedCount() {
        Coupon c = coupon("c-1", "SAN", null, DiscountType.FIXED_AMOUNT, "5", "0", null);
        c.setUsedCount(6);
        when(coupons.findLocked("c-1")).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.save("admin-1", null, "c-1", input("SAN", DiscountType.FIXED_AMOUNT, "5", "0", null)))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void previewListsReasonsAndRecommendsBiggestSaving() {
        cartOf("c-1", shop, "300000", 1);
        Coupon pct = coupon("v-1", "PCT10", null, DiscountType.PERCENTAGE, "10", "0", "20000");
        Coupon fixed = coupon("v-2", "FIX30", shop, DiscountType.FIXED_AMOUNT, "30000", "0", null);
        Coupon big = coupon("v-3", "MIN500", null, DiscountType.FIXED_AMOUNT, "90000", "500000", null);
        Coupon expired = coupon("v-4", "OLD", null, DiscountType.FIXED_AMOUNT, "5000", "0", null);
        expired.setEndDate(now().minusDays(1));
        Coupon full = coupon("v-5", "FULL", null, DiscountType.FIXED_AMOUNT, "5000", "0", null);
        full.setUsedCount(3);
        when(coupons.available("shop-1")).thenReturn(List.of(pct, fixed, big, expired, full));

        var preview = service.preview("c-1", "shop-1", List.of("ci-1"), null);

        assertThat(preview.recommendedCode()).isEqualTo("FIX30");
        assertThat(preview.bestSaving()).isEqualByComparingTo("30000");
        assertThat(preview.vouchers()).extracting(CouponService.Choice::code, CouponService.Choice::eligible)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("PCT10", true), org.assertj.core.groups.Tuple.tuple("FIX30", true),
                        org.assertj.core.groups.Tuple.tuple("MIN500", false), org.assertj.core.groups.Tuple.tuple("OLD", false),
                        org.assertj.core.groups.Tuple.tuple("FULL", false));
        assertThat(preview.vouchers().get(2).reason()).contains("tối thiểu");
        assertThat(preview.vouchers().get(3).reason()).contains("hết hạn");
        assertThat(preview.vouchers().get(4).reason()).contains("hết lượt");
        assertThat(preview.discountAmount()).isEqualByComparingTo("0");
        verify(coupons, never()).save(any());
    }

    @Test
    void previewWithSelectedCodeReturnsTotalAfterDiscountAndCapsPercentage() {
        cartOf("c-1", shop, "300000", 1);
        when(coupons.available("shop-1")).thenReturn(List.of(coupon("v-1", "PCT10", null, DiscountType.PERCENTAGE, "50", "0", "20000")));

        var preview = service.preview("c-1", "shop-1", List.of("ci-1"), "pct10");

        assertThat(preview.selectedCode()).isEqualTo("PCT10");
        assertThat(preview.discountAmount()).isEqualByComparingTo("20000");
        assertThat(preview.totalAfterDiscount()).isEqualByComparingTo("280000");
    }

    @Test
    void previewRejectsUnknownOrIneligibleSelectedCodeAndForeignCart() {
        cartOf("c-1", shop, "100000", 1);
        Coupon min = coupon("v-1", "MIN500", null, DiscountType.FIXED_AMOUNT, "9000", "500000", null);
        when(coupons.available("shop-1")).thenReturn(List.of(min));

        assertThatThrownBy(() -> service.preview("c-1", "shop-1", List.of("ci-1"), "MIN500")).hasMessageContaining("tối thiểu");
        assertThatThrownBy(() -> service.preview("c-1", "shop-1", List.of("ci-1"), "NOPE")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.preview("c-2", "shop-1", List.of("ci-1"), null)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.preview("c-1", "shop-2", List.of("ci-1"), null)).isInstanceOf(ApiException.class);
    }

    @Test
    void previewMarksEveryVoucherUnusableWhenCustomerAlreadyUsedOne() {
        cartOf("c-1", shop, "300000", 1);
        when(coupons.available("shop-1")).thenReturn(List.of(coupon("v-1", "PCT10", null, DiscountType.FIXED_AMOUNT, "10000", "0", null)));
        when(orders.existsByCustomerIdAndCouponIdIsNotNullAndStatusIn("c-1", CouponService.VOUCHER_USED_STATUSES)).thenReturn(true);

        var preview = service.preview("c-1", "shop-1", List.of("ci-1"), null);

        assertThat(preview.vouchers().get(0).eligible()).isFalse();
        assertThat(preview.vouchers().get(0).reason()).contains("mỗi người chỉ được dùng 1 mã");
        assertThat(preview.recommendedCode()).isNull();
    }

    @Test
    void applyHoldsOneUseAndReturnsDiscount() {
        Coupon c = coupon("v-1", "PCT10", null, DiscountType.PERCENTAGE, "10", "100000", "50000");
        when(coupons.findByCodeForUpdate("PCT10")).thenReturn(Optional.of(c));

        var applied = service.apply("c-1", "shop-1", new BigDecimal("300000"), " PCT10 ");

        assertThat(applied.couponId()).isEqualTo("v-1");
        assertThat(applied.discountAmount()).isEqualByComparingTo("30000");
        assertThat(c.getUsedCount()).isEqualTo(1);
        verify(users).findByIdForUpdate("c-1");
    }

    @Test
    void unpaidOrderHoldingVoucherDoesNotLockCustomer() {
        Coupon c = coupon("v-1", "PCT10", null, DiscountType.PERCENTAGE, "10", "100000", "50000");
        when(coupons.findByCodeForUpdate("PCT10")).thenReturn(Optional.of(c));
        when(orders.existsByCustomerIdAndCouponIdIsNotNullAndStatusIn("c-1", CouponService.VOUCHER_USED_STATUSES)).thenReturn(false);

        assertThat(service.apply("c-1", "shop-1", new BigDecimal("300000"), "PCT10").couponId()).isEqualTo("v-1");
        assertThat(CouponService.VOUCHER_USED_STATUSES).doesNotContain(OrderStatus.PENDING, OrderStatus.AWAITING_DEPOSIT, OrderStatus.CANCELLED);
    }

    @Test
    void applyRejectsSecondVoucherForSameCustomer() {
        when(orders.existsByCustomerIdAndCouponIdIsNotNullAndStatusIn("c-1", CouponService.VOUCHER_USED_STATUSES)).thenReturn(true);

        assertThatThrownBy(() -> service.apply("c-1", "shop-1", new BigDecimal("300000"), "ANY"))
                .isInstanceOf(ApiException.class).hasMessage("Bạn đã dùng mã giảm giá rồi, mỗi người chỉ được dùng 1 mã.");
        verify(coupons, never()).findByCodeForUpdate(any());
    }

    @Test
    void applyRejectsExpiredExhaustedWrongShopBelowMinimumAndDisabled() {
        BigDecimal sub = new BigDecimal("100000");
        Coupon expired = coupon("v-1", "E", null, DiscountType.FIXED_AMOUNT, "5000", "0", null);
        expired.setEndDate(now().minusMinutes(1));
        Coupon full = coupon("v-2", "F", null, DiscountType.FIXED_AMOUNT, "5000", "0", null);
        full.setUsedCount(3);
        Coupon foreign = coupon("v-3", "S", otherShop, DiscountType.FIXED_AMOUNT, "5000", "0", null);
        Coupon min = coupon("v-4", "M", null, DiscountType.FIXED_AMOUNT, "5000", "200000", null);
        Coupon off = coupon("v-5", "D", null, DiscountType.FIXED_AMOUNT, "5000", "0", null);
        off.setStatus(CouponStatus.DISABLED);
        for (Coupon c : List.of(expired, full, foreign, min, off)) {
            when(coupons.findByCodeForUpdate(c.getCode())).thenReturn(Optional.of(c));
            assertThatThrownBy(() -> service.apply("c-1", "shop-1", sub, c.getCode())).isInstanceOf(ApiException.class);
            assertThat(c.getUsedCount()).isNotEqualTo(4);
        }
        assertThat(full.getUsedCount()).isEqualTo(3);
        assertThatThrownBy(() -> service.apply("c-1", "shop-1", sub, "UNKNOWN")).hasMessage("Mã giảm giá không hợp lệ.");
    }

    @Test
    void applyCapsFixedDiscountAtSubTotalMinusNothingButRejectsFullDiscount() {
        Coupon c = coupon("v-1", "BIG", null, DiscountType.FIXED_AMOUNT, "500000", "0", null);
        when(coupons.findByCodeForUpdate("BIG")).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.apply("c-1", "shop-1", new BigDecimal("100000"), "BIG")).isInstanceOf(ApiException.class);
    }

    @Test
    void cancellingOrderReturnsVoucherUse() {
        OrderRepository orderRepo = mock(OrderRepository.class);
        OrderDetailRepository details = mock(OrderDetailRepository.class);
        CouponRepository couponRepo = mock(CouponRepository.class);
        OrderService orderService = new OrderService(orderRepo, details, mock(CartItemRepository.class),
                mock(UserRepository.class), mock(AddressRepository.class), mock(ProductRepository.class),
                mock(PaymentRepository.class), couponRepo, mock(ShopRepository.class), mock(GhnService.class),
                mock(NotificationService.class), mock(org.springframework.context.ApplicationEventPublisher.class),
                mock(RefundRepository.class));
        Coupon c = coupon("v-1", "PCT10", null, DiscountType.FIXED_AMOUNT, "5000", "0", null);
        c.setUsedCount(2);
        Order o = Order.builder().id("o-1").customer(User.builder().id("c-1").build()).status(OrderStatus.PENDING).couponId("v-1").build();
        when(orderRepo.findByIdForUpdate("o-1")).thenReturn(Optional.of(o));
        when(details.findByOrderId("o-1")).thenReturn(List.of());
        when(couponRepo.findById("v-1")).thenReturn(Optional.of(c));

        orderService.cancelByCustomer("c-1", "o-1", null);

        assertThat(c.getUsedCount()).isEqualTo(1);
    }
}
