package com.example.flowershop;

import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.*;
import com.example.flowershop.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={
    "spring.datasource.url=jdbc:h2:mem:following;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.shop-follow-mail.enabled=false", "spring.jpa.show-sql=false"
})
@Transactional
@AutoConfigureMockMvc
class ShopFollowingTests {
    @MockitoBean JavaMailSender sender;
    @Autowired UserRepository users;
    @Autowired ShopRepository shops;
    @Autowired ProductRepository products;
    @Autowired CategoryRepository categories;
    @Autowired FavoriteShopService follows;
    @Autowired ShopProductMailRepository mails;
    @Autowired ShopProductPublicationRepository publications;
    @Autowired ManagerProductService manager;
    @Autowired ShopProductMailDelivery delivery;
    @Autowired PlatformTransactionManager transactions;
    @Autowired MockMvc mvc;

    User user(UserRole role) {
        return users.saveAndFlush(User.builder().id(UUID.randomUUID().toString())
                .email(UUID.randomUUID()+"@example.test").fullName("Test customer")
                .role(role).status(UserStatus.ACTIVE).isEmailVerified(true).build());
    }
    record Fixture(User owner,User customer,Shop shop,Category category) {}
    Fixture fixture() {
        var owner=user(UserRole.SHOP);var customer=user(UserRole.CUSTOMER);
        var shop=shops.saveAndFlush(Shop.builder().id(UUID.randomUUID().toString()).owner(owner)
                .name("Test flowers").status(ShopStatus.ACTIVE).build());
        var category=categories.saveAndFlush(Category.builder().id(UUID.randomUUID().toString())
                .name("Test category").status(CategoryStatus.ACTIVE).build());
        follows.add(customer.getId(),shop.getId());return new Fixture(owner,customer,shop,category);
    }
    ManagerProductService.Input input(Fixture f,ProductStatus status) {
        return new ManagerProductService.Input("New roses","Fresh roses",f.category().getId(),
                new BigDecimal("250000"),5,status,ProductType.READY_MADE,null);
    }
    @Test void followPersistsAndIsPrivateAndIdempotent() {
        var f=fixture();follows.add(f.customer().getId(),f.shop().getId());
        assertThat(follows.mine(f.customer().getId())).hasSize(1);
        assertThat(follows.status(f.customer().getId(),f.shop().getId()).favorite()).isTrue();
        assertThat(follows.mine(user(UserRole.CUSTOMER).getId())).isEmpty();
        follows.remove(f.customer().getId(),f.shop().getId());
        follows.remove(f.customer().getId(),f.shop().getId());
        assertThat(follows.mine(f.customer().getId())).isEmpty();
    }
    @Test void shopInformationIsPublicAndFollowListRequiresLogin() throws Exception {
        var f=fixture();
        mvc.perform(get("/api/public/shops").param("q","Test flowers"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(f.shop().getId()));
        mvc.perform(get("/api/public/shops/"+f.shop().getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Test flowers"));
        mvc.perform(get("/api/customer/favorite-shops")).andExpect(status().isUnauthorized());
        f.shop().setStatus(ShopStatus.INACTIVE);shops.saveAndFlush(f.shop());
        mvc.perform(get("/api/public/shops/"+f.shop().getId())).andExpect(status().isNotFound());
    }
    @Test void hiddenProductDoesNotCreateNotification() {
        var f=fixture();var result=manager.save(f.shop().getId(),null,f.owner().getId(),input(f,ProductStatus.INACTIVE));
        var product=products.findById(result.id()).orElseThrow();product.setAdminHidden(true);products.saveAndFlush(product);
        manager.unhide(f.shop().getId(),result.id(),f.owner().getId());
        assertThat(mails.findAll()).isEmpty();verifyNoInteractions(sender);
    }
    @Test void publicationQueuesOnlyOnceAndSendsIndividualEmail() {
        var f=fixture();var result=manager.save(f.shop().getId(),null,f.owner().getId(),input(f,ProductStatus.ACTIVE));
        manager.save(f.shop().getId(),result.id(),f.owner().getId(),input(f,ProductStatus.ACTIVE));
        manager.hide(f.shop().getId(),result.id(),f.owner().getId());
        manager.unhide(f.shop().getId(),result.id(),f.owner().getId());
        assertThat(mails.findAll()).hasSize(1);verifyNoInteractions(sender);
        var queued=mails.findAll().get(0);delivery.deliver(queued.getId());
        var capture=org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(capture.capture());
        assertThat(capture.getValue().getTo()).containsExactly(f.customer().getEmail());
        assertThat(capture.getValue().getText()).contains("New roses","/products/"+result.id(),"/following-shops");
        assertThat(queued.getStatus()).isEqualTo("SENT");
        delivery.deliver(queued.getId());verify(sender,times(1)).send(any(SimpleMailMessage.class));
    }
    @Test void draftOnlyNotifiesOnFirstActivation() {
        var f=fixture();var result=manager.save(f.shop().getId(),null,f.owner().getId(),input(f,ProductStatus.INACTIVE));
        assertThat(mails.findAll()).isEmpty();
        manager.unhide(f.shop().getId(),result.id(),f.owner().getId());
        assertThat(mails.findAll()).hasSize(1);
    }
    @Test void unfollowCancelsPendingEmail() {
        var f=fixture();manager.save(f.shop().getId(),null,f.owner().getId(),input(f,ProductStatus.ACTIVE));
        follows.remove(f.customer().getId(),f.shop().getId());
        var queued=mails.findAll().get(0);delivery.deliver(queued.getId());
        assertThat(queued.getStatus()).isEqualTo("CANCELLED");verifyNoInteractions(sender);
    }
    @Test void smtpFailureRetriesWithoutLosingProduct() {
        var f=fixture();var result=manager.save(f.shop().getId(),null,f.owner().getId(),input(f,ProductStatus.ACTIVE));
        doThrow(new MailSendException("Test SMTP failure")).when(sender).send(any(SimpleMailMessage.class));
        var queued=mails.findAll().get(0);delivery.deliver(queued.getId());
        assertThat(queued.getStatus()).isEqualTo("PENDING");assertThat(queued.getAttempts()).isEqualTo(1);
        assertThat(queued.getNextAttemptAt()).isAfter(LocalDateTime.now());
        assertThat(products.existsById(result.id())).isTrue();
        for(int attempt=2;attempt<=5;attempt++){queued.setNextAttemptAt(LocalDateTime.now().minusSeconds(1));delivery.deliver(queued.getId());}
        assertThat(queued.getStatus()).isEqualTo("FAILED");
    }
    @Test void unverifiedFollowersDoNotReceiveEmail() {
        var f=fixture();f.customer().setIsEmailVerified(false);users.saveAndFlush(f.customer());
        manager.save(f.shop().getId(),null,f.owner().getId(),input(f,ProductStatus.ACTIVE));
        assertThat(mails.findAll()).isEmpty();verifyNoInteractions(sender);
    }
    @Test @Transactional(propagation=Propagation.NOT_SUPPORTED)
    void rolledBackProductDoesNotLeaveMailOrPublication() {
        var tx=new TransactionTemplate(transactions);
        String id=tx.execute(status->{var f=fixture();var result=manager.save(f.shop().getId(),null,f.owner().getId(),input(f,ProductStatus.ACTIVE));status.setRollbackOnly();return result.id();});
        assertThat(products.existsById(id)).isFalse();assertThat(publications.existsById(id)).isFalse();
        assertThat(mails.findAll()).noneMatch(m->m.getProductId().equals(id));verifyNoInteractions(sender);
    }
}
