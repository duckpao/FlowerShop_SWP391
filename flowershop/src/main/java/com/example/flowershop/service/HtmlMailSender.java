package com.example.flowershop.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/** Gửi mail HTML cho khách (biên lai, thông báo đơn, nhắc dịp) và các hàm dựng HTML dùng chung. */
@Component
public class HtmlMailSender {

    private static final Logger log = LoggerFactory.getLogger(HtmlMailSender.class);
    private static final Locale VI = Locale.forLanguageTag("vi-VN");

    private final JavaMailSender mailSender;
    private final String from;
    private final String frontendBaseUrl;

    public HtmlMailSender(JavaMailSender mailSender,
                          @Value("${app.mail.from}") String from,
                          @Value("${app.frontend-base-url}") String frontendBaseUrl) {
        this.mailSender = mailSender;
        this.from = from;
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
    }

    /** @return true nếu SMTP nhận thư; lỗi chỉ được ghi log, không ném ra ngoài để không ảnh hưởng nghiệp vụ. */
    public boolean send(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            String reason = e instanceof MailAuthenticationException
                    ? "SMTP authentication failed: check MAIL_USERNAME and Google App Password."
                    : "SMTP delivery failed: " + e.getMessage();
            log.warn("HTML email to {} failed ({}). {}", to, subject, reason);
            return false;
        }
    }

    public String url(String path) {
        return frontendBaseUrl + path;
    }

    public static String esc(String text) {
        return text == null ? "" : HtmlUtils.htmlEscape(text);
    }

    public static String money(BigDecimal amount) {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount;
        return NumberFormat.getNumberInstance(VI).format(value.longValue()) + " ₫";
    }

    /** Khung thư chung: tiêu đề, nội dung HTML, nút hành động tùy chọn. */
    public static String layout(String heading, String bodyHtml, String buttonLabel, String buttonUrl) {
        String button = buttonLabel == null ? "" :
                "<p style=\"margin:24px 0 0\"><a href=\"" + esc(buttonUrl) + "\" style=\"background:#e11d48;color:#fff;"
                        + "text-decoration:none;padding:10px 20px;border-radius:8px;font-weight:600;display:inline-block\">"
                        + esc(buttonLabel) + "</a></p>";
        return "<div style=\"font-family:Arial,Helvetica,sans-serif;background:#f6f7f9;padding:24px\">"
                + "<div style=\"max-width:600px;margin:0 auto;background:#fff;border-radius:12px;padding:28px;color:#1f2937\">"
                + "<p style=\"margin:0 0 4px;color:#e11d48;font-weight:700\">🌸 FlowerShop</p>"
                + "<h2 style=\"margin:0 0 16px;font-size:20px\">" + esc(heading) + "</h2>"
                + bodyHtml + button
                + "<p style=\"margin:28px 0 0;font-size:12px;color:#9ca3af\">Đây là email tự động từ FlowerShop, vui lòng không trả lời thư này.</p>"
                + "</div></div>";
    }
}
