package com.example.flowershop;

import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.*;
import com.example.flowershop.service.CustomFlowerOrderService;
import com.example.flowershop.service.CustomFlowerOrderService.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Shop báo giá kèm % cọc, khách từ chối, quyền truy cập và không báo giá lại sau khi khách đã chấp nhận. */
@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:customquote;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=", "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop", "app.shop-follow-mail.enabled=false", "spring.jpa.show-sql=false"})
@Transactional
class CustomQuoteTests {
    @MockitoBean JavaMailSender mail;
    @MockitoBean com.example.flowershop.service.TokenAuthService tokenAuth;
    @Autowired CustomFlowerOrderService service;
    @Autowired UserRepository users;
    @Autowired ShopRepository shops;
    @Autowired ShopStaffRepository staffRepo;
    @Autowired CustomOrderRequestRepository requests;
    @Autowired NotificationRepository notifications;
    User owner, customer; Shop shop; String requestId;

    String id() { return UUID.randomUUID().toString(); }
    User user(UserRole role) {
        return users.saveAndFlush(User.builder().id(id()).email(id() + "@example.test").fullName("Test user").role(role)
                .status(UserStatus.ACTIVE).isEmailVerified(true).build());
    }
    Quote quote(String price, Integer percent) { return new Quote(new BigDecimal(price), percent, "Hoa hồng pastel", null); }
    static void assertStatus(Throwable e, int code) {
        assertThat(e).isInstanceOfSatisfying(ResponseStatusException.class, x -> assertThat(x.getStatusCode().value()).isEqualTo(code));
    }

    @BeforeEach void fixture() {
        owner = user(UserRole.SHOP); customer = user(UserRole.CUSTOMER);
        shop = shops.saveAndFlush(Shop.builder().id(id()).owner(owner).name("Quote shop").status(ShopStatus.ACTIVE).build());
        requestId = service.create(customer.getId(), new RequestInput(shop.getId(), new BigDecimal("500000"), 1, "Bó hoa sinh nhật",
                new Preferences("Hồng", "Hồng", "Sinh nhật", null, null, null))).id();
    }

    @Test void quoteComputesDepositAndNotifiesCustomer() {
        var r = service.quote(shop.getId(), owner.getId(), requestId, quote("400000", 40));
        assertThat(r.status()).isEqualTo(CustomOrderStatus.QUOTED);
        assertThat(r.quotedPrice()).isEqualByComparingTo("400000");
        assertThat(r.depositPercent()).isEqualTo(40);
        assertThat(r.depositAmount()).isEqualByComparingTo("160000");
        assertThat(r.remainingAmount()).isEqualByComparingTo("240000");
        assertThat(r.design().shopNote()).isEqualTo("Hoa hồng pastel");
        assertThat(notifications.findByUserIdOrderByCreatedDateDesc(customer.getId()))
                .anyMatch(n -> n.getTitle().contains("báo giá") && n.getLink().equals("/custom-orders?open=" + requestId));
    }

    @Test void depositPercentDefaultsTo30AndIsRangeChecked() {
        assertThat(service.quote(shop.getId(), owner.getId(), requestId, quote("333333", null)).depositAmount()).isEqualByComparingTo("100000");
        assertThatThrownBy(() -> service.quote(shop.getId(), owner.getId(), requestId, quote("400000", 9))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.quote(shop.getId(), owner.getId(), requestId, quote("400000", 101))).isInstanceOf(IllegalArgumentException.class);
        assertThat(service.quote(shop.getId(), owner.getId(), requestId, quote("400000", 100)).depositAmount()).isEqualByComparingTo("400000");
    }

