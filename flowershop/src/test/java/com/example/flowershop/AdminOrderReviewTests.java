package com.example.flowershop;

import com.example.flowershop.dto.auth.CurrentUser;
import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.*;
import com.example.flowershop.service.AdminOrderService;
import com.example.flowershop.service.AdminReviewService;
import com.example.flowershop.service.NotificationService;
import com.example.flowershop.service.TokenAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** UC33: admin xem đơn toàn hệ thống và xóa đánh giá; người không phải admin bị chặn. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:adminorders;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.shop-follow-mail.enabled=false", "app.occasion-reminder.enabled=false"
})
@AutoConfigureMockMvc
@Transactional
class AdminOrderReviewTests {
    @MockitoBean JavaMailSender mail;
    @MockitoBean TokenAuthService tokenAuth;
    @MockitoBean NotificationService notifications;
    @Autowired AdminOrderService adminOrders;
    @Autowired AdminReviewService adminReviews;
    @Autowired UserRepository users;
    @Autowired ShopRepository shops;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired AddressRepository addresses;
    @Autowired OrderRepository orders;
    @Autowired OrderDetailRepository orderDetails;
    @Autowired RefundRepository refunds;
    @Autowired CouponRepository coupons;
    @Autowired ProductReviewRepository reviews;
    @Autowired MockMvc mvc;

    User customer, other, owner;
    Shop shop;
    Product rose;
    Order done, pending;

    String id() { return UUID.randomUUID().toString(); }

    User user(UserRole role, String name) {
        return users.saveAndFlush(User.builder().id(id()).email(id() + "@example.test").fullName(name).role(role)
                .status(UserStatus.ACTIVE).isEmailVerified(true).build());
    }

    Order order(User who, OrderStatus st, String couponId) {
        Address address = addresses.saveAndFlush(Address.builder().id(id()).user(who).addressLine("1 Lê Lợi")
                .city("Hà Nội").district("Hoàn Kiếm").ward("Hàng Bạc").build());
        Order o = orders.saveAndFlush(Order.builder().id(id()).customer(who).shop(shop).deliveryAddress(address)
                .subTotal(new BigDecimal("200000")).totalAmount(new BigDecimal("230000"))
                .shippingFee(new BigDecimal("30000")).status(st).couponId(couponId).build());
        orderDetails.saveAndFlush(OrderDetail.builder().id(id()).order(o).product(rose)
                .price(new BigDecimal("100000")).quantity(2).build());
        return o;
    }

    ProductReview review(User who, Order o, int rating, String comment) {
        return reviews.saveAndFlush(ProductReview.builder().id(id()).product(rose).user(who).order(o)
                .rating(rating).comment(comment).build());
    }

    double average() {
        return ((Number) reviews.summarize(List.of(rose.getId())).get(0)[1]).doubleValue();
    }

    @BeforeEach
    void fixture() {
        customer = user(UserRole.CUSTOMER, "Nguyễn Lan");
        other = user(UserRole.CUSTOMER, "Trần Minh");
        owner = user(UserRole.SHOP, "Chủ shop");
        shop = shops.saveAndFlush(Shop.builder().id(id()).owner(owner).name("Hoa Xuân").status(ShopStatus.ACTIVE).build());
        Category category = categories.saveAndFlush(Category.builder().id(id()).name("Hoa").status(CategoryStatus.ACTIVE).build());
        rose = products.saveAndFlush(Product.builder().id(id()).shop(shop).category(category).name("Hồng đỏ")
                .price(new BigDecimal("100000")).stock(5).build());
        done = order(customer, OrderStatus.COMPLETED, null);
        pending = order(other, OrderStatus.PENDING, null);
    }

