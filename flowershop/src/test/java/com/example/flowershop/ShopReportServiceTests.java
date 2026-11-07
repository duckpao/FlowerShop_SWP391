package com.example.flowershop;

import com.example.flowershop.entity.Address;
import com.example.flowershop.entity.Blog;
import com.example.flowershop.entity.BlogComment;
import com.example.flowershop.entity.BlogInteraction;
import com.example.flowershop.entity.BlogInteractionId;
import com.example.flowershop.entity.Category;
import com.example.flowershop.entity.FavoriteShop;
import com.example.flowershop.entity.FavoriteShopId;
import com.example.flowershop.entity.Order;
import com.example.flowershop.entity.OrderDetail;
import com.example.flowershop.entity.Payment;
import com.example.flowershop.entity.Product;
import com.example.flowershop.entity.Refund;
import com.example.flowershop.entity.Shop;
import com.example.flowershop.entity.User;
import com.example.flowershop.entity.enums.OrderStatus;
import com.example.flowershop.entity.enums.BlogStatus;
import com.example.flowershop.entity.enums.InteractionType;
import com.example.flowershop.entity.enums.PaymentMethod;
import com.example.flowershop.entity.enums.PaymentStatus;
import com.example.flowershop.entity.enums.PaymentType;
import com.example.flowershop.entity.enums.RefundStatus;
import com.example.flowershop.entity.enums.ShopStatus;
import com.example.flowershop.entity.enums.UserRole;
import com.example.flowershop.entity.enums.UserStatus;
import com.example.flowershop.repository.AddressRepository;
import com.example.flowershop.repository.BlogRepository;
import com.example.flowershop.repository.BlogCommentRepository;
import com.example.flowershop.repository.BlogInteractionRepository;
import com.example.flowershop.repository.CategoryRepository;
import com.example.flowershop.repository.FavoriteShopRepository;
import com.example.flowershop.repository.OrderDetailRepository;
import com.example.flowershop.repository.OrderRepository;
import com.example.flowershop.repository.PaymentRepository;
import com.example.flowershop.repository.ProductRepository;
import com.example.flowershop.repository.RefundRepository;
import com.example.flowershop.repository.ShopRepository;
import com.example.flowershop.repository.UserRepository;
import com.example.flowershop.service.ShopReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:shopreports;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.shop-follow-mail.enabled=false", "app.occasion-reminder.enabled=false",
        "spring.jpa.show-sql=false"
})
@Transactional
class ShopReportServiceTests {
    @MockitoBean JavaMailSender mail;
    @Autowired UserRepository users;
    @Autowired ShopRepository shops;
    @Autowired AddressRepository addresses;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired OrderRepository orders;
    @Autowired OrderDetailRepository orderDetails;
    @Autowired PaymentRepository payments;
    @Autowired RefundRepository refunds;
    @Autowired FavoriteShopRepository followers;
    @Autowired ShopReportService reports;
    @Autowired BlogRepository blogs;
    @Autowired BlogCommentRepository comments;
    @Autowired BlogInteractionRepository interactions;
    @Autowired EntityManager entityManager;

