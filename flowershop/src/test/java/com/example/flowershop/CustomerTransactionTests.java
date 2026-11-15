package com.example.flowershop;

import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.*;
import com.example.flowershop.service.CustomerTransactionService;
import com.example.flowershop.service.NotificationService;
import com.example.flowershop.service.TokenAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** UC22 19.2: lịch sử giao dịch của khách, chỉ của chính khách, có lọc, phân trang, tổng và dòng hoàn tiền. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:custtx;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.shop-follow-mail.enabled=false", "app.occasion-reminder.enabled=false"
})
@Transactional
class CustomerTransactionTests {
    @MockitoBean JavaMailSender mail;
    @MockitoBean TokenAuthService tokenAuth;
    @MockitoBean NotificationService notifications;
    @Autowired CustomerTransactionService service;
    @Autowired UserRepository users;
    @Autowired ShopRepository shops;
    @Autowired AddressRepository addresses;
    @Autowired OrderRepository orders;
    @Autowired PaymentRepository payments;
    @Autowired RefundRepository refunds;
    @Autowired JdbcTemplate jdbc;

    User customer, other;
    Shop shop;
    Order paidOrder, partialOrder, otherOrder;

    String id() { return UUID.randomUUID().toString(); }

    User user(UserRole role) {
        return users.saveAndFlush(User.builder().id(id()).email(id() + "@example.test").fullName("Khách").role(role)
                .status(UserStatus.ACTIVE).isEmailVerified(true).build());
    }

    Order order(User who, String total, OrderStatus st) {
        Address address = addresses.saveAndFlush(Address.builder().id(id()).user(who).addressLine("1 Lê Lợi")
                .city("Hà Nội").district("Hoàn Kiếm").ward("Hàng Bạc").build());
        return orders.saveAndFlush(Order.builder().id(id()).customer(who).shop(shop).deliveryAddress(address)
                .subTotal(new BigDecimal(total)).totalAmount(new BigDecimal(total)).shippingFee(BigDecimal.ZERO)
                .status(st).build());
    }

    Payment pay(Order o, String amount, PaymentStatus st, PaymentType type) {
        return payments.saveAndFlush(Payment.builder().id(id()).order(o).paymentType(type).paymentMethod(PaymentMethod.ONLINE)
                .amount(new BigDecimal(amount)).status(st).invoiceNumber("INV-" + id()).build());
    }

    @BeforeEach
    void fixture() {
        customer = user(UserRole.CUSTOMER);
        other = user(UserRole.CUSTOMER);
        User owner = user(UserRole.SHOP);
        shop = shops.saveAndFlush(Shop.builder().id(id()).owner(owner).name("Hoa Xuân").status(ShopStatus.ACTIVE).build());
        paidOrder = order(customer, "300000", OrderStatus.PROCESSING);
        partialOrder = order(customer, "500000", OrderStatus.DEPOSIT_PAID);
        otherOrder = order(other, "999000", OrderStatus.PROCESSING);
    }

    @Test
    void customerSeesOnlyOwnTransactionsWithTotalsAndInvoiceFlag() {
        pay(paidOrder, "300000", PaymentStatus.SUCCESS, PaymentType.FULL);
        pay(partialOrder, "150000", PaymentStatus.SUCCESS, PaymentType.DEPOSIT);
        pay(partialOrder, "350000", PaymentStatus.FAILED, PaymentType.FINAL);
        pay(otherOrder, "999000", PaymentStatus.SUCCESS, PaymentType.FULL);

        var result = service.list(customer.getId(), 0, null, null, null);

        assertThat(result.totalElements()).isEqualTo(3);
        assertThat(result.content()).allMatch(l -> !l.orderId().equals(otherOrder.getId()));
        assertThat(result.totalPaid()).isEqualByComparingTo("450000");
        var full = result.content().stream().filter(l -> l.orderId().equals(paidOrder.getId())).findFirst().orElseThrow();
        assertThat(full.invoiceAvailable()).isTrue();
        assertThat(full.shopName()).isEqualTo("Hoa Xuân");
        assertThat(result.content().stream().filter(l -> l.orderId().equals(partialOrder.getId())))
                .allMatch(l -> !l.invoiceAvailable());
        assertThat(service.list(other.getId(), 0, null, null, null).totalPaid()).isEqualByComparingTo("999000");
    }

