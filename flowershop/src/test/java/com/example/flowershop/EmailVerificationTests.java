package com.example.flowershop;

import com.example.flowershop.dto.auth.RegisterRequest;
import com.example.flowershop.entity.*;
import com.example.flowershop.exception.InvalidOtpException;
import com.example.flowershop.repository.*;
import com.example.flowershop.service.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import static com.example.flowershop.entity.OtpChallenge.Purpose.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:pendingtest;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop", "app.mail.from=test@example.com", "debug=false"
})
@AutoConfigureMockMvc
class EmailVerificationTests {
    @Autowired AuthService auth;
    @Autowired OtpService otp;
    @Autowired com.example.flowershop.security.CaptchaService captcha;
    @Autowired UserRepository users;
    @Autowired PendingRegistrationRepository pending;
    @Autowired OtpChallengeRepository challenges;
    @Autowired PasswordEncoder encoder;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired MockMvc mvc;
    @MockitoBean JavaMailSender mailSender;
    private static final String EMAIL = "otp-test@example.com";
    private static final String PASSWORD = "FlowerShop-test-password";

    @BeforeEach void clean() {
        pending.deleteAll(); challenges.deleteAll(); users.deleteAll(); reset(mailSender);
    }

    @Test void noUserUntilCorrectOtpThenPendingIsDeletedAndWelcomeSent() {
        String code = register();
        assertThat(users.count()).isZero();
        assertThat(challenges.count()).isZero();
        assertThat(draft().getPasswordHash()).isNotEqualTo(PASSWORD);
        assertThat(encoder.matches(PASSWORD, draft().getPasswordHash())).isTrue();
        assertThat(encoder.matches(code, draft().getOtpHash())).isTrue();
        assertThat(mails()).hasSize(1);
        auth.verify(EMAIL, code);
        assertThat(users.count()).isEqualTo(1);
        assertThat(user().getIsEmailVerified()).isTrue();
        assertThat(user().getRole().name()).isEqualTo("CUSTOMER");
        assertThat(encoder.matches(PASSWORD, user().getPasswordHash())).isTrue();
        assertThat(pending.count()).isZero();
        assertThat(mails()).hasSize(2);
        assertThat(mails().get(1).getSubject()).contains("Đăng ký thành công");
        assertThatThrownBy(() -> auth.verify(EMAIL, code)).isInstanceOf(InvalidOtpException.class);
    }

    @Test void expiredOrWrongOtpCreatesNoUser() {
        String code = register();
        assertThatThrownBy(() -> auth.verify(EMAIL, wrong(code))).isInstanceOf(InvalidOtpException.class);
        assertThat(draft().getFailedAttempts()).isEqualTo(1);
        var p = draft(); p.setExpiresAt(Instant.now().minusSeconds(1)); pending.saveAndFlush(p);
        assertThatThrownBy(() -> auth.verify(EMAIL, code)).isInstanceOf(InvalidOtpException.class);
        assertThat(users.count()).isZero(); assertThat(pending.count()).isEqualTo(1);
    }

    @Test void attemptLimitsPersistAcrossResends() {
        String code = register();
        for (int i = 1; i <= 5; i++) {
            assertThatThrownBy(() -> auth.verify(EMAIL, wrong(code))).isInstanceOf(InvalidOtpException.class);
            assertThat(draft().getFailedAttempts()).isEqualTo(i);
        }
        assertThatThrownBy(() -> auth.verify(EMAIL, code)).isInstanceOf(InvalidOtpException.class);
        ageIssue(); assertThatThrownBy(() -> auth.resend(EMAIL)).isInstanceOf(com.example.flowershop.exception.CaptchaRequiredException.class); assertThat(mails()).hasSize(1);
        var p = draft(); p.setWindowStart(Instant.now().minusSeconds(901)); pending.saveAndFlush(p);
        var image=captcha.create(EMAIL,"register");
        auth.resend(EMAIL,image.id(),answer(image.id())); assertThat(draft().getFailedAttempts()).isZero();
        auth.verify(EMAIL, lastCode()); assertThat(users.count()).isEqualTo(1);
    }

