package com.example.flowershop;

import com.example.flowershop.entity.Address;
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
import com.example.flowershop.entity.enums.PaymentMethod;
import com.example.flowershop.entity.enums.PaymentStatus;
import com.example.flowershop.entity.enums.PaymentType;
import com.example.flowershop.entity.enums.RefundStatus;
import com.example.flowershop.entity.enums.ShopStatus;
import com.example.flowershop.entity.enums.UserRole;
import com.example.flowershop.entity.enums.UserStatus;
import com.example.flowershop.repository.AddressRepository;
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

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

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
