package com.example.flowershop;

import com.example.flowershop.dto.auth.LoginRequest;
import com.example.flowershop.entity.User;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.*;
import com.example.flowershop.service.*;
import com.example.flowershop.security.JwtService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.mockito.ArgumentCaptor;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:jwttest;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa","spring.datasource.password=","spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop","debug=false"})
@AutoConfigureMockMvc
class JwtAuthenticationTests {
    @Autowired TokenAuthService auth;
    @Autowired JwtService jwt;
    @Autowired UserRepository users;
    @Autowired AuthSessionRepository sessions;
    @Autowired OtpChallengeRepository challenges;
    @Autowired OtpService otp;
    @Autowired PasswordEncoder encoder;
    @Autowired MockMvc mvc;
    @MockitoBean JavaMailSender mail;
    private User user;
    private final String password="FlowerShop123";
    @BeforeEach void setup() {
        sessions.deleteAll(); challenges.deleteAll(); users.deleteAll(); reset(mail);
        user=users.saveAndFlush(User.builder().id(UUID.randomUUID().toString()).email("jwt@example.com")
                .fullName("JWT Test").role(UserRole.CUSTOMER).status(UserStatus.ACTIVE)
                .isEmailVerified(true).passwordHash(encoder.encode(password)).build());
    }
    private TokenAuthService.Tokens login() { return auth.login(new LoginRequest(user.getEmail(),password)); }
    @Test void loginReturnsSafeUserAndValidAccessToken() {
        var tokens=login();
        assertThat(tokens.accessToken().split("\\.")).hasSize(3);
        assertThat(auth.authenticate(tokens.accessToken()).role()).isEqualTo("CUSTOMER");
        assertThat(sessions.findAll().get(0).getRefreshHash()).hasSize(64).isNotEqualTo(tokens.refreshToken());
    }
    @Test void incorrectUnverifiedInactiveAndUnknownUsersCannotLogin() {
        assertThatThrownBy(()->auth.login(new LoginRequest(user.getEmail(),"wrong"))).isInstanceOf(org.springframework.security.core.AuthenticationException.class);
        assertThatThrownBy(()->auth.login(new LoginRequest("missing@example.com",password))).isInstanceOf(org.springframework.security.core.AuthenticationException.class);
        user.setIsEmailVerified(false); users.saveAndFlush(user);
        assertThatThrownBy(this::login).isInstanceOf(org.springframework.security.core.AuthenticationException.class);
        user.setIsEmailVerified(true); user.setStatus(UserStatus.BANNED); users.saveAndFlush(user);
        assertThatThrownBy(this::login).isInstanceOf(org.springframework.security.core.AuthenticationException.class);
        assertThat(sessions.count()).isZero();
    }
    @Test void refreshRotatesAndReplayRevokesEntireSession() {
        var first=login(); var second=auth.refresh(first.refreshToken());
        assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());
        assertThat(auth.authenticate(second.accessToken()).id()).isEqualTo(user.getId());
        assertThatThrownBy(()->auth.refresh(first.refreshToken())).isInstanceOf(org.springframework.security.core.AuthenticationException.class);
        assertThatThrownBy(()->auth.authenticate(second.accessToken())).isInstanceOf(org.springframework.security.core.AuthenticationException.class);
        assertThatThrownBy(()->auth.refresh(second.refreshToken())).isInstanceOf(org.springframework.security.core.AuthenticationException.class);
    }
    @Test void logoutRevokesAccessAndRefreshButNotOtherDevice() {
        var first=login(); var other=login(); auth.logout(first.refreshToken());
        assertThatThrownBy(()->auth.authenticate(first.accessToken())).isInstanceOf(org.springframework.security.core.AuthenticationException.class);
        assertThatThrownBy(()->auth.refresh(first.refreshToken())).isInstanceOf(org.springframework.security.core.AuthenticationException.class);
        assertThat(auth.authenticate(other.accessToken()).id()).isEqualTo(user.getId());
    }
    @Test void invalidExpiredAndWrongKindTokensAreRejected() throws InterruptedException {
        var token=login(); String sid=jwt.decode(token.accessToken(),"access").getClaimAsString("sid");
        assertThatThrownBy(()->auth.authenticate(token.refreshToken())).isInstanceOf(org.springframework.security.oauth2.jwt.JwtException.class);
        assertThatThrownBy(()->auth.refresh(token.accessToken())).isInstanceOf(org.springframework.security.oauth2.jwt.JwtException.class);
        assertThatThrownBy(()->auth.authenticate(token.accessToken()+"tamper")).isInstanceOf(org.springframework.security.oauth2.jwt.JwtException.class);
        String expired=jwt.issue(user.getId(),sid,"access",Instant.now().plusSeconds(1));
        Thread.sleep(1100);
        assertThatThrownBy(()->auth.authenticate(expired)).isInstanceOf(org.springframework.security.oauth2.jwt.JwtException.class);
    }
    @Test void roleAndAccountStateAreCheckedOnEveryRequest() throws Exception {
        var token=login();
        mvc.perform(get("/api/admin/not-implemented").header("Authorization","Bearer "+token.accessToken())).andExpect(status().isForbidden());
        for (UserRole role:UserRole.values()) {
            user.setRole(role); users.saveAndFlush(user);
            if (role == UserRole.DELIVERY) {
                assertThatThrownBy(this::login).isInstanceOf(org.springframework.security.core.AuthenticationException.class);
                assertThatThrownBy(()->auth.authenticate(token.accessToken())).isInstanceOf(org.springframework.security.core.AuthenticationException.class);
                assertThatThrownBy(()->auth.refresh(token.refreshToken())).isInstanceOf(org.springframework.security.core.AuthenticationException.class);
                continue;
            }
            assertThat(auth.authenticate(token.accessToken()).role()).isEqualTo(role.name());
            var paths=java.util.Map.of(UserRole.ADMIN,"/api/admin/probe",UserRole.SHOP,"/api/shop/probe",
                    UserRole.CUSTOMER,"/api/customer/probe",UserRole.SHOP_STAFF,"/api/staff/probe");
            for (var entry:paths.entrySet()) {
                if (role != entry.getKey()) mvc.perform(get(entry.getValue())
                        .header("Authorization","Bearer "+token.accessToken())).andExpect(status().isForbidden());
            }
            mvc.perform(get("/api/delivery/probe").header("Authorization","Bearer "+token.accessToken()))
                    .andExpect(status().isForbidden());
        }
        user.setStatus(UserStatus.BANNED); users.saveAndFlush(user);
        assertThatThrownBy(()->auth.authenticate(token.accessToken())).isInstanceOf(org.springframework.security.core.AuthenticationException.class);
    }
    @Test void passwordResetRevokesEveryDevice() {
        var first=login(); var second=login(); otp.forgotPassword(user.getEmail());
        var capture=ArgumentCaptor.forClass(SimpleMailMessage.class); verify(mail).send(capture.capture());
        var matcher=Pattern.compile("[0-9]{6}").matcher(capture.getValue().getText()); assertThat(matcher.find()).isTrue();
        otp.resetPassword(user.getEmail(),matcher.group(),"NewPassword123456","NewPassword123456");
        assertThatThrownBy(()->auth.authenticate(first.accessToken())).isInstanceOf(org.springframework.security.core.AuthenticationException.class);
        assertThatThrownBy(()->auth.refresh(second.refreshToken())).isInstanceOf(org.springframework.security.core.AuthenticationException.class);
        assertThatThrownBy(this::login).isInstanceOf(org.springframework.security.core.AuthenticationException.class);
        assertThat(auth.login(new LoginRequest(user.getEmail(),"NewPassword123456")).user().id()).isEqualTo(user.getId());
    }
    @Test void httpCookieCsrfAndBearerFlow() throws Exception {
        mvc.perform(get("/images/logo/logo.svg")).andExpect(status().isOk());
        String body="{\"email\":\"jwt@example.com\",\"password\":\""+password+"\"}";
        mvc.perform(post("/api/auth/login").contentType("application/json").content(body)).andExpect(status().isForbidden());
        var csrf=mvc.perform(get("/api/auth/csrf")).andReturn();
        var session=(MockHttpSession)csrf.getRequest().getSession(false);
        var matcher=Pattern.compile("\"token\":\"([^\"]+)\"").matcher(csrf.getResponse().getContentAsString()); assertThat(matcher.find()).isTrue();
        String value=matcher.group(1);
        mvc.perform(post("/api/auth/refresh").session(session).header("X-CSRF-TOKEN",value))
            .andExpect(status().isNoContent());
        mvc.perform(post("/api/auth/refresh").session(session).cookie(new Cookie("refresh_token","invalid"))
            .header("X-CSRF-TOKEN",value)).andExpect(status().isNoContent());
        var result=mvc.perform(post("/api/auth/login").session(session).header("X-CSRF-TOKEN",value)
                .contentType("application/json").content(body)).andExpect(status().isOk()).andReturn();
        String json=result.getResponse().getContentAsString();
        assertThat(json).doesNotContain("refreshToken","passwordHash",password);
        assertThat(result.getResponse().getHeader("Set-Cookie")).contains("HttpOnly","SameSite=Lax","Path=/api/auth");
        var accessMatch=Pattern.compile("\"accessToken\":\"([^\"]+)\"").matcher(json); assertThat(accessMatch.find()).isTrue();
        String access=accessMatch.group(1);
        Cookie refresh=result.getResponse().getCookie("refresh_token"); assertThat(refresh).isNotNull();
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header("Authorization","Bearer "+access)).andExpect(status().isOk());
        mvc.perform(post("/api/auth/refresh").cookie(refresh)).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/logout").cookie(refresh).session(session).header("X-CSRF-TOKEN",value)).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").header("Authorization","Bearer "+access)).andExpect(status().isUnauthorized());
    }
}
