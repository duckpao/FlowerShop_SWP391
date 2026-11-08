package com.example.flowershop;

import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.*;
import com.example.flowershop.service.CustomerOrderService;
import jakarta.persistence.EntityManager;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:hibernaterepositories;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=", "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop", "app.shop-follow-mail.enabled=false", "spring.jpa.show-sql=false"})
@Transactional
class HibernateRepositoryTests {
    @MockitoBean JavaMailSender mail;
    @Autowired UserRepository users;
    @Autowired ShopRepository shops;
    @Autowired AddressRepository addresses;
    @Autowired OrderRepository orders;
    @Autowired OrderDetailRepository details;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired ProductImageRepository images;
    @Autowired AuthSessionRepository authSessions;
    @Autowired CustomerOrderService customerOrders;
    @Autowired EntityManager entityManager;
    @Autowired PlatformTransactionManager transactions;

    String id() { return UUID.randomUUID().toString(); }
    User newUser(UserRole role) {
        return User.builder().id(id()).email(id()+"@example.test").fullName("Before update")
                .role(role).status(UserStatus.ACTIVE).isEmailVerified(true).build();
    }
    User user(UserRole role) { return users.saveAndFlush(newUser(role)); }
    Shop shop(String name, ShopStatus status) {
        return shops.saveAndFlush(Shop.builder().id(id()).owner(user(UserRole.SHOP)).name(name).status(status).build());
    }

    @Test void managedChangesSurviveFlushAndClearAndWriteLockIsApplied() {
        var user=user(UserRole.CUSTOMER);
        assertThat(entityManager.contains(user)).isTrue();
        user.setFullName("After update");
        users.flush();
        entityManager.clear();
        var loaded=users.findByIdForUpdate(user.getId()).orElseThrow();
        assertThat(loaded.getFullName()).isEqualTo("After update");
        assertThat(entityManager.unwrap(Session.class).getCurrentLockMode(loaded)).isEqualTo(LockMode.PESSIMISTIC_WRITE);
    }

    @Test void criteriaPaginationUsesTheSameFilterForRowsAndTotal() {
        shop("Rose B", ShopStatus.ACTIVE); shop("Rose A", ShopStatus.ACTIVE); shop("Hidden", ShopStatus.INACTIVE);
        entityManager.clear();
        Specification<Shop> active=(root, query, cb) -> cb.equal(root.get("status"), ShopStatus.ACTIVE);
        var first=shops.findAll(active, PageRequest.of(0, 1, Sort.by("name")));
        var second=shops.findAll(active, PageRequest.of(1, 1, Sort.by("name")));
        assertThat(first.getTotalElements()).isEqualTo(2);
        assertThat(first.getTotalPages()).isEqualTo(2);
        assertThat(first.getContent()).extracting(Shop::getName).containsExactly("Rose A");
        assertThat(second.getContent()).extracting(Shop::getName).containsExactly("Rose B");
    }

    @Test void searchEscapesSqlWildcardsAndPickupSelectionPrefersDefault() {
        var matching=shop("Rose_100%", ShopStatus.ACTIVE);
        shop("RoseX100A", ShopStatus.ACTIVE);
        assertThat(shops.findByStatusAndNameContainingIgnoreCaseOrderByNameAsc(ShopStatus.ACTIVE, "_100%", PageRequest.of(0, 10)))
                .extracting(Shop::getId).containsExactly(matching.getId());
        addresses.saveAndFlush(Address.builder().id(id()).shop(matching).addressLine("First").build());
        var preferred=addresses.saveAndFlush(Address.builder().id(id()).shop(matching).addressLine("Default").isDefault(true).build());
        entityManager.clear();
        assertThat(addresses.findPickupByShopId(matching.getId()).orElseThrow().getId()).isEqualTo(preferred.getId());
    }

    @Test void ownershipAndEmptyCollectionsAreHandledInsideQueries() {
        var owner=user(UserRole.CUSTOMER); var other=user(UserRole.CUSTOMER);
        var address=addresses.saveAndFlush(Address.builder().id(id()).user(owner).addressLine("Private").build());
        entityManager.clear();
        assertThat(addresses.findByIdAndUserId(address.getId(), other.getId())).isEmpty();
        assertThat(addresses.findByIdAndUserId(address.getId(), owner.getId())).isPresent();
        assertThat(details.findByOrderIdIn(List.of())).isEmpty();
        assertThat(details.countByOrderIds(List.of())).isEmpty();
        assertThat(images.findByProductIdInOrderByDisplayOrderAscIdAsc(List.of())).isEmpty();
        assertThat(orders.findByShopIdAndStatusIn("missing", List.of(), PageRequest.of(0, 10)).getTotalElements()).isZero();
    }

    @Test void customerListUsesGroupedLineCountsAndKeepsOrdersIsolated() {
        var customer=user(UserRole.CUSTOMER); var other=user(UserRole.CUSTOMER); var shop=shop("Orders", ShopStatus.ACTIVE);
        var order=orders.saveAndFlush(Order.builder().id(id()).customer(customer).shop(shop)
                .subTotal(new BigDecimal("20")).totalAmount(new BigDecimal("20")).build());
        for (int i=0; i<2; i++) details.saveAndFlush(OrderDetail.builder().id(id()).order(order).price(BigDecimal.TEN).quantity(1).build());
        entityManager.clear();
        assertThat(details.countByOrderIds(List.of(order.getId()))).containsEntry(order.getId(), 2L);
        assertThat(customerOrders.list(customer.getId(), 0, null).content().get(0).itemCount()).isEqualTo(2);
        assertThat(customerOrders.list(other.getId(), 0, null).content()).isEmpty();
        assertThat(orders.findByIdAndCustomerId(order.getId(), other.getId())).isEmpty();
    }

    @Test void deletingProductImagesUpdatesDatabase() {
        var shop=shop("Images", ShopStatus.ACTIVE);
        var category=categories.saveAndFlush(Category.builder().id(id()).name("Flowers").build());
        var product=products.saveAndFlush(Product.builder().id(id()).shop(shop).category(category).name("Rose").price(BigDecimal.TEN).stock(5).build());
        images.saveAndFlush(ProductImage.builder().id(id()).product(product).imageUrl("/sample.png").build());
        images.deleteByProductId(product.getId()); images.flush(); entityManager.clear();
        assertThat(images.countByProductId(product.getId())).isZero();
    }

    @Test
    @Transactional(propagation=Propagation.NOT_SUPPORTED)
    void repositoriesManageTransactionsWhenCalledDirectly() {
        var user=newUser(UserRole.CUSTOMER);
        try {
            users.saveAndFlush(user);
            assertThat(users.findById(user.getId())).isPresent();
        } finally { users.deleteById(user.getId()); }
        assertThat(users.existsById(user.getId())).isFalse();
    }

    @Test
    @Transactional(propagation=Propagation.NOT_SUPPORTED)
    void nativeHibernateAndRemainingJpaRepositoriesShareRollback() {
        var user=newUser(UserRole.CUSTOMER); var sessionId=id();
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            users.saveAndFlush(user);
            var authSession=new AuthSession(); authSession.setId(sessionId); authSession.setUserId(user.getId());
            authSession.setRefreshHash("hash"); authSession.setExpiresAt(Instant.now().plusSeconds(60));
            authSessions.saveAndFlush(authSession);
            status.setRollbackOnly();
        });
        assertThat(users.existsById(user.getId())).isFalse();
        assertThat(authSessions.existsById(sessionId)).isFalse();
    }
}