    @Test void invalidPriceAndExpiryAreRejected() {
        assertThatThrownBy(() -> service.quote(shop.getId(), owner.getId(), requestId, quote("0", 30))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.quote(shop.getId(), owner.getId(), requestId, quote("500001", 30))).isInstanceOf(IllegalArgumentException.class);
        var past = new Quote(new BigDecimal("400000"), 30, null, LocalDateTime.now().minusDays(1));
        assertThatThrownBy(() -> service.quote(shop.getId(), owner.getId(), requestId, past)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void shopCanRequoteWhileQuoted() {
        service.quote(shop.getId(), owner.getId(), requestId, quote("400000", 30));
        var again = service.quote(shop.getId(), owner.getId(), requestId, quote("350000", 50));
        assertThat(again.quotedPrice()).isEqualByComparingTo("350000");
        assertThat(again.depositAmount()).isEqualByComparingTo("175000");
    }

    @Test void activeStaffMayQuoteButStrangersGet403() {
        var staffUser = user(UserRole.SHOP_STAFF);
        var membership = new ShopStaff(); membership.setId(id()); membership.setShop(shop); membership.setUser(staffUser); membership.setActive(true);
        staffRepo.saveAndFlush(membership);
        assertThat(service.quote(shop.getId(), staffUser.getId(), requestId, quote("400000", 30)).status()).isEqualTo(CustomOrderStatus.QUOTED);

        var strangerShopOwner = user(UserRole.SHOP);
        var strangerStaff = user(UserRole.SHOP_STAFF);
        assertThatThrownBy(() -> service.quote(shop.getId(), strangerShopOwner.getId(), requestId, quote("400000", 30))).satisfies(e -> assertStatus(e, 403));
        assertThatThrownBy(() -> service.quote(shop.getId(), strangerStaff.getId(), requestId, quote("400000", 30))).satisfies(e -> assertStatus(e, 403));
        assertThatThrownBy(() -> service.quote(shop.getId(), customer.getId(), requestId, quote("400000", 30))).satisfies(e -> assertStatus(e, 403));
    }

    @Test void requestOfAnotherShopIsNotFound() {
        var otherOwner = user(UserRole.SHOP);
        var other = shops.saveAndFlush(Shop.builder().id(id()).owner(otherOwner).name("Other").status(ShopStatus.ACTIVE).build());
        assertThatThrownBy(() -> service.quote(other.getId(), otherOwner.getId(), requestId, quote("400000", 30))).satisfies(e -> assertStatus(e, 404));
    }

    @Test void cannotRequoteOrRejectAfterCustomerAccepted() {
        service.quote(shop.getId(), owner.getId(), requestId, quote("400000", 30));
        var request = requests.findById(requestId).orElseThrow();
        request.setStatus(CustomOrderStatus.ACCEPTED); requests.saveAndFlush(request);
        assertThatThrownBy(() -> service.quote(shop.getId(), owner.getId(), requestId, quote("300000", 30))).satisfies(e -> assertStatus(e, 409));
        assertThatThrownBy(() -> service.reject(shop.getId(), owner.getId(), requestId, new Reason("Hết hoa"))).isInstanceOf(ResponseStatusException.class);
        assertThat(requests.findById(requestId).orElseThrow().getQuotedPrice()).isEqualByComparingTo("400000");
    }

    @Test void shopRejectNeedsReasonAndNotifiesCustomer() {
        assertThatThrownBy(() -> service.reject(shop.getId(), owner.getId(), requestId, new Reason("  "))).isInstanceOf(IllegalArgumentException.class);
        var r = service.reject(shop.getId(), owner.getId(), requestId, new Reason("Hết hoa"));
        assertThat(r.status()).isEqualTo(CustomOrderStatus.REJECTED);
        assertThat(r.rejectReason()).isEqualTo("Hết hoa");
        assertThat(r.rejectedBy()).isEqualTo("SHOP");
        assertThat(notifications.findByUserIdOrderByCreatedDateDesc(customer.getId())).anyMatch(n -> n.getTitle().contains("từ chối"));
        assertThatThrownBy(() -> service.quote(shop.getId(), owner.getId(), requestId, quote("400000", 30))).isInstanceOf(ResponseStatusException.class);
    }

    @Test void customerRejectsQuoteAndShopIsNotified() {
        assertThatThrownBy(() -> service.customerReject(customer.getId(), requestId, new Reason("Đắt"))).isInstanceOf(ResponseStatusException.class);
        service.quote(shop.getId(), owner.getId(), requestId, quote("400000", 30));
        var stranger = user(UserRole.CUSTOMER);
        assertThatThrownBy(() -> service.customerReject(stranger.getId(), requestId, new Reason("x"))).satisfies(e -> assertStatus(e, 404));
        var r = service.customerReject(customer.getId(), requestId, new Reason("Đắt quá"));
        assertThat(r.status()).isEqualTo(CustomOrderStatus.REJECTED);
        assertThat(r.rejectedBy()).isEqualTo("CUSTOMER");
        assertThat(notifications.findByUserIdOrderByCreatedDateDesc(owner.getId()))
                .anyMatch(n -> n.getTitle().contains("từ chối") && n.getMessage().contains("Đắt quá"));
    }

    @Test void legacyAcceptDecisionIsNoLongerAvailableToShop() {
        assertThatThrownBy(() -> service.decide(shop.getId(), owner.getId(), requestId,
                new Decision(CustomOrderStatus.ACCEPTED, "ok", new BigDecimal("400000")))).isInstanceOf(IllegalArgumentException.class);
        var reply = service.decide(shop.getId(), owner.getId(), requestId, new Decision(CustomOrderStatus.NEGOTIATING, "Bạn muốn thêm hoa gì?", null));
        assertThat(reply.status()).isEqualTo(CustomOrderStatus.NEGOTIATING);
    }
}
