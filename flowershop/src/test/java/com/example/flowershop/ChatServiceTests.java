package com.example.flowershop;

import com.example.flowershop.chat.ChatDtos.ContextInput;
import com.example.flowershop.chat.ChatDtos.SendInput;
import com.example.flowershop.chat.ChatService;
import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:chat;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.shop-follow-mail.enabled=false", "app.occasion-reminder.enabled=false"
})
@Transactional
class ChatServiceTests {
    @MockitoBean JavaMailSender sender;
    @Autowired ChatService chat;
    @Autowired UserRepository users;
    @Autowired ShopRepository shops;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired CustomOrderRequestRepository customOrders;
    @Autowired OrderRepository orders;
    @Autowired OrderDetailRepository orderDetails;
    @Autowired ChatSessionRepository sessions;
    @Autowired ChatMessageRepository messages;
    @Autowired NotificationRepository notifications;
    @Autowired CustomerOccasionRepository occasions;
    @Autowired jakarta.persistence.EntityManager entityManager;
    @Autowired AddressRepository addresses;

    record Fixture(User owner, User customer, Shop shop, Product product) {}

    private User user(UserRole role) {
        return users.saveAndFlush(User.builder().id(UUID.randomUUID().toString()).email(UUID.randomUUID() + "@example.test")
                .fullName("User " + role).role(role).status(UserStatus.ACTIVE).isEmailVerified(true).build());
    }

    private Fixture fixture() {
        User owner = user(UserRole.SHOP);
        User customer = user(UserRole.CUSTOMER);
        Shop shop = shops.saveAndFlush(Shop.builder().id(UUID.randomUUID().toString()).owner(owner).name("Chat flowers")
                .status(ShopStatus.ACTIVE).build());
        Category category = categories.saveAndFlush(Category.builder().id(UUID.randomUUID().toString()).name("C")
                .status(CategoryStatus.ACTIVE).build());
        Product product = products.saveAndFlush(Product.builder().id(UUID.randomUUID().toString()).shop(shop).category(category)
                .name("Bó hồng").price(new BigDecimal("250000")).stock(5).status(ProductStatus.ACTIVE)
                .type(ProductType.READY_MADE).build());
        return new Fixture(owner, customer, shop, product);
    }

    private static SendInput text(String message) {
        return new SendInput(message, null, null);
    }

    @Test
    void historyBindsSessionAndDateAndHonorsPaging() {
        var f=fixture();
        var session=sessions.saveAndFlush(ChatSession.builder().id(UUID.randomUUID().toString()).customer(f.customer()).shop(f.shop()).build());
        messages.saveAllAndFlush(List.of(
                ChatMessage.builder().id(UUID.randomUUID().toString()).session(session).sender(f.customer()).senderType(SenderType.CUSTOMER).message("First").build(),
                ChatMessage.builder().id(UUID.randomUUID().toString()).session(session).sender(f.owner()).senderType(SenderType.SHOP).message("Second").build()));
        assertThat(messages.latest(session.getId(),org.springframework.data.domain.PageRequest.of(0,10))).hasSize(2);
        assertThat(messages.olderThan(session.getId(),java.time.LocalDateTime.now().plusDays(1),org.springframework.data.domain.PageRequest.of(0,1))).hasSize(1);
        assertThat(messages.olderThan(session.getId(),java.time.LocalDateTime.now().minusDays(1),org.springframework.data.domain.PageRequest.of(0,10))).isEmpty();
    }

    @Test
    void markingNotificationsReadOnlyChangesTheSelectedUsersRows() {
        var customer=user(UserRole.CUSTOMER); var other=user(UserRole.CUSTOMER);
        var own=notifications.saveAndFlush(Notification.builder().id(UUID.randomUUID().toString()).user(customer).title("Own").build());
        var unrelated=notifications.saveAndFlush(Notification.builder().id(UUID.randomUUID().toString()).user(other).title("Other").build());
        notifications.markAllAsRead(customer.getId());
        entityManager.clear();
        assertThat(notifications.findById(own.getId()).orElseThrow().isRead()).isTrue();
        assertThat(notifications.findById(unrelated.getId()).orElseThrow().isRead()).isFalse();
    }

