package com.example.flowershop;

import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.event.OrderStatusChangedEvent;
import com.example.flowershop.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/** Kiểm tra OrderStatusListener chạy thật trong Hibernate: đổi trạng thái đơn đã lưu thì phát event, tạo mới thì không. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:orderevents;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.shop-follow-mail.enabled=false", "app.occasion-reminder.enabled=false"
})
class OrderStatusEventTests {

    static class Captured {
        final List<OrderStatusChangedEvent> events = new CopyOnWriteArrayList<>();

        @EventListener
        void on(OrderStatusChangedEvent event) {
            events.add(event);
        }
    }

    @TestConfiguration
    static class Config {
        @Bean
        Captured captured() {
            return new Captured();
        }
    }

    @MockitoBean JavaMailSender sender;
    @Autowired Captured captured;
    @Autowired UserRepository users;
    @Autowired ShopRepository shops;
    @Autowired AddressRepository addresses;
    @Autowired OrderRepository orders;
    @Autowired PlatformTransactionManager transactions;

    @Test
    void statusChangeOfSavedOrderPublishesEventButCreationDoesNot() {
        var tx = new TransactionTemplate(transactions);
        String orderId = UUID.randomUUID().toString();
        tx.executeWithoutResult(s -> {
            var owner = users.saveAndFlush(user(UserRole.SHOP));
            var customer = users.saveAndFlush(user(UserRole.CUSTOMER));
            var shop = shops.saveAndFlush(Shop.builder().id(UUID.randomUUID().toString()).owner(owner)
                    .name("Event flowers").status(ShopStatus.ACTIVE).build());
            var address = addresses.saveAndFlush(Address.builder().id(UUID.randomUUID().toString()).user(customer)
                    .addressLine("1 Test street").build());
            orders.saveAndFlush(Order.builder().id(orderId).customer(customer).shop(shop).deliveryAddress(address)
                    .subTotal(new BigDecimal("100000")).totalAmount(new BigDecimal("130000")).build());
        });
        assertThat(captured.events).isEmpty();

        tx.executeWithoutResult(s -> {
            Order order = orders.findById(orderId).orElseThrow();
            order.setStatus(OrderStatus.PROCESSING);
            orders.saveAndFlush(order);
        });
        assertThat(captured.events).hasSize(1);
        assertThat(captured.events.get(0).orderId()).isEqualTo(orderId);
        assertThat(captured.events.get(0).from()).isEqualTo(OrderStatus.PENDING);
        assertThat(captured.events.get(0).to()).isEqualTo(OrderStatus.PROCESSING);

        tx.executeWithoutResult(s -> {
            Order order = orders.findById(orderId).orElseThrow();
            order.setShopNote("chỉ đổi ghi chú");
            orders.saveAndFlush(order);
        });
        assertThat(captured.events).hasSize(1);
    }

    private static User user(UserRole role) {
        return User.builder().id(UUID.randomUUID().toString()).email(UUID.randomUUID() + "@example.test")
                .fullName("Test user").role(role).status(UserStatus.ACTIVE).isEmailVerified(true).build();
    }
}
