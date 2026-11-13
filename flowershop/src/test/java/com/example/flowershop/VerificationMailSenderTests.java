package com.example.flowershop;

import com.example.flowershop.service.AuthMailEvent;
import com.example.flowershop.service.AuthMailEvent.Kind;
import com.example.flowershop.service.VerificationMailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Mọi email tài khoản: OTP đăng ký / đặt lại, chào mừng, đổi mật khẩu, đơn ứng tuyển và lời mời nhân viên. */
class VerificationMailSenderTests {

    private JavaMailSender smtp;
    private VerificationMailSender sender;

    @BeforeEach
    void setUp() {
        smtp = mock(JavaMailSender.class);
        sender = new VerificationMailSender(smtp);
        ReflectionTestUtils.setField(sender, "from", "noreply@flowershop.test");
        ReflectionTestUtils.setField(sender, "frontendBaseUrl", "http://app.test/");
    }

    private SimpleMailMessage sentAfterCommit(Kind kind, String code) {
        sender.send(new AuthMailEvent("u1", "khach@example.test", kind, code));
        var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(smtp).send(captor.capture());
        return captor.getValue();
    }

    @Test
    void registerOtpIsSentBeforeCommitWithCodeAndFiveMinuteNotice() {
        sender.sendOtp(new AuthMailEvent("u1", "khach@example.test", Kind.REGISTER_OTP, "123456"));
        var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(smtp).send(captor.capture());
        var mail = captor.getValue();
        assertEquals("FlowerShop - Mã xác thực đăng ký", mail.getSubject());
        assertArrayEquals(new String[]{"khach@example.test"}, mail.getTo());
        assertEquals("noreply@flowershop.test", mail.getFrom());
        assertTrue(mail.getText().contains("123456"));
        assertTrue(mail.getText().contains("5 phút"));
    }

    @Test
    void resetOtpIsSentBeforeCommit() {
        sender.sendOtp(new AuthMailEvent("u1", "khach@example.test", Kind.RESET_OTP, "654321"));
        var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(smtp).send(captor.capture());
        assertEquals("FlowerShop - Mã đặt lại mật khẩu", captor.getValue().getSubject());
        assertTrue(captor.getValue().getText().contains("654321"));
    }

    @Test
    void otpIsNeverSentTwiceBecauseTheAfterCommitHandlerIgnoresIt() {
        sender.send(new AuthMailEvent("u1", "khach@example.test", Kind.REGISTER_OTP, "123456"));
        sender.send(new AuthMailEvent("u1", "khach@example.test", Kind.RESET_OTP, "123456"));
        verifyNoInteractions(smtp);
    }

    @Test
    void nonOtpMailsAreNotSentBeforeCommit() {
        sender.sendOtp(new AuthMailEvent("u1", "khach@example.test", Kind.WELCOME, null));
        verifyNoInteractions(smtp);
    }

    @Test
    void welcomeMailIsSentAfterCommit() {
        var mail = sentAfterCommit(Kind.WELCOME, null);
        assertEquals("FlowerShop - Đăng ký thành công", mail.getSubject());
    }

    @Test
    void passwordChangedMailIsSentAfterCommit() {
        var mail = sentAfterCommit(Kind.PASSWORD_CHANGED, null);
        assertEquals("FlowerShop - Mật khẩu đã được thay đổi", mail.getSubject());
        assertTrue(mail.getText().contains("liên hệ hỗ trợ"));
    }

    @Test
    void staffApplicationMailsNameTheShop() {
        assertTrue(sentAfterCommit(Kind.STAFF_APPLICATION_NEW, "Hoa Xinh").getText().contains("Hoa Xinh"));
        reset(smtp);
        var approved = sentAfterCommit(Kind.STAFF_APPLICATION_APPROVED, "Hoa Xinh");
        assertEquals("FlowerShop - Đơn ứng tuyển đã được duyệt", approved.getSubject());
        assertTrue(approved.getText().contains("Hoa Xinh"));
        reset(smtp);
        var rejected = sentAfterCommit(Kind.STAFF_APPLICATION_REJECTED, "Hoa Xinh");
        assertEquals("FlowerShop - Kết quả ứng tuyển", rejected.getSubject());
        assertTrue(rejected.getText().contains("vẫn là Customer"));
    }

    @Test
    void staffInvitationCarriesAnEncodedAcceptLinkWithCodeAndEmail() {
        var mail = sentAfterCommit(Kind.STAFF_INVITATION, "Hoa Xinh: AB+12/x");
        assertEquals("FlowerShop - Lời mời làm nhân viên cửa hàng", mail.getSubject());
        assertTrue(mail.getText().contains("http://app.test/invitations/accept?code=AB%2B12%2Fx&email=khach%40example.test"),
                "trailing slash of the base URL must be trimmed and parameters URL-encoded: " + mail.getText());
        assertTrue(mail.getText().contains("24 giờ"));
    }

    @Test
    void smtpFailureOnOtpBlocksTheRequestWith503SoTheUserKnows() {
        doThrow(new MailSendException("smtp down")).when(smtp).send(any(SimpleMailMessage.class));
        var e = assertThrows(ResponseStatusException.class,
                () -> sender.sendOtp(new AuthMailEvent("u1", "khach@example.test", Kind.REGISTER_OTP, "123456")));
        assertEquals(503, e.getStatusCode().value());
        assertTrue(e.getReason().contains("Chưa gửi được email OTP"));
    }

    @Test
    void smtpFailureOnOtherMailsNeverBreaksTheBusinessAction() {
        doThrow(new MailSendException("smtp down")).when(smtp).send(any(SimpleMailMessage.class));
        assertDoesNotThrow(() -> sender.send(new AuthMailEvent("u1", "khach@example.test", Kind.WELCOME, null)));
    }

    @Test
    void toStringNeverLeaksTheOtpOrEmail() {
        var text = new AuthMailEvent("u1", "khach@example.test", Kind.REGISTER_OTP, "123456").toString();
        assertFalse(text.contains("123456"));
        assertFalse(text.contains("khach@example.test"));
    }
}