    @Test
    void reminderCandidatesBindDateAndExcludePastOrAlreadyRemindedOneOffEvents() {
        var customer=user(UserRole.CUSTOMER); var today=java.time.LocalDate.now();
        var recurring=occasions.saveAndFlush(CustomerOccasion.builder().id(UUID.randomUUID().toString()).customer(customer).title("Recurring").occasionDate(today.minusYears(1)).recurringYearly(true).reminderSent(true).build());
        var future=occasions.saveAndFlush(CustomerOccasion.builder().id(UUID.randomUUID().toString()).customer(customer).title("Future").occasionDate(today.plusDays(2)).build());
        occasions.saveAndFlush(CustomerOccasion.builder().id(UUID.randomUUID().toString()).customer(customer).title("Past").occasionDate(today.minusDays(2)).build());
        occasions.saveAndFlush(CustomerOccasion.builder().id(UUID.randomUUID().toString()).customer(customer).title("Already reminded").occasionDate(today.plusDays(2)).reminderSent(true).build());
        assertThat(occasions.findReminderCandidates(today)).extracting(CustomerOccasion::getId).containsExactlyInAnyOrder(recurring.getId(),future.getId());
    }

    @Test
    void openingTwiceReturnsTheSameSessionPerCustomerAndShop() {
        var f = fixture();
        var first = chat.openForCustomer(f.customer().getId(), f.shop().getId());
        var second = chat.openForCustomer(f.customer().getId(), f.shop().getId());
        assertThat(second.id()).isEqualTo(first.id());
    }

