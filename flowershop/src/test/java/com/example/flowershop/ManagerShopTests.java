package com.example.flowershop;
import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.*;
import com.example.flowershop.service.*;
import com.example.flowershop.dto.account.AddressRequest;
import com.example.flowershop.dto.auth.LoginRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:manager;MODE=MySQL;DB_CLOSE_DELAY=-1","spring.datasource.username=sa","spring.datasource.password=","spring.datasource.driver-class-name=org.h2.Driver","spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureMockMvc
class ManagerShopTests {
    @Autowired UserRepository users; @Autowired ShopRepository shops; @Autowired ShopStaffRepository members;
    @Autowired AddressRepository addresses; @Autowired AuthSessionRepository sessions;
    @Autowired ManagerShopService service; @Autowired ShopOperationGuard guard;
    @Autowired PlatformTransactionManager transactions; @Autowired TokenAuthService auth;
    @Autowired PasswordEncoder encoder; @Autowired MockMvc mvc;
    @MockitoBean JavaMailSender mail;
    User owner,other,staff; Shop shop;
    @BeforeEach void setup() {
        members.deleteAll(); addresses.deleteAll(); shops.deleteAll(); sessions.deleteAll(); users.deleteAll();
        owner=user(UserRole.SHOP); other=user(UserRole.SHOP); staff=user(UserRole.SHOP_STAFF);
        shop=shops.saveAndFlush(Shop.builder().id(UUID.randomUUID().toString()).name("Test").owner(owner).status(ShopStatus.ACTIVE).build());
    }
    User user(UserRole role) { return users.saveAndFlush(User.builder().id(UUID.randomUUID().toString()).email(UUID.randomUUID()+"@test.example").fullName("Test")
        .role(role).status(UserStatus.ACTIVE).isEmailVerified(true).passwordHash(encoder.encode("FlowerShop123")).build()); }
    ManagerShopService.Profile profile() { return new ManagerShopService.Profile("New Shop","Description","https://example.com/logo.png"); }
    @Test void managerCannotOwnSecondShop() {
        assertThatThrownBy(()->shops.saveAndFlush(Shop.builder().id(UUID.randomUUID().toString()).name("Second Shop").owner(owner).build()))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(service.mine(owner.getId())).hasSize(1);
    }
    @Test void ownershipAndStatusCannotBeChangedViaProfile() {
        assertThat(service.mine(other.getId())).isEmpty();
        assertThatThrownBy(()->service.update(shop.getId(),other.getId(),profile())).isInstanceOf(ResponseStatusException.class);
        assertThat(service.update(shop.getId(),owner.getId(),profile()).name()).isEqualTo("New Shop");
        assertThat(shops.findById(shop.getId()).orElseThrow().getStatus()).isEqualTo(ShopStatus.ACTIVE);
        shop.setStatus(ShopStatus.BANNED); shops.saveAndFlush(shop);
        assertThat(service.mine(owner.getId())).hasSize(1);
        assertThatThrownBy(()->service.update(shop.getId(),owner.getId(),profile())).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->service.add(shop.getId(),owner.getId(),staff.getEmail())).isInstanceOf(ResponseStatusException.class);
    }
    @Test void membershipRevocationAffectsOnlyShopNotAccount() {
        service.add(shop.getId(),owner.getId(),staff.getEmail());
        assertThat(service.assigned(staff.getId())).hasSize(1);
        var tx=new TransactionTemplate(transactions);
        tx.executeWithoutResult(s->guard.requireAccess(shop.getId(),staff.getId()));
        service.active(shop.getId(),owner.getId(),staff.getId(),false);
        assertThat(service.assigned(staff.getId())).isEmpty();
        assertThatThrownBy(()->tx.executeWithoutResult(s->guard.requireAccess(shop.getId(),staff.getId()))).isInstanceOf(ResponseStatusException.class);
        assertThat(users.findById(staff.getId()).orElseThrow().getStatus()).isEqualTo(UserStatus.ACTIVE);
        service.active(shop.getId(),owner.getId(),staff.getId(),true);
        shop.setStatus(ShopStatus.BANNED); shops.saveAndFlush(shop);
        assertThatThrownBy(()->tx.executeWithoutResult(s->guard.requireAccess(shop.getId(),staff.getId()))).isInstanceOf(ResponseStatusException.class);
    }
    @Test void rejectsWrongRolesDuplicateAndForeignMembership() {
        assertThatThrownBy(()->service.add(shop.getId(),owner.getId(),other.getEmail())).isInstanceOf(IllegalArgumentException.class);
        service.add(shop.getId(),owner.getId(),staff.getEmail());
        assertThatThrownBy(()->service.add(shop.getId(),owner.getId(),staff.getEmail())).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->service.active(shop.getId(),other.getId(),staff.getId(),false)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->service.staff(shop.getId(),other.getId())).isInstanceOf(ResponseStatusException.class);
    }
    @Test void shopAddressSeparateFromCustomerAndOwnerOnly() {
        var request=new AddressRequest("12 Test","Hà Nội","Test","Test",true);
        service.saveAddress(shop.getId(),owner.getId(),request); service.saveAddress(shop.getId(),owner.getId(),request);
        assertThat(service.address(shop.getId(),owner.getId())).hasSize(1);
        assertThat(addresses.findByUserIdAndShopIsNullOrderByCreatedDateAscIdAsc(owner.getId())).isEmpty();
        assertThatThrownBy(()->service.saveAddress(shop.getId(),other.getId(),request)).isInstanceOf(ResponseStatusException.class);
    }
    @Test void endpointRolesAndCsrf() throws Exception {
        mvc.perform(get("/api/shop/mine")).andExpect(status().isUnauthorized());
        String staffToken=auth.login(new LoginRequest(staff.getEmail(),"FlowerShop123")).accessToken();
        mvc.perform(get("/api/shop/mine").header("Authorization","Bearer "+staffToken)).andExpect(status().isForbidden());
        mvc.perform(get("/api/staff/shops").header("Authorization","Bearer "+staffToken)).andExpect(status().isOk());
        String token=auth.login(new LoginRequest(owner.getEmail(),"FlowerShop123")).accessToken();
        mvc.perform(get("/api/shop/mine").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$[0].owner.passwordHash").doesNotExist());
        mvc.perform(put("/api/shop/mine/"+shop.getId()).header("Authorization","Bearer "+token).contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }
}
