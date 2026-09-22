package com.example.flowershop;

import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.dto.auth.LoginRequest;
import com.example.flowershop.repository.*;
import com.example.flowershop.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:adminshops;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa","spring.datasource.password=","spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop","debug=false"})
@AutoConfigureMockMvc
class AdminShopTests {
    @Autowired ShopRepository shops;
    @Autowired UserRepository users;
    @Autowired AuthSessionRepository sessions;
    @Autowired AdminShopService service;
    @Autowired ShopOperationGuard guard;
    @Autowired TokenAuthService auth;
    @Autowired PasswordEncoder encoder;
    @Autowired PlatformTransactionManager transactions;
    @Autowired MockMvc mvc;
    @MockitoBean JavaMailSender mail;
    User owner,admin;
    Shop shop;
    @BeforeEach void setup() {
        shops.deleteAll(); sessions.deleteAll(); users.deleteAll();
        owner=user(UserRole.SHOP); admin=user(UserRole.ADMIN);
        shop=shops.saveAndFlush(Shop.builder().id(UUID.randomUUID().toString()).owner(owner).name("Rose Store").build());
    }
    User user(UserRole role) {
        return users.saveAndFlush(User.builder().id(UUID.randomUUID().toString()).email(role.name().toLowerCase(Locale.ROOT)+"@test.example").fullName(role.name())
                .role(role).status(UserStatus.ACTIVE).isEmailVerified(true).passwordHash(encoder.encode("FlowerShop123")).build());
    }
    void conflict(Runnable call) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(409));
    }
    @Test void statusLifecyclePreservesOwnerLoginAndRecordsActor() {
        var tokens=auth.login(new LoginRequest(owner.getEmail(),"FlowerShop123"));
        conflict(()->service.transition(shop.getId(),"unblock",admin.getId()));
        conflict(()->service.transition(shop.getId(),"block",admin.getId()));
        assertThat(service.transition(shop.getId(),"approve",admin.getId()).status()).isEqualTo("ACTIVE");
        conflict(()->service.transition(shop.getId(),"approve",admin.getId()));
        assertThat(service.transition(shop.getId(),"block",admin.getId()).status()).isEqualTo("BANNED");
        assertThat(auth.authenticate(tokens.accessToken()).id()).isEqualTo(owner.getId());
        assertThat(service.transition(shop.getId(),"unblock",admin.getId()).status()).isEqualTo("ACTIVE");
        assertThat(shops.findById(shop.getId()).orElseThrow().getLastModifyBy()).isEqualTo(admin.getId());
        assertThat(users.findById(owner.getId()).orElseThrow().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }
    @Test void approvalRequiresEligibleOwnerAndInactiveIsNotApprovedOrUnlocked() {
        owner.setIsEmailVerified(false); users.saveAndFlush(owner);
        conflict(()->service.transition(shop.getId(),"approve",admin.getId()));
        owner.setIsEmailVerified(true); owner.setRole(UserRole.CUSTOMER); users.saveAndFlush(owner);
        conflict(()->service.transition(shop.getId(),"approve",admin.getId()));
        owner.setRole(UserRole.SHOP); owner.setStatus(UserStatus.BANNED); users.saveAndFlush(owner);
        conflict(()->service.transition(shop.getId(),"approve",admin.getId()));
        shop.setStatus(ShopStatus.INACTIVE); shops.saveAndFlush(shop);
        conflict(()->service.transition(shop.getId(),"unblock",admin.getId()));
        conflict(()->service.transition(shop.getId(),"approve",admin.getId()));
    }
    @Test void queryPaginationAndFilters() {
        var secondOwner=users.saveAndFlush(User.builder().id(UUID.randomUUID().toString()).email("second@test.example").fullName("Other Manager").role(UserRole.SHOP).status(UserStatus.ACTIVE).isEmailVerified(true).build());
        shops.saveAndFlush(Shop.builder().id(UUID.randomUUID().toString()).owner(secondOwner).name("Lily").status(ShopStatus.ACTIVE).build());
        assertThat(service.list("","",0,1).totalPages()).isEqualTo(2);
        assertThat(service.list("ROSE","PENDING",0,10).content()).hasSize(1);
        assertThat(service.list(owner.getEmail(),"",0,10).totalElements()).isZero();
        assertThat(service.list("Other Manager","",0,10).totalElements()).isZero();
        assertThat(service.list("%","",0,10).totalElements()).isZero();
        assertThatThrownBy(()->service.list("","BAD",0,10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->service.list("","",0,101)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->service.detail("missing")).isInstanceOf(ResponseStatusException.class);
    }
    @Test void guardRequiresActiveShopInExistingTransaction() {
        var tx=new TransactionTemplate(transactions);
        for(var status:ShopStatus.values()) {
            shop.setStatus(status); shops.saveAndFlush(shop);
            if(status==ShopStatus.ACTIVE) {
                String actual=tx.execute(s->guard.requireActive(shop.getId()).getId());
                assertThat(actual).isEqualTo(shop.getId());
            }
            else assertThatThrownBy(()->tx.execute(s->guard.requireActive(shop.getId())))
                    .isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(403));
        }
        assertThatThrownBy(()->guard.requireActive(shop.getId())).isInstanceOf(org.springframework.transaction.IllegalTransactionStateException.class);
    }
    @Test void onlyAdminAndCsrfCanApproveWithSafeResponses() throws Exception {
        mvc.perform(get("/api/admin/shops")).andExpect(status().isUnauthorized());
        var ownerToken=auth.login(new LoginRequest(owner.getEmail(),"FlowerShop123")).accessToken();
        mvc.perform(get("/api/admin/shops").header("Authorization","Bearer "+ownerToken)).andExpect(status().isForbidden());
        var token=auth.login(new LoginRequest(admin.getEmail(),"FlowerShop123")).accessToken();
        mvc.perform(get("/api/admin/shops/"+shop.getId()).header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.owner.email").value(owner.getEmail()))
                .andExpect(jsonPath("$.owner.passwordHash").doesNotExist());
        var path="/api/admin/shops/"+shop.getId()+"/approve";
        mvc.perform(put(path).header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
        var result=mvc.perform(get("/api/auth/csrf")).andReturn();
        var session=(MockHttpSession)result.getRequest().getSession(false);
        var match=Pattern.compile("\"token\":\"([^\"]+)\"").matcher(result.getResponse().getContentAsString());
        assertThat(match.find()).isTrue();
        mvc.perform(put(path).header("Authorization","Bearer "+token).session(session).header("X-CSRF-TOKEN",match.group(1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
    }
    @Test void simultaneousApprovalsCannotBothSucceed() throws Exception {
        var pool=Executors.newFixedThreadPool(2); var start=new CountDownLatch(1);
        Callable<Boolean> task=()-> { start.await(); try { service.transition(shop.getId(),"approve",admin.getId()); return true; }
            catch(ResponseStatusException e) { assertThat(e.getStatusCode().value()).isEqualTo(409); return false; } };
        try {
            var a=pool.submit(task); var b=pool.submit(task); start.countDown();
            assertThat(List.of(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
        } finally { pool.shutdownNow(); }
    }
}
