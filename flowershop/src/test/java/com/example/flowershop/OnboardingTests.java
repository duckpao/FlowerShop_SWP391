package com.example.flowershop;

import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.*;
import com.example.flowershop.service.*;
import com.example.flowershop.dto.auth.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.server.ResponseStatusException;
import org.mockito.ArgumentCaptor;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:onboarding;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa","spring.datasource.password=","spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop","debug=false"})
@AutoConfigureMockMvc
class OnboardingTests {
    @Autowired AuthService registration; @Autowired TokenAuthService auth; @Autowired AdminShopService admin;
    @Autowired StaffInvitationService invites; @Autowired StaffInvitationRepository invitations;
    @Autowired UserRepository users; @Autowired ShopRepository shops; @Autowired ShopStaffRepository staff;
    @Autowired AuthSessionRepository sessions; @Autowired PendingRegistrationRepository pending;
    @Autowired PasswordEncoder encoder; @Autowired MockMvc mvc;
    @MockitoBean JavaMailSender mail;
    User owner,recipient,other; Shop shop;
    @BeforeEach void setup() {
        invitations.deleteAll();staff.deleteAll();shops.deleteAll();sessions.deleteAll();pending.deleteAll();users.deleteAll();reset(mail);
        owner=user("owner@example.com",UserRole.SHOP);recipient=user("recipient@example.com",UserRole.CUSTOMER);other=user("other@example.com",UserRole.CUSTOMER);
        shop=shops.saveAndFlush(Shop.builder().id(UUID.randomUUID().toString()).owner(owner).name("Test Flowers").status(ShopStatus.ACTIVE).build());
    }
    User user(String email,UserRole role) { return users.saveAndFlush(User.builder().id(UUID.randomUUID().toString()).email(email).fullName("Test User")
        .role(role).status(UserStatus.ACTIVE).isEmailVerified(true).passwordHash(encoder.encode("FlowerShop123")).build()); }
    String lastCode(String pattern) {
        var capture=ArgumentCaptor.forClass(SimpleMailMessage.class);verify(mail,atLeastOnce()).send(capture.capture());
        var matcher=Pattern.compile(pattern).matcher(capture.getAllValues().get(capture.getAllValues().size()-1).getText());
        assertThat(matcher.find()).isTrue();return matcher.group();
    }
    String invite(String email) { invites.send(shop.getId(),owner.getId(),email);return lastCode("[a-f0-9-]{36}\\.[A-Za-z0-9_-]{43}"); }
    @Test void shopOnlyCreatedAfterOtpAndRequiresApproval() {
        String email="newmanager@example.com";
        registration.registerShop(new ShopRegisterRequest(new RegisterRequest(email,"FlowerShop123","Manager"),"New Florist","Description"));
        String otp=lastCode("[0-9]{6}");
        assertThat(users.findByEmail(email)).isEmpty();assertThat(shops.count()).isEqualTo(1);
        String wrong=otp.equals("000000")?"111111":"000000";
        assertThatThrownBy(()->registration.verify(email,wrong)).isInstanceOf(IllegalArgumentException.class);
        assertThat(shops.count()).isEqualTo(1);
        registration.verify(email,otp);
        var manager=users.findByEmail(email).orElseThrow();assertThat(manager.getRole()).isEqualTo(UserRole.SHOP);
        var created=shops.findByOwnerIdOrderByNameAsc(manager.getId());assertThat(created).hasSize(1);
        assertThat(created.get(0).getStatus()).isEqualTo(ShopStatus.PENDING);
        assertThat(auth.login(new LoginRequest(email,"FlowerShop123")).user().role()).isEqualTo("SHOP");
        admin.transition(created.get(0).getId(),"approve","test-admin");
        assertThat(shops.findById(created.get(0).getId()).orElseThrow().getStatus()).isEqualTo(ShopStatus.ACTIVE);
        assertThatThrownBy(()->registration.verify(email,otp)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void managerCanRegisterWithoutShopDetails() {
        String email="simplemanager@example.com";
        registration.registerShop(new ShopRegisterRequest(new RegisterRequest(email,"FlowerShop123","Manager"),null,null));
        assertThat(users.findByEmail(email)).isEmpty();
        registration.verify(email,lastCode("[0-9]{6}"));
        var manager=users.findByEmail(email).orElseThrow();
        assertThat(manager.getRole()).isEqualTo(UserRole.SHOP);
        var created=shops.findByOwnerIdOrderByNameAsc(manager.getId());
        assertThat(created).hasSize(1);
        assertThat(created.get(0).getName()).isEqualTo("Cửa hàng của Manager");
        assertThat(created.get(0).getStatus()).isEqualTo(ShopStatus.PENDING);
        assertThat(auth.login(new LoginRequest(email,"FlowerShop123")).user().role()).isEqualTo("SHOP");
    }
    @Test void acceptingRequiresMatchingEmailAndRevokesSessions() {
        var token=auth.login(new LoginRequest(recipient.getEmail(),"FlowerShop123"));String code=invite(recipient.getEmail());
        var stored=invitations.findAll().get(0);assertThat(stored.getTokenHash()).hasSize(64).doesNotContain(code);
        assertThatThrownBy(()->invites.accept(other.getId(),code)).isInstanceOf(IllegalArgumentException.class);
        invites.accept(recipient.getId(),code);
        assertThat(users.findById(recipient.getId()).orElseThrow().getRole()).isEqualTo(UserRole.SHOP_STAFF);
        assertThat(staff.findByShopIdAndUserId(shop.getId(),recipient.getId())).isPresent();
        assertThatThrownBy(()->auth.authenticate(token.accessToken())).isInstanceOf(org.springframework.security.core.AuthenticationException.class);
        assertThatThrownBy(()->invites.accept(recipient.getId(),code)).isInstanceOf(IllegalArgumentException.class);
        assertThat(auth.login(new LoginRequest(recipient.getEmail(),"FlowerShop123")).user().role()).isEqualTo("SHOP_STAFF");
    }
    @Test void newRecipientRegistersWithOtpBeforeJoining() {
        String email="newstaff@example.com";String code=invite(email);
        assertThat(users.findByEmail(email)).isEmpty();
        registration.registerCustomer(new RegisterRequest(email,"FlowerShop123","Staff"));String otp=lastCode("[0-9]{6}");
        registration.verify(email,otp);var newUser=users.findByEmail(email).orElseThrow();
        invites.accept(newUser.getId(),code);
        assertThat(staff.findByShopIdAndUserId(shop.getId(),newUser.getId())).isPresent();
    }
    @Test void cancellationExpiryAndBlockedShopRejectAcceptance() {
        String code=invite(recipient.getEmail());var i=invitations.findAll().get(0);
        invites.cancel(shop.getId(),owner.getId(),i.getId());
        assertThatThrownBy(()->invites.accept(recipient.getId(),code)).isInstanceOf(IllegalArgumentException.class);
        i.setStatus("PENDING");i.setExpiresAt(Instant.now().minusSeconds(1));invitations.saveAndFlush(i);
        assertThatThrownBy(()->invites.accept(recipient.getId(),code)).isInstanceOf(IllegalArgumentException.class);
        i.setExpiresAt(Instant.now().plusSeconds(300));invitations.saveAndFlush(i);
        admin.transition(shop.getId(),"block","admin");
        assertThatThrownBy(()->invites.accept(recipient.getId(),code)).isInstanceOf(IllegalArgumentException.class);
        assertThat(staff.count()).isZero();
    }
    @Test void resendInvalidatesOldCodeAndLimitsEmail() {
        String old=invite(recipient.getEmail());
        assertThatThrownBy(()->invites.send(shop.getId(),owner.getId(),recipient.getEmail())).isInstanceOf(ResponseStatusException.class);
        var i=invitations.findAll().get(0);i.setIssuedAt(Instant.now().minusSeconds(61));invitations.saveAndFlush(i);
        String fresh=invite(recipient.getEmail());assertThat(fresh).isNotEqualTo(old);
        assertThatThrownBy(()->invites.accept(recipient.getId(),old)).isInstanceOf(IllegalArgumentException.class);
        invites.accept(recipient.getId(),fresh);
    }
    @Test void wrongOwnerAndPrivilegedRecipientsAreRejected() {
        assertThatThrownBy(()->invites.send(shop.getId(),other.getId(),recipient.getEmail())).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->invites.send(shop.getId(),owner.getId(),owner.getEmail())).isInstanceOf(IllegalArgumentException.class);
        recipient.setRole(UserRole.ADMIN);users.saveAndFlush(recipient);
        assertThatThrownBy(()->invite(recipient.getEmail())).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void concurrentAcceptanceOnlySucceedsOnce() throws Exception {
        String code=invite(recipient.getEmail());var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        Callable<Boolean> task=()->{start.await();try{invites.accept(recipient.getId(),code);return true;}catch(IllegalArgumentException e){return false;}};
        try { var a=pool.submit(task);var b=pool.submit(task);start.countDown();assertThat(List.of(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false); }
        finally {pool.shutdownNow();}
        assertThat(staff.count()).isEqualTo(1);
    }
    @Test void httpRegistrationValidationAndInvitationSecurity() throws Exception {
        mvc.perform(post("/api/auth/register-shop").contentType("application/json").content("{}")).andExpect(status().isForbidden());
        var result=mvc.perform(get("/api/auth/csrf")).andReturn();var session=(MockHttpSession)result.getRequest().getSession(false);
        var matcher=Pattern.compile("\"token\":\"([^\"]+)\"").matcher(result.getResponse().getContentAsString());assertThat(matcher.find()).isTrue();
        mvc.perform(post("/api/auth/register-shop").session(session).header("X-CSRF-TOKEN",matcher.group(1)).contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/shop/mine/"+shop.getId()+"/invitations")).andExpect(status().isUnauthorized());
        var token=auth.login(new LoginRequest(recipient.getEmail(),"FlowerShop123")).accessToken();
        mvc.perform(get("/api/shop/mine/"+shop.getId()+"/invitations").header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
        mvc.perform(post("/api/account/staff-invitations/accept").header("Authorization","Bearer "+token).contentType("application/json").content("{\"code\":\"bad\"}"))
                .andExpect(status().isForbidden());
    }
}