    private String answer(String id) {
        var map=(java.util.Map<?,?>) org.springframework.test.util.ReflectionTestUtils.getField(captcha,"challenges");
        return org.springframework.test.util.ReflectionTestUtils.invokeMethod(map.get(id),"answer");
    }
    @Test void resetRequiresCaptchaAfterFiveMistakesAndAcceptsCorrectCodeWithCaptcha() {
        auth.verify(EMAIL,register()); otp.forgotPassword(EMAIL); String code=lastCode();
        for(int i=0;i<5;i++) assertThatThrownBy(() -> otp.resetPassword(EMAIL,wrong(code),PASSWORD,PASSWORD)).isInstanceOf(InvalidOtpException.class);
        assertThatThrownBy(() -> otp.resetPassword(EMAIL,code,PASSWORD,PASSWORD)).isInstanceOf(com.example.flowershop.exception.CaptchaRequiredException.class);
        var image=captcha.create(EMAIL,"reset");
        otp.resetPassword(EMAIL,code,PASSWORD,PASSWORD,image.id(),answer(image.id()));
        assertThat(challenges.findByUserIdAndPurpose(user().getId(),RESET_PASSWORD).orElseThrow().getUsedAt()).isNotNull();
    }
    @Test void resendImmediatelyRotatesCode() {
        String old = register();
        auth.resend(EMAIL); assertThat(lastCode()).isNotEqualTo(old);
        assertThatThrownBy(() -> auth.verify(EMAIL, old)).isInstanceOf(InvalidOtpException.class);
        ageIssue(); auth.resend(EMAIL); assertThat(mails()).hasSize(3);
        auth.resend(EMAIL); assertThat(mails()).hasSize(4);
        assertThat(draft().getFailedAttempts()).isEqualTo(1); assertThat(users.count()).isZero();
    }

    @Test void newRegistrationDetailsRequireNewCode() {
        String old = register(); ageIssue();
        auth.registerCustomer(new RegisterRequest(EMAIL, "Another-long-password", "Updated Name"));
        String fresh = lastCode();
        assertThatThrownBy(() -> auth.verify(EMAIL, old)).isInstanceOf(InvalidOtpException.class);
        assertThat(users.count()).isZero();
        auth.verify(EMAIL, fresh);
        assertThat(user().getFullName()).isEqualTo("Updated Name");
        assertThat(encoder.matches("Another-long-password", user().getPasswordHash())).isTrue();
        String hash = user().getPasswordHash();
        auth.registerCustomer(request()); auth.resend(EMAIL);
        assertThat(user().getPasswordHash()).isEqualTo(hash);
        assertThat(pending.count()).isZero(); assertThat(mails()).hasSize(3);
    }

    @Test void forgotPasswordCannotUsePendingRegistrationOrRegistrationCode() {
        String code = register(); otp.forgotPassword(EMAIL);
        assertThat(challenges.count()).isZero(); assertThat(mails()).hasSize(1);
        assertThatThrownBy(() -> otp.resetPassword(EMAIL, code, PASSWORD, PASSWORD)).isInstanceOf(InvalidOtpException.class);
        auth.verify(EMAIL, code);
        assertThatThrownBy(() -> otp.resetPassword(EMAIL, code, PASSWORD, PASSWORD)).isInstanceOf(InvalidOtpException.class);
        otp.forgotPassword(EMAIL); String reset = lastCode();
        otp.resetPassword(EMAIL, reset, "New-secure-password", "New-secure-password");
        assertThat(encoder.matches("New-secure-password", user().getPasswordHash())).isTrue();
        assertThatThrownBy(() -> otp.resetPassword(EMAIL, reset, PASSWORD, PASSWORD)).isInstanceOf(InvalidOtpException.class);
    }

    @Test void resetWrongAttemptsAreCommitted() {
        auth.verify(EMAIL, register()); otp.forgotPassword(EMAIL); String code = lastCode();
        assertThatThrownBy(() -> otp.resetPassword(EMAIL, wrong(code), PASSWORD, PASSWORD)).isInstanceOf(InvalidOtpException.class);
        assertThat(challenges.findByUserIdAndPurpose(user().getId(), RESET_PASSWORD).orElseThrow().getFailedAttempts()).isEqualTo(1);
    }

    @Test void resetRejectsExpiredOtpAndMismatchedPasswordsWithoutChangingPassword() {
        auth.verify(EMAIL, register()); otp.forgotPassword(EMAIL); String code = lastCode();
        String oldHash = user().getPasswordHash();
        assertThatThrownBy(() -> otp.resetPassword(EMAIL, code, PASSWORD, "Different-password"))
                .isInstanceOf(IllegalArgumentException.class);
        var c = challenges.findByUserIdAndPurpose(user().getId(), RESET_PASSWORD).orElseThrow();
        assertThat(c.getUsedAt()).isNull();
        c.setExpiresAt(Instant.now().minusSeconds(1)); challenges.saveAndFlush(c);
        assertThatThrownBy(() -> otp.resetPassword(EMAIL, code, PASSWORD, PASSWORD)).isInstanceOf(InvalidOtpException.class);
        assertThat(user().getPasswordHash()).isEqualTo(oldHash);
    }

