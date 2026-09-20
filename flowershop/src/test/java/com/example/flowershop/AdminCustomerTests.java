package com.example.flowershop;

import com.example.flowershop.dto.auth.LoginRequest;
import com.example.flowershop.entity.User;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.*;
import com.example.flowershop.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.AuthenticationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.util.regex.Pattern;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:admincustomers;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa","spring.datasource.password=","spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop","debug=false"})
@AutoConfigureMockMvc
class AdminCustomerTests {
    @Autowired UserRepository users;
    @Autowired AuthSessionRepository sessions;
    @Autowired AdminCustomerService adminService;
    @Autowired TokenAuthService auth;
    @Autowired PasswordEncoder encoder;
    @Autowired MockMvc mvc;
    @MockitoBean JavaMailSender mail;
    User admin,customer;
    @BeforeEach void setup() {
        sessions.deleteAll(); users.deleteAll();
        admin=create("admin@test.example",UserRole.ADMIN); customer=create("customer@test.example",UserRole.CUSTOMER);
    }
    User create(String email,UserRole role) {
        return users.saveAndFlush(User.builder().id(UUID.randomUUID().toString()).email(email).fullName("Test User")
                .passwordHash(encoder.encode("FlowerShop123")).role(role).status(UserStatus.ACTIVE).isEmailVerified(true).build());
    }
    TokenAuthService.Tokens login(User u) { return auth.login(new LoginRequest(u.getEmail(),"FlowerShop123")); }
    @Test void listFiltersAndPaginatesOnlyCustomersAndEscapesWildcards() {
        create("second@test.example",UserRole.CUSTOMER); create("shop@test.example",UserRole.SHOP);
        var page=adminService.list("","",0,1);
        assertThat(page.totalElements()).isEqualTo(2); assertThat(page.totalPages()).isEqualTo(2);
        assertThat(page.content()).hasSize(1);
        assertThat(adminService.list("CUSTOMER@","",0,10).content()).singleElement().satisfies(c->assertThat(c.id()).isEqualTo(customer.getId()));
        assertThat(adminService.list("%","",0,10).totalElements()).isZero();
        adminService.setBlocked(customer.getId(),true,admin.getId());
        assertThat(adminService.list("","BANNED",0,10).totalElements()).isEqualTo(1);
        assertThatThrownBy(()->adminService.list("","UNKNOWN",0,10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->adminService.list("","",-1,10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->adminService.list("","",0,101)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void blockingRevokesAllDevicesAndUnblockingDoesNotRestoreTokens() {
        var first=login(customer); var second=login(customer);
        adminService.setBlocked(customer.getId(),true,admin.getId());
        assertThatThrownBy(()->auth.authenticate(first.accessToken())).isInstanceOf(AuthenticationException.class);
        assertThatThrownBy(()->auth.refresh(second.refreshToken())).isInstanceOf(AuthenticationException.class);
        assertThatThrownBy(()->login(customer)).isInstanceOf(AuthenticationException.class);
        assertThat(users.findById(customer.getId()).orElseThrow().getLastModifyBy()).isEqualTo(admin.getId());
        adminService.setBlocked(customer.getId(),false,admin.getId());
        assertThatThrownBy(()->auth.authenticate(first.accessToken())).isInstanceOf(AuthenticationException.class);
        assertThatThrownBy(()->auth.refresh(second.refreshToken())).isInstanceOf(AuthenticationException.class);
        assertThat(login(customer).user().id()).isEqualTo(customer.getId());
    }
    @Test void nonCustomerTargetsAreNotAccessibleAndVerificationIsPreserved() {
        assertThatThrownBy(()->adminService.detail(admin.getId())).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->adminService.setBlocked(admin.getId(),true,admin.getId())).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->adminService.detail("missing")).isInstanceOf(ResponseStatusException.class);
        customer.setIsEmailVerified(false); users.saveAndFlush(customer);
        adminService.setBlocked(customer.getId(),true,admin.getId());
        assertThat(adminService.setBlocked(customer.getId(),false,admin.getId()).emailVerified()).isFalse();
        assertThatThrownBy(()->login(customer)).isInstanceOf(AuthenticationException.class);
    }
    @Test void adminOnlyEndpointsAndSafeResponse() throws Exception {
        mvc.perform(get("/api/admin/customers")).andExpect(status().isUnauthorized());
        for(var role:List.of(UserRole.CUSTOMER,UserRole.SHOP,UserRole.SHOP_STAFF)) {
            customer.setRole(role); users.saveAndFlush(customer);
            mvc.perform(get("/api/admin/customers").header("Authorization","Bearer "+login(customer).accessToken())).andExpect(status().isForbidden());
        }
        customer.setRole(UserRole.CUSTOMER); users.saveAndFlush(customer);
        String token=login(admin).accessToken();
        mvc.perform(get("/api/admin/customers").header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist());
        mvc.perform(get("/api/admin/customers/"+customer.getId()).header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(customer.getEmail()))
                .andExpect(jsonPath("$.passwordHash").doesNotExist()).andExpect(jsonPath("$.googleId").doesNotExist());
    }
    @Test void mutationRequiresCsrfAndExplicitBoolean() throws Exception {
        String bearer="Bearer "+login(admin).accessToken();
        String path="/api/admin/customers/"+customer.getId()+"/blocked";
        mvc.perform(put(path).header("Authorization",bearer).contentType("application/json").content("{\"blocked\":true}"))
                .andExpect(status().isForbidden());
        for(String body:List.of("{}","{\"blocked\":true}")) {
            var result=mvc.perform(get("/api/auth/csrf")).andReturn();
            var session=(MockHttpSession)result.getRequest().getSession(false);
            var matcher=Pattern.compile("\"token\":\"([^\"]+)\"").matcher(result.getResponse().getContentAsString());
            assertThat(matcher.find()).isTrue();
            mvc.perform(put(path).header("Authorization",bearer).session(session).header("X-CSRF-TOKEN",matcher.group(1))
                    .contentType("application/json").content(body)).andExpect(status().is(body.equals("{}")?400:200));
        }
        assertThat(users.findById(customer.getId()).orElseThrow().getStatus()).isEqualTo(UserStatus.BANNED);
    }
}