    @Test
    void reportsFollowersOrdersRevenueAndProductReturnsForMonth() {
        User owner = users.saveAndFlush(user(UserRole.SHOP, "Shop owner"));
        User customer = users.saveAndFlush(user(UserRole.CUSTOMER, "Customer"));
        Shop shop = shops.saveAndFlush(Shop.builder().id(id()).owner(owner).name("Report Flowers")
                .status(ShopStatus.ACTIVE).build());
        Address address = addresses.saveAndFlush(Address.builder().id(id()).user(customer)
                .addressLine("1 Report Street").build());
        Category category = categories.saveAndFlush(Category.builder().id(id()).name("Roses").build());
        Product product = products.saveAndFlush(Product.builder().id(id()).shop(shop).category(category)
                .name("Red roses").description("").price(new BigDecimal("100000")).stock(20).build());
        followers.saveAndFlush(FavoriteShop.builder().id(new FavoriteShopId(customer.getId(), shop.getId()))
                .user(customer).shop(shop).createdBy(customer.getId()).build());

        Order processing = order(shop, customer, address, OrderStatus.PROCESSING, "100000");
        Order delivering = order(shop, customer, address, OrderStatus.DELIVERING, "200000");
        Order completed = order(shop, customer, address, OrderStatus.COMPLETED, "500000");
        Order returned = order(shop, customer, address, OrderStatus.REFUNDED, "200000");
        orders.saveAllAndFlush(java.util.List.of(processing, delivering, completed, returned));
        orderDetails.saveAndFlush(detail(completed, product, 5));
        orderDetails.saveAndFlush(detail(returned, product, 2));

        payments.saveAndFlush(Payment.builder().id(id()).order(completed).paymentType(PaymentType.FULL)
                .paymentMethod(PaymentMethod.COD).amount(new BigDecimal("500000"))
                .status(PaymentStatus.SUCCESS).build());
        refunds.saveAndFlush(Refund.builder().id(id()).order(returned).amount(new BigDecimal("100000"))
                .reason("Returned delivery").status(RefundStatus.PROCESSED).build());

        String month = YearMonth.now(ZoneId.of("Asia/Ho_Chi_Minh")).toString();
        ShopReportService.ReportResult report = reports.report(shop.getId(), owner.getId(), "MONTH", month);

        assertThat(report.followers().current()).isEqualTo(1);
        assertThat(report.followers().newInPeriod()).isEqualTo(1);
        assertThat(report.orders()).isEqualTo(new ShopReportService.OrderMetrics(4, 1, 1, 1, 0, 1));
        assertThat(report.revenue().grossReceived()).isEqualByComparingTo("500000");
        assertThat(report.revenue().refundedAmount()).isEqualByComparingTo("100000");
        assertThat(report.revenue().netRevenue()).isEqualByComparingTo("400000");
        assertThat(report.products()).singleElement().satisfies(row -> {
            assertThat(row.productId()).isEqualTo(product.getId());
            assertThat(row.completedQuantity()).isEqualTo(5);
            assertThat(row.returnedQuantity()).isEqualTo(2);
            assertThat(row.quantityDifference()).isEqualTo(3);
            assertThat(row.completedOrderCount()).isEqualTo(1);
            assertThat(row.returnedOrderCount()).isEqualTo(1);
            assertThat(row.returnRate()).isEqualByComparingTo("28.57");
        });

        ZoneId businessZone = ZoneId.of("Asia/Ho_Chi_Minh");
        assertThat(reports.report(shop.getId(), owner.getId(), "DAY",
                LocalDate.now(businessZone).toString()).orders().total()).isEqualTo(4);
        assertThat(reports.report(shop.getId(), owner.getId(), "YEAR",
                Year.now(businessZone).toString()).orders().total()).isEqualTo(4);
    }

    private Order order(Shop shop, User customer, Address address, OrderStatus status, String amount) {
        BigDecimal total = new BigDecimal(amount);
        return Order.builder().id(id()).shop(shop).customer(customer).deliveryAddress(address)
                .subTotal(total).totalAmount(total).status(status).build();
    }

