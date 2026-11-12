package com.example.flowershop;

import com.example.flowershop.dto.account.ProfileRequest;
import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.*;
import com.example.flowershop.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** SRS 3.3 (số điện thoại không trùng, tên sản phẩm lúc mua) và 4.4 (quyền staff, quyền admin). */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:srscompliance;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.shop-follow-mail.enabled=false", "app.occasion-reminder.enabled=false"
})
@Transactional
class SrsComplianceTests {
    @MockitoBean JavaMailSender sender;
    @Autowired AccountService account;
    @Autowired ManagerProductService productService;
    @Autowired BlogService blogs;
    @Autowired ShopReportService reports;
    @Autowired StaffRefundService refundService;
    @Autowired AdminOversightService oversight;
    @Autowired UserRepository users;
    @Autowired ShopRepository shops;
    @Autowired ShopStaffRepository staffRows;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired OrderRepository orders;
    @Autowired RefundRepository refunds;
    @Autowired ProductReviewRepository reviews;

    private String id() { return UUID.randomUUID().toString(); }

    private User user(UserRole role) {
        return users.saveAndFlush(User.builder().id(id()).email(id() + "@example.test").fullName("U " + role)
                .role(role).status(UserStatus.ACTIVE).isEmailVerified(true).build());
    }

    @Test
    void secondAccountCannotTakeAnExistingPhoneButBlankPhoneIsAllowedTwice() {
        var a = user(UserRole.CUSTOMER);
        var b = user(UserRole.CUSTOMER);
        account.updateProfile(a.getId(), new ProfileRequest("A", "0912345678"));
        assertThatThrownBy(() -> account.updateProfile(b.getId(), new ProfileRequest("B", "0912345678")))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        account.updateProfile(a.getId(), new ProfileRequest("A", "0912345678")); // cùng chủ số thì vẫn lưu được
        account.updateProfile(a.getId(), new ProfileRequest("A", ""));
        account.updateProfile(b.getId(), new ProfileRequest("B", ""));
        assertThat(users.findById(b.getId()).orElseThrow().getPhone()).isNull();
    }

    @Test
    void orderLineKeepsThePurchaseTimeProductName() {
        var product = Product.builder().id(id()).name("Tên mới").build();
        assertThat(OrderDetail.builder().product(product).productName("Tên lúc mua").build().snapshotName()).isEqualTo("Tên lúc mua");
        assertThat(OrderDetail.builder().product(product).build().snapshotName()).isEqualTo("Tên mới");
        assertThat(OrderDetail.builder().build().snapshotName()).isNull();
    }

    @Test
    void activeStaffManagesProductsBlogsAndReportsButCannotDeleteProducts() {
        var owner = user(UserRole.SHOP);
        var worker = user(UserRole.SHOP_STAFF);
        var shop = shops.saveAndFlush(Shop.builder().id(id()).owner(owner).name("S").status(ShopStatus.ACTIVE).build());
        var category = categories.saveAndFlush(Category.builder().id(id()).name("C").status(CategoryStatus.ACTIVE).build());
        var product = products.saveAndFlush(Product.builder().id(id()).shop(shop).category(category).name("Hồng")
                .price(new BigDecimal("100000")).stock(3).status(ProductStatus.ACTIVE).type(ProductType.READY_MADE).build());
        ShopStaff row = new ShopStaff();
        row.setId(id()); row.setShop(shop); row.setUser(worker); row.setActive(false);
        staffRows.saveAndFlush(row);

        assertThatThrownBy(() -> productService.list(shop.getId(), worker.getId(), 0)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> blogs.listManaged(shop.getId(), worker.getId(), 0)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> reports.report(shop.getId(), worker.getId(), "MONTH", null)).isInstanceOf(ResponseStatusException.class);

        row.setActive(true);
        staffRows.saveAndFlush(row);
        assertThat(productService.list(shop.getId(), worker.getId(), 0).content()).hasSize(1);
        productService.hide(shop.getId(), product.getId(), worker.getId());
        assertThat(products.findById(product.getId()).orElseThrow().getStatus()).isEqualTo(ProductStatus.INACTIVE);
        assertThat(blogs.listManaged(shop.getId(), worker.getId(), 0).content()).isEmpty();
        assertThat(reports.report(shop.getId(), worker.getId(), "MONTH", null)).isNotNull();
        assertThatThrownBy(() -> productService.remove(shop.getId(), product.getId(), worker.getId()))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        productService.remove(shop.getId(), product.getId(), owner.getId()); // chủ shop vẫn xóa được
    }

    @Test
    void adminSeesOrdersProcessesAnyRefundAndRemovesReviews() {
        var owner = user(UserRole.SHOP);
        var customer = user(UserRole.CUSTOMER);
        var shop = shops.saveAndFlush(Shop.builder().id(id()).owner(owner).name("S").status(ShopStatus.ACTIVE).build());
        var category = categories.saveAndFlush(Category.builder().id(id()).name("C").status(CategoryStatus.ACTIVE).build());
        var product = products.saveAndFlush(Product.builder().id(id()).shop(shop).category(category).name("Hồng")
                .price(new BigDecimal("100000")).stock(3).status(ProductStatus.ACTIVE).type(ProductType.READY_MADE).build());
        var order = orders.saveAndFlush(Order.builder().id(id()).customer(customer).shop(shop).orderType(OrderType.STANDARD)
                .status(OrderStatus.COMPLETED).subTotal(new BigDecimal("100000")).totalAmount(new BigDecimal("100000")).build());
        var refund = refunds.saveAndFlush(Refund.builder().id(id()).order(order).amount(new BigDecimal("100000")).reason("Giao hàng trễ").build());
        var review = reviews.saveAndFlush(ProductReview.builder().id(id()).product(product).user(customer).order(order).rating(1).comment("spam").build());
        var admin = user(UserRole.ADMIN);

        assertThat(oversight.orders("COMPLETED", 0).content()).extracting(AdminOversightService.OrderRow::id).contains(order.getId());
        assertThat(oversight.orders("", 0).totalElements()).isGreaterThanOrEqualTo(1);
        assertThat(refundService.adminList("REQUESTED", 0).content()).extracting(StaffRefundService.RefundResponse::id).contains(refund.getId());
        assertThat(refundService.adminUpdateStatus(admin.getId(), refund.getId(), "APPROVED", null).status()).isEqualTo("APPROVED");
        assertThat(refundService.adminUpdateStatus(admin.getId(), refund.getId(), "REJECTED", "Không đủ bằng chứng").status()).isEqualTo("REJECTED");
        assertThatThrownBy(() -> refundService.adminUpdateStatus(admin.getId(), refund.getId(), "PROCESSED", null))
                .isInstanceOf(ResponseStatusException.class); // đã đóng, không sửa lại (SRS 3.3 Refund Finality)

        assertThat(oversight.reviews(0).content()).extracting(AdminOversightService.ReviewRow::id).contains(review.getId());
        oversight.deleteReview(review.getId());
        assertThat(reviews.findById(review.getId())).isEmpty();
    }
}
