package com.example.flowershop.service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;

@Component @RequiredArgsConstructor @Slf4j
public class VerificationMailSender {
    private final JavaMailSender mailSender;
    @Value("${app.mail.from}") private String from;
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void send(AuthMailEvent event) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(from);
        mail.setTo(event.email());
        switch (event.kind()) {
            case REGISTER_OTP -> {
                mail.setSubject("FlowerShop - Mã xác thực đăng ký");
                mail.setText("Mã OTP đăng ký của bạn: " + event.code()
                        + "\nMã có hiệu lực 5 phút, chỉ dùng một lần. Không chia sẻ mã này."
                        + "\nNếu không phải bạn yêu cầu đăng ký, hãy bỏ qua thư này.");
            }
            case RESET_OTP -> {
                mail.setSubject("FlowerShop - Mã đặt lại mật khẩu");
                mail.setText("Mã OTP đặt lại mật khẩu của bạn: " + event.code()
                        + "\nMã có hiệu lực 5 phút, chỉ dùng một lần. Không chia sẻ mã này."
                        + "\nNếu không phải bạn yêu cầu, hãy bỏ qua thư này. Mật khẩu chưa thay đổi.");
            }
            case WELCOME -> {
                mail.setSubject("FlowerShop - Đăng ký thành công");
                mail.setText("Bạn đã xác thực email và hoàn tất đăng ký FlowerShop. Chào mừng bạn!");
            }
            case PASSWORD_CHANGED -> {
                mail.setSubject("FlowerShop - Mật khẩu đã được thay đổi");
                mail.setText("Mật khẩu FlowerShop của bạn vừa được đặt lại. Nếu không phải bạn thực hiện, hãy liên hệ hỗ trợ.");
            }
        }
        try { mailSender.send(mail); }
        catch (MailException exception) {
            String reason = exception instanceof MailAuthenticationException
                    ? "SMTP authentication failed: check MAIL_USERNAME and Google App Password in the backend terminal."
                    : "SMTP delivery failed: check SMTP host/port, network and sender configuration.";
            log.warn("Auth email delivery failed for user {}, kind {}. {}", event.userId(), event.kind(), reason);
        }
    }
}