    @Test
    void statusFilterNarrowsRowsButNotTotalsAndHidesRefundRows() {
        pay(paidOrder, "300000", PaymentStatus.SUCCESS, PaymentType.FULL);
        pay(partialOrder, "350000", PaymentStatus.FAILED, PaymentType.FINAL);

        var failed = service.list(customer.getId(), 0, "failed", null, null);

        assertThat(failed.content()).hasSize(1).allMatch(l -> l.status().equals("FAILED"));
        assertThat(failed.totalPaid()).isEqualByComparingTo("300000");
        assertThat(failed.refunds()).isEmpty();
        assertThatThrownBy(() -> service.list(customer.getId(), 0, "XYZ", null, null))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void dateRangeFiltersPaymentsAndTotals() {
        Payment old = pay(paidOrder, "100000", PaymentStatus.SUCCESS, PaymentType.DEPOSIT);
        pay(paidOrder, "200000", PaymentStatus.SUCCESS, PaymentType.FINAL);
        jdbc.update("UPDATE Payments SET created_date = ? WHERE id = ?", java.sql.Timestamp.valueOf("2024-01-15 10:00:00"), old.getId());

        var recent = service.list(customer.getId(), 0, null, LocalDate.of(2025, 1, 1), null);
        var january = service.list(customer.getId(), 0, null, LocalDate.of(2024, 1, 15), LocalDate.of(2024, 1, 15));

        assertThat(recent.totalElements()).isEqualTo(1);
        assertThat(recent.totalPaid()).isEqualByComparingTo("200000");
        assertThat(january.totalElements()).isEqualTo(1);
        assertThat(january.content().get(0).amount()).isEqualByComparingTo("100000");
        assertThatThrownBy(() -> service.list(customer.getId(), 0, null, LocalDate.of(2025, 2, 1), LocalDate.of(2025, 1, 1)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void paginationOrdersNewestFirst() {
        for (int i = 0; i < 25; i++) {
            Payment p = pay(paidOrder, "1000", PaymentStatus.SUCCESS, PaymentType.FULL);
            jdbc.update("UPDATE Payments SET created_date = ? WHERE id = ?",
                    new java.sql.Timestamp(java.sql.Timestamp.valueOf("2025-03-01 10:00:00").getTime() + i * 1000L), p.getId());
        }

        var first = service.list(customer.getId(), 0, null, null, null);
        var second = service.list(customer.getId(), 1, null, null, null);

        assertThat(first.content()).hasSize(20);
        assertThat(second.content()).hasSize(5);
        assertThat(first.totalPages()).isEqualTo(2);
        assertThat(first.content().get(0).createdDate()).isAfterOrEqualTo(first.content().get(19).createdDate());
        assertThat(first.content().get(19).createdDate()).isAfterOrEqualTo(second.content().get(0).createdDate());
    }

    @Test
    void refundsAreListedForOwnerAndProcessedOnesAreSummed() {
        refunds.saveAndFlush(Refund.builder().id(id()).order(paidOrder).amount(new BigDecimal("300000")).reason("Hoa héo")
                .status(RefundStatus.PROCESSED).build());
        refunds.saveAndFlush(Refund.builder().id(id()).order(partialOrder).amount(new BigDecimal("50000")).reason("Giao trễ")
                .status(RefundStatus.REQUESTED).build());
        refunds.saveAndFlush(Refund.builder().id(id()).order(partialOrder).amount(new BigDecimal("70000")).reason("Hủy yêu cầu")
                .status(RefundStatus.CANCELLED).build());
        refunds.saveAndFlush(Refund.builder().id(id()).order(otherOrder).amount(new BigDecimal("999000")).reason("Khác")
                .status(RefundStatus.PROCESSED).build());

        var result = service.list(customer.getId(), 0, null, null, null);

        assertThat(result.refunds()).hasSize(2).allMatch(r -> !r.orderId().equals(otherOrder.getId()));
        assertThat(result.totalRefunded()).isEqualByComparingTo("300000");
    }
}