    @Test
    void reportsUniqueCommentersAndRanksPostsByEngagementWithinSelectedPeriodAndShop() {
        User owner = users.saveAndFlush(user(UserRole.SHOP, "Owner"));
        User otherOwner = users.saveAndFlush(user(UserRole.SHOP, "Other shop owner"));
        User alice = users.saveAndFlush(user(UserRole.CUSTOMER, "Alice"));
        User bob = users.saveAndFlush(user(UserRole.CUSTOMER, "Bob"));
        Shop shop = shops.saveAndFlush(Shop.builder().id(id()).owner(owner).name("Flowers")
                .status(ShopStatus.ACTIVE).build());
        Shop otherShop = shops.saveAndFlush(Shop.builder().id(id()).owner(otherOwner).name("Other Flowers")
                .status(ShopStatus.ACTIVE).build());
        LocalDateTime start = LocalDate.of(2026, 5, 1).atStartOfDay();
        LocalDateTime end = start.plusMonths(1);
        Blog first = blog(shop, "Rose care", BlogStatus.HIDDEN, start.minusYears(1));
        Blog second = blog(shop, "Lily care", BlogStatus.PUBLISHED, start.minusYears(1));
        Blog savedOnly = blog(shop, "Tulip care", BlogStatus.PUBLISHED, start.minusYears(1));
        blog(shop, "No engagement", BlogStatus.PUBLISHED, start);
        Blog foreign = blog(otherShop, "Other shop post", BlogStatus.PUBLISHED, start);

        comment(first, alice, start);
        comment(first, alice, start.plusHours(1));
        comment(first, bob, end.minusSeconds(1));
        comment(second, alice, start.plusDays(2));
        comment(first, bob, start.minusSeconds(1));
        comment(second, bob, end);
        comment(foreign, bob, start);
        interaction(first, alice, InteractionType.LIKE, start);
        interaction(first, bob, InteractionType.LIKE, start);
        interaction(first, alice, InteractionType.SAVE, start);
        interaction(second, alice, InteractionType.LIKE, start);
        interaction(second, alice, InteractionType.SAVE, start);
        interaction(savedOnly, bob, InteractionType.SAVE, start);
        interaction(savedOnly, alice, InteractionType.LIKE, start.minusSeconds(1));
        interaction(savedOnly, alice, InteractionType.SAVE, end);
        interaction(foreign, bob, InteractionType.LIKE, start);

        var engagement = reports.report(shop.getId(), owner.getId(), "MONTH", "2026-05").blogEngagement();
        assertThat(engagement.commenters()).isEqualTo(2);
        assertThat(engagement.comments()).isEqualTo(4);
        assertThat(engagement.likes()).isEqualTo(3);
        assertThat(engagement.saves()).isEqualTo(3);
        assertThat(engagement.totalInteractions()).isEqualTo(10);
        assertThat(engagement.posts()).containsExactly(
                new ShopReportService.BlogPerformance(first.getId(), first.getTitle(), BlogStatus.HIDDEN, 2, 3, 2, 1, 6),
                new ShopReportService.BlogPerformance(second.getId(), second.getTitle(), BlogStatus.PUBLISHED, 1, 1, 1, 1, 3),
                new ShopReportService.BlogPerformance(savedOnly.getId(), savedOnly.getTitle(), BlogStatus.PUBLISHED, 0, 0, 0, 1, 1));

        var day = reports.report(shop.getId(), owner.getId(), "DAY", "2026-05-31").blogEngagement();
        assertThat(day.commenters()).isEqualTo(1);
        assertThat(day.totalInteractions()).isEqualTo(1);
        assertThat(day.posts()).singleElement().satisfies(post -> assertThat(post.blogId()).isEqualTo(first.getId()));
        assertThat(reports.report(shop.getId(), owner.getId(), "YEAR", "2026")
                .blogEngagement().totalInteractions()).isEqualTo(14);
        assertThat(reports.report(shop.getId(), owner.getId(), "MONTH", "2024-01").blogEngagement())
                .isEqualTo(new ShopReportService.BlogEngagementMetrics(0, 0, 0, 0, 0, java.util.List.of()));
    }

    @Test
    void rejectsReportsForAnotherShopOwner() {
        User owner = users.saveAndFlush(user(UserRole.SHOP, "Owner"));
        User outsider = users.saveAndFlush(user(UserRole.SHOP, "Other owner"));
        Shop shop = shops.saveAndFlush(Shop.builder().id(id()).owner(owner).name("Private Flowers")
                .status(ShopStatus.ACTIVE).build());
        assertThatThrownBy(() -> reports.report(shop.getId(), outsider.getId(), "MONTH", "2026-05"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode().value()).isEqualTo(403));
    }

    private Blog blog(Shop shop, String title, BlogStatus status, LocalDateTime createdDate) {
        Blog blog = blogs.saveAndFlush(Blog.builder().id(id()).shop(shop).title(title).content(title).status(status).build());
        entityManager.createNativeQuery("update Blogs set created_date = :date where id = :id")
                .setParameter("date", createdDate).setParameter("id", blog.getId()).executeUpdate();
        return blog;
    }

    private void comment(Blog blog, User user, LocalDateTime createdDate) {
        BlogComment comment = comments.saveAndFlush(BlogComment.builder().id(id()).blog(blog).user(user).content("Helpful").build());
        entityManager.createNativeQuery("update Blog_Comments set created_date = :date where id = :id")
                .setParameter("date", createdDate).setParameter("id", comment.getId()).executeUpdate();
    }

    private void interaction(Blog blog, User user, InteractionType type, LocalDateTime createdDate) {
        interactions.saveAndFlush(BlogInteraction.builder().id(new BlogInteractionId(user.getId(), blog.getId(), type))
                .blog(blog).user(user).build());
        entityManager.createNativeQuery("update Blog_Interactions set created_date = :date "
                        + "where blog_id = :blogId and user_id = :userId and interaction_type = :type")
                .setParameter("date", createdDate).setParameter("blogId", blog.getId())
                .setParameter("userId", user.getId()).setParameter("type", type.name()).executeUpdate();
    }

    private OrderDetail detail(Order order, Product product, int quantity) {
        return OrderDetail.builder().id(id()).order(order).product(product)
                .price(product.getPrice()).quantity(quantity).build();
    }

    private User user(UserRole role, String name) {
        return User.builder().id(id()).email(id() + "@example.test").fullName(name)
                .role(role).status(UserStatus.ACTIVE).isEmailVerified(true).build();
    }

    private String id() {
        return UUID.randomUUID().toString();
    }
}
