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

    @Transactional
    public void registerCustomer(RegisterRequest request) {
        register(request,null,null);
    }

    @Transactional
    public void registerShop(com.example.flowershop.dto.auth.ShopRegisterRequest request) {
        register(request.account(),request.shopName().strip(),request.description().strip());
    }

    private void register(RegisterRequest request,String shopName,String description) {
        String email = normalize(request.email());
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException("Mật khẩu không được vượt quá 72 byte UTF-8.");
        if (users.existsByEmail(email)) return;
        PendingRegistration registration = pending.findLocked(email).orElseGet(() -> {
            PendingRegistration fresh = new PendingRegistration();
            fresh.setEmail(email);
            fresh.setWindowStart(Instant.now());
            return fresh;
        });
        if (!canIssue(registration)) return;
        // Replace submitted details only when issuing a new code: old codes cannot
        // activate a different password/name from a subsequent registration request.
        registration.setFullName(request.fullName().strip());
        registration.setShopName(shopName);
        registration.setShopDescription(description);
        registration.setPasswordHash(encoder.encode(request.password()));
        issue(registration);
    }

    @Transactional
    public void resend(String email) {
        String normalized = normalize(email);
        if (users.existsByEmail(normalized)) return;
        pending.findLocked(normalized).ifPresent(registration -> {
            if (canIssue(registration)) issue(registration);
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
    public void verify(String email, String code) {
        PendingRegistration p = pending.findLocked(normalize(email)).orElseThrow(InvalidOtpException::new);
        if (users.existsByEmail(p.getEmail()) || !Instant.now().isBefore(p.getExpiresAt())
                || p.getFailedAttempts() >= 5) throw new InvalidOtpException();
        if (!encoder.matches(code, p.getOtpHash())) {
            p.setFailedAttempts(p.getFailedAttempts() + 1);
            throw new InvalidOtpException();
        }
        String id = UUID.randomUUID().toString();
        User user = User.builder().id(id).email(p.getEmail()).fullName(p.getFullName())
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