    @Test void resetResendInvalidatesOldOtpAndRetainsFailedAttempts() {
        auth.verify(EMAIL, register()); otp.forgotPassword(EMAIL); String old = lastCode();
        int sent = mails().size(); otp.forgotPassword(EMAIL); assertThat(mails()).hasSize(sent + 1);
        assertThatThrownBy(() -> otp.resetPassword(EMAIL, wrong(old), PASSWORD, PASSWORD)).isInstanceOf(InvalidOtpException.class);
        var c = challenges.findByUserIdAndPurpose(user().getId(), RESET_PASSWORD).orElseThrow();
        c.setIssuedAt(Instant.now().minusSeconds(61)); challenges.saveAndFlush(c);
        otp.forgotPassword(EMAIL); String fresh = lastCode();
        assertThat(fresh).isNotEqualTo(old);
        assertThat(challenges.findByUserIdAndPurpose(user().getId(), RESET_PASSWORD).orElseThrow().getFailedAttempts()).isEqualTo(1);
        assertThatThrownBy(() -> otp.resetPassword(EMAIL, old, PASSWORD, PASSWORD)).isInstanceOf(InvalidOtpException.class);
        otp.resetPassword(EMAIL, fresh, PASSWORD, PASSWORD);
    }

    @Test void forgotUnknownOrBannedAccountDoesNotSendEmail() {
        otp.forgotPassword("unknown@example.com"); verifyNoInteractions(mailSender);
        auth.verify(EMAIL, register());
        var u = user(); u.setStatus(com.example.flowershop.entity.enums.UserStatus.BANNED); users.saveAndFlush(u);
        clearInvocations(mailSender); otp.forgotPassword(EMAIL); verifyNoInteractions(mailSender);
        assertThat(challenges.count()).isZero();
    }

    @Test void concurrentVerificationCreatesOneUserAndOneWelcome() throws Exception {
        String code = register(); var pool = Executors.newFixedThreadPool(2); CountDownLatch gate = new CountDownLatch(1);
        Callable<Boolean> attempt = () -> { gate.await(); try { auth.verify(EMAIL, code); return true; }
            catch (InvalidOtpException e) { return false; } };
        try {
            var a = pool.submit(attempt); var b = pool.submit(attempt); gate.countDown();
            assertThat(a.get(15, TimeUnit.SECONDS)).isNotEqualTo(b.get(15, TimeUnit.SECONDS));
            assertThat(users.count()).isEqualTo(1); assertThat(pending.count()).isZero(); assertThat(mails()).hasSize(2);
        } finally { pool.shutdownNow(); }
    }

    @Test void verificationRollbackRestoresPendingAndDoesNotSendWelcome() {
        String code = register(); clearInvocations(mailSender);
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            auth.verify(EMAIL, code); tx.setRollbackOnly();
        });
        assertThat(users.count()).isZero(); assertThat(pending.count()).isEqualTo(1);
        verifyNoInteractions(mailSender);
        auth.verify(EMAIL, code); assertThat(users.count()).isEqualTo(1);
    }

    @Test void smtpFailureLeavesOnlyPendingAndCanRetry() {
        doThrow(new MailSendException("simulated")).when(mailSender).send(any(SimpleMailMessage.class));
        assertThatThrownBy(() -> auth.registerCustomer(request())).isInstanceOf(org.springframework.web.server.ResponseStatusException.class); assertThat(users.count()).isZero(); assertThat(pending.count()).isZero();
        reset(mailSender); auth.registerCustomer(request()); auth.verify(EMAIL, lastCode());
        assertThat(users.count()).isEqualTo(1);
    }

    @Test void httpVerificationRequiresCsrf() throws Exception {
        String code = register(); String body = "{\"email\":\"" + EMAIL + "\",\"otp\":\"" + code + "\"}";
        mvc.perform(post("/api/auth/verify-email").contentType("application/json").content(body)).andExpect(status().isForbidden());
        var result = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        var match = Pattern.compile("\"token\":\"([^\"]+)\"").matcher(result.getResponse().getContentAsString());
        assertThat(match.find()).isTrue();
        mvc.perform(post("/api/auth/verify-email").session(session).header("X-CSRF-TOKEN", match.group(1))
                .contentType("application/json").content(body)).andExpect(status().isOk());
        assertThat(users.count()).isEqualTo(1); assertThat(pending.count()).isZero();
    }

    private RegisterRequest request() { return new RegisterRequest(EMAIL, PASSWORD, "OTP Test"); }
    private String register() { auth.registerCustomer(request()); return lastCode(); }
    private User user() { return users.findByEmail(EMAIL).orElseThrow(); }
    private PendingRegistration draft() { return pending.findById(EMAIL).orElseThrow(); }
    private void ageIssue() { var p = draft(); p.setIssuedAt(Instant.now().minusSeconds(61)); pending.saveAndFlush(p); }
    private java.util.List<SimpleMailMessage> mails() {
        var capture = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, atLeastOnce()).send(capture.capture()); return capture.getAllValues();
    }
    private String lastCode() {
        var sent = mails(); var matcher = Pattern.compile("[0-9]{6}").matcher(sent.get(sent.size() - 1).getText());
        assertThat(matcher.find()).isTrue(); return matcher.group();
    }
    private String wrong(String code) { return code.equals("000000") ? "111111" : "000000"; }
}