    @Test
    void adminListsFiltersAndSearchesOrders() {
        assertThat(adminOrders.list(null, null, null, null, 0, 10).totalElements()).isEqualTo(2);
        assertThat(adminOrders.list("completed", null, null, null, 0, 10).content())
                .extracting(AdminOrderService.OrderRow::id).containsExactly(done.getId());
        assertThat(adminOrders.list(null, "trần", null, null, 0, 10).content())
                .extracting(AdminOrderService.OrderRow::id).containsExactly(pending.getId());
        assertThat(adminOrders.list(null, "hoa xuân", null, null, 0, 10).totalElements()).isEqualTo(2);
        assertThat(adminOrders.list(null, done.getId().substring(0, 8), null, null, 0, 10).content())
                .extracting(AdminOrderService.OrderRow::id).containsExactly(done.getId());
        String today = java.time.LocalDate.now().toString();
        assertThat(adminOrders.list(null, null, today, today, 0, 10).totalElements()).isEqualTo(2);
        assertThat(adminOrders.list(null, null, java.time.LocalDate.now().plusDays(1).toString(), null, 0, 10).totalElements()).isZero();
        assertThatThrownBy(() -> adminOrders.list("BOGUS", null, null, null, 0, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adminOrders.list(null, null, "2026-13-40", null, 0, 10)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void adminOrderDetailShowsItemsCouponAndRefunds() {
        Coupon coupon = coupons.saveAndFlush(Coupon.builder().id(id()).code("SALE10").discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.TEN).startDate(java.time.LocalDateTime.now().minusDays(1))
                .endDate(java.time.LocalDateTime.now().plusDays(1)).build());
        done.setCouponId(coupon.getId());
        orders.saveAndFlush(done);
        refunds.saveAndFlush(Refund.builder().id(id()).order(done).amount(new BigDecimal("50000")).reason("Hoa héo").build());
        var detail = adminOrders.detail(done.getId());
        assertThat(detail.couponCode()).isEqualTo("SALE10");
        assertThat(detail.customerName()).isEqualTo("Nguyễn Lan");
        assertThat(detail.shopName()).isEqualTo("Hoa Xuân");
        assertThat(detail.items()).hasSize(1);
        assertThat(detail.refunds()).hasSize(1);
        assertThat(detail.deliveryAddress()).contains("Hà Nội");
        assertThatThrownBy(() -> adminOrders.detail("missing")).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode().value()).isEqualTo(404));
    }

    @Test
    void adminListsReviewsWithFilters() {
        review(customer, done, 5, "Rất đẹp");
        review(other, pending, 1, "Hoa héo, giao trễ");
        assertThat(adminReviews.list(null, null, null, null, 0, 10).totalElements()).isEqualTo(2);
        assertThat(adminReviews.list(1, null, null, null, 0, 10).content()).extracting(AdminReviewService.ReviewRow::reviewerName)
                .containsExactly("Trần Minh");
        assertThat(adminReviews.list(null, rose.getId(), shop.getId(), "đẹp", 0, 10).content()).hasSize(1);
        assertThat(adminReviews.list(null, "other-product", null, null, 0, 10).content()).isEmpty();
        assertThat(adminReviews.list(null, null, null, "hồng đỏ", 0, 10).totalElements()).isEqualTo(2);
        assertThatThrownBy(() -> adminReviews.list(9, null, null, null, 0, 10)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deletingReviewUpdatesAverageNotifiesCustomerAndAllowsReReview() {
        review(customer, done, 5, "Rất đẹp");
        ProductReview bad = review(other, pending, 1, "Spam quảng cáo");
        assertThat(average()).isEqualTo(3.0);

        adminReviews.delete(bad.getId(), "admin-1", "Nội dung quảng cáo");

        assertThat(reviews.findById(bad.getId())).isEmpty();
        assertThat(average()).isEqualTo(5.0);
        verify(notifications).notifyUser(eq(other), eq("Đánh giá đã bị gỡ"), contains("Nội dung quảng cáo"), any());
        assertThat(reviews.findByProductIdAndUserId(rose.getId(), other.getId())).isEmpty();
        assertThatThrownBy(() -> adminReviews.delete(bad.getId(), "admin-1", null)).isInstanceOfSatisfying(
                ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(404));
    }

    @Test
    void onlyAdminReachesTheEndpoints() throws Exception {
        when(tokenAuth.authenticate("admin")).thenReturn(new CurrentUser("admin-1", "a@x.test", "Admin", "ADMIN"));
        when(tokenAuth.authenticate("customer")).thenReturn(new CurrentUser(customer.getId(), "c@x.test", "Khách", "CUSTOMER"));
        when(tokenAuth.authenticate("shop")).thenReturn(new CurrentUser(owner.getId(), "s@x.test", "Shop", "SHOP"));
        ProductReview r = review(customer, done, 4, "Ổn");

        mvc.perform(get("/api/admin/orders")).andExpect(status().isUnauthorized());
        for (String token : new String[]{"customer", "shop"}) {
            mvc.perform(get("/api/admin/orders").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
            mvc.perform(get("/api/admin/orders/" + done.getId()).header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
            mvc.perform(get("/api/admin/reviews").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
            mvc.perform(delete("/api/admin/reviews/" + r.getId()).header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/admin/orders").header("Authorization", "Bearer admin")).andExpect(status().isOk());
        mvc.perform(get("/api/admin/orders/" + done.getId()).header("Authorization", "Bearer admin")).andExpect(status().isOk());
        mvc.perform(get("/api/admin/reviews").header("Authorization", "Bearer admin")).andExpect(status().isOk());
        assertThat(reviews.findById(r.getId())).isPresent();
    }
}
