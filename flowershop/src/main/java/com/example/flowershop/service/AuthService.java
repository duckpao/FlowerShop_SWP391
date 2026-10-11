package com.example.flowershop.service;

import com.example.flowershop.dto.auth.RegisterRequest;
import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.exception.InvalidOtpException;
import com.example.flowershop.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

@Service @RequiredArgsConstructor
public class AuthService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final UserRepository users;
    private final PendingRegistrationRepository pending;
    private final PasswordEncoder encoder;
    private final ApplicationEventPublisher events;
    private final ShopRepository shops;
    private final CloudinaryService cloudinary;
    private final com.example.flowershop.security.CaptchaService captcha;

    @Transactional
    public void registerCustomer(RegisterRequest request) {
        register(request,null,null);
    }

    @Transactional
    public void registerShop(com.example.flowershop.dto.auth.ShopRegisterRequest request) {
        String name = request.shopName() == null || request.shopName().isBlank()
                ? "Cửa hàng của " + request.account().fullName().strip() : request.shopName().strip();
        register(request.account(), name, request.description() == null ? "" : request.description().strip());
    }

    private void register(RegisterRequest request,String shopName,String description) {
        String email = normalize(request.email());
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException("Mật khẩu không được vượt quá 72 byte UTF-8.");
        if (users.existsByEmail(email)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT, "Email đã được đăng ký.");
        }
        PendingRegistration registration = pending.findLocked(email).orElseGet(() -> {
            PendingRegistration fresh = new PendingRegistration();
            fresh.setEmail(email);
            fresh.setWindowStart(Instant.now());
            return fresh;
        });
        if (!canIssue(registration)) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,
                "Bạn đã yêu cầu quá nhiều mã OTP. Vui lòng chờ 60 giây giữa các lần đăng ký, tối đa 3 mã trong 15 phút.");
        // Replace submitted details only when issuing a new code: old codes cannot
        // activate a different password/name from a subsequent registration request.
        registration.setFullName(request.fullName().strip());
        registration.setShopName(shopName);
        registration.setShopDescription(description);
        registration.setPasswordHash(encoder.encode(request.password()));
        issue(registration);
    }

    @Transactional
    public void resend(String email) {resend(email,null,null);}
    @Transactional
    public void resend(String email,String captchaId,String captchaAnswer) {
        String normalized = normalize(email);
        if (users.existsByEmail(normalized)) return;
        pending.findLocked(normalized).ifPresent(registration -> {
            if(registration.getFailedAttempts()>=5){
                captcha.verify(email,"register",captchaId,captchaAnswer);
                registration.setFailedAttempts(0); registration.setSendCount(0); registration.setIssuedAt(null); registration.setWindowStart(Instant.now());
            }
            issue(registration);
        });
    }

    private boolean canIssue(PendingRegistration p) {
        Instant now = Instant.now();
        if (p.getIssuedAt() != null && now.isBefore(p.getIssuedAt().plusSeconds(60))) return false;
        if (!now.isBefore(p.getWindowStart().plusSeconds(900))) {
            p.setWindowStart(now); p.setFailedAttempts(0); p.setSendCount(0);
        }
        return p.getFailedAttempts() < 5 && p.getSendCount() < 3;
    }

    private void issue(PendingRegistration p) {
        String code;
        do { code = String.format(Locale.ROOT, "%06d", RANDOM.nextInt(1_000_000)); }
        while (p.getOtpHash() != null && encoder.matches(code, p.getOtpHash()));
        p.setOtpHash(encoder.encode(code));
        p.setIssuedAt(Instant.now());
        p.setExpiresAt(p.getIssuedAt().plusSeconds(300));
        p.setSendCount(p.getSendCount() + 1);
        pending.saveAndFlush(p);
        events.publishEvent(new AuthMailEvent("pending-registration", p.getEmail(), AuthMailEvent.Kind.REGISTER_OTP, code));
    }

    @Transactional(noRollbackFor = InvalidOtpException.class)
    public void verify(String email, String code) { verify(email,code,null,null); }

    @Transactional(noRollbackFor = InvalidOtpException.class)
    public void verify(String email, String code, String captchaId, String captchaAnswer) {
        PendingRegistration p = pending.findLocked(normalize(email)).orElseThrow(InvalidOtpException::new);
        if(p.getFailedAttempts() >= 5) captcha.verify(email,"register",captchaId,captchaAnswer);
        if (users.existsByEmail(p.getEmail()) || !Instant.now().isBefore(p.getExpiresAt())
                ) throw new InvalidOtpException();
        if (!encoder.matches(code, p.getOtpHash())) {
            p.setFailedAttempts(Math.min(5, p.getFailedAttempts() + 1));
            if(p.getFailedAttempts() >= 5) throw new com.example.flowershop.exception.CaptchaRequiredException();
            throw new InvalidOtpException();
        }
        String id = UUID.randomUUID().toString();
        String fullName = p.getFullName() == null ? "" : p.getFullName().strip();
        String avatarUrl = cloudinary.uploadDefaultAvatar(fullName);
        User user = User.builder().id(id).email(p.getEmail()).fullName(fullName)
                .avatarUrl(avatarUrl)
                .passwordHash(p.getPasswordHash()).role(p.getShopName()==null ? UserRole.CUSTOMER : UserRole.SHOP)
                .status(UserStatus.ACTIVE).isEmailVerified(true).createdBy(id).build();
        users.saveAndFlush(user);
        if(p.getShopName()!=null) shops.saveAndFlush(Shop.builder().id(UUID.randomUUID().toString()).owner(user)
                .name(p.getShopName()).description(p.getShopDescription()).status(ShopStatus.PENDING).createdBy(id).build());
        pending.delete(p);
        pending.flush();
        events.publishEvent(new AuthMailEvent(id, user.getEmail(), AuthMailEvent.Kind.WELCOME, null));
    }

    private String normalize(String email) { return email.strip().toLowerCase(Locale.ROOT); }
}