    @Test
    void sharedCustomOrdersOpenChatAndLinkToTheirOrderWithoutLeakingAcrossCustomersOrShops() {
        var f = fixture();
        var order = orders.saveAndFlush(Order.builder().id(UUID.randomUUID().toString())
                .customer(f.customer()).shop(f.shop()).orderType(OrderType.CUSTOM)
                .status(OrderStatus.AWAITING_QUOTE).subTotal(BigDecimal.ZERO).totalAmount(BigDecimal.ZERO).build());
        orderDetails.saveAndFlush(OrderDetail.builder().id(UUID.randomUUID().toString()).order(order)
                .customMaterials("{\"budget\":500000,\"description\":\"Birthday\",\"preferences\":{\"quantity\":2}}")
                .price(BigDecimal.ZERO).quantity(1).build());
        var session = chat.openForShop(f.shop().getId(), f.owner().getId(), order.getId());
        var sent = chat.send(session.id(), f.owner().getId(),
                new SendInput("Design quote", null, new ContextInput(ChatContextType.CUSTOM_ORDER, order.getId())));
        assertThat(sent.context()).containsEntry("orderId", order.getId()).containsEntry("status", "AWAITING_QUOTE");
        assertThat(sent.context().get("price")).isNull();
        order.setSubTotal(new BigDecimal("650000"));
        order.setStatus(OrderStatus.QUOTED);
        orders.saveAndFlush(order);
        var quoted = chat.send(session.id(), f.owner().getId(),
                new SendInput("Shop quote", null, new ContextInput(ChatContextType.CUSTOM_ORDER, order.getId())));
        assertThat(new BigDecimal(quoted.context().get("price").toString())).isEqualByComparingTo("650000");
        var other = fixture();
        assertThatThrownBy(() -> chat.openForShop(other.shop().getId(), other.owner().getId(), order.getId()))
                .isInstanceOf(ResponseStatusException.class);
        var otherSession = chat.openForCustomer(other.customer().getId(), f.shop().getId());
        assertThatThrownBy(() -> chat.send(otherSession.id(), other.customer().getId(),
                new SendInput("Wrong customer", null, new ContextInput(ChatContextType.CUSTOM_ORDER, order.getId()))))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void messagesTrackUnreadOnEachSideAndMarkReadClearsIt() {
        var f = fixture();
        var session = chat.openForCustomer(f.customer().getId(), f.shop().getId());

        chat.send(session.id(), f.customer().getId(), text("Xin chào"));
        chat.send(session.id(), f.customer().getId(), text("Còn hàng không?"));
        assertThat(chat.shopSessions(f.shop().getId(), f.owner().getId()).get(0).unread()).isEqualTo(2);
        assertThat(chat.shopUnread(f.shop().getId(), f.owner().getId())).isEqualTo(2);

        chat.markRead(session.id(), f.owner().getId());
        assertThat(chat.shopUnread(f.shop().getId(), f.owner().getId())).isZero();

        chat.send(session.id(), f.owner().getId(), text("Còn nhé"));
        assertThat(chat.customerUnread(f.customer().getId())).isEqualTo(1);
        var history = chat.history(session.id(), f.customer().getId(), null, 30);
        assertThat(history).extracting(m -> m.message()).containsExactly("Xin chào", "Còn hàng không?", "Còn nhé");
        assertThat(history.get(0).read()).isTrue();
    }

    @Test
    void strangersCannotReadSendOrSubscribe() {
        var f = fixture();
        var session = chat.openForCustomer(f.customer().getId(), f.shop().getId());
        User stranger = user(UserRole.CUSTOMER);
        User otherOwner = user(UserRole.SHOP);

        assertThatThrownBy(() -> chat.history(session.id(), stranger.getId(), null, 30)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> chat.send(session.id(), otherOwner.getId(), text("hi"))).isInstanceOf(ResponseStatusException.class);
        assertThat(chat.canAccess(session.id(), stranger.getId())).isFalse();
        assertThat(chat.canAccess(session.id(), f.owner().getId())).isTrue();
        assertThatThrownBy(() -> chat.shopSessions(f.shop().getId(), otherOwner.getId())).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void productCardMustBelongToTheShopAndIsSnapshotted() {
        var f = fixture();
        var other = fixture();
        var session = chat.openForCustomer(f.customer().getId(), f.shop().getId());

        var sent = chat.send(session.id(), f.customer().getId(),
                new SendInput("Hỏi về bó này", null, new ContextInput(ChatContextType.PRODUCT, f.product().getId())));
        assertThat(sent.contextType()).isEqualTo(ChatContextType.PRODUCT);
        assertThat(sent.context()).containsEntry("name", "Bó hồng");

        assertThatThrownBy(() -> chat.send(session.id(), f.customer().getId(),
                new SendInput("x", null, new ContextInput(ChatContextType.PRODUCT, other.product().getId()))))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void customOrderCardMustBelongToThisCustomerAndShopAndShopCanOpenFromIt() {
        var f = fixture();
        var request = customOrders.saveAndFlush(CustomOrderRequest.builder().id(UUID.randomUUID().toString())
                .customer(f.customer()).shop(f.shop()).budget(new BigDecimal("500000")).description("Hoa sinh nhật").build());

        var session = chat.openForShop(f.shop().getId(), f.owner().getId(), request.getId());
        var sent = chat.send(session.id(), f.owner().getId(),
                new SendInput("Mình tư vấn thêm nhé", null, new ContextInput(ChatContextType.CUSTOM_ORDER, request.getId())));
        assertThat(sent.context().get("name").toString()).startsWith("Yêu cầu hoa custom #");

        User otherCustomer = user(UserRole.CUSTOMER);
        var otherSession = chat.openForCustomer(otherCustomer.getId(), f.shop().getId());
        assertThatThrownBy(() -> chat.send(otherSession.id(), otherCustomer.getId(),
                new SendInput("x", null, new ContextInput(ChatContextType.CUSTOM_ORDER, request.getId()))))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shopOpensChatWithCustomerOfItsOwnOrderOnly() {
        var f = fixture();
        var address = addresses.saveAndFlush(Address.builder().id(UUID.randomUUID().toString()).user(f.customer())
                .addressLine("1 Phố Huế").build());
        var order = orders.saveAndFlush(Order.builder().id(UUID.randomUUID().toString()).customer(f.customer()).shop(f.shop())
                .deliveryAddress(address).subTotal(new BigDecimal("250000")).totalAmount(new BigDecimal("250000")).build());

        var session = chat.openForShopByOrder(f.shop().getId(), f.owner().getId(), order.getId());
        assertThat(session.customerId()).isEqualTo(f.customer().getId());
        assertThat(chat.openForCustomer(f.customer().getId(), f.shop().getId()).id()).isEqualTo(session.id());

        var other = fixture();
        assertThatThrownBy(() -> chat.openForShopByOrder(other.shop().getId(), other.owner().getId(), order.getId()))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void emptyMessageAndNonHttpsImagesAreRejected() {
        var f = fixture();
        var session = chat.openForCustomer(f.customer().getId(), f.shop().getId());
        assertThatThrownBy(() -> chat.send(session.id(), f.customer().getId(), text("   "))).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> chat.send(session.id(), f.customer().getId(),
                new SendInput(null, List.of("javascript:alert(1)"), null))).isInstanceOf(ResponseStatusException.class);
    }
}
