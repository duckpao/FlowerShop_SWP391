package com.example.flowershop.service;

import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.UserStatus;
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
import java.util.Locale;
import java.util.UUID;
import static com.example.flowershop.entity.OtpChallenge.Purpose.*;
import static com.example.flowershop.service.AuthMailEvent.Kind.*;

@Service @RequiredArgsConstructor
public class OtpService {
    private static final int CAPTCHA_THRESHOLD = 6;
    private static final SecureRandom RANDOM = new SecureRandom();
    private final UserRepository users;
    private final OtpChallengeRepository challenges;
    private final PasswordEncoder encoder;
    private final ApplicationEventPublisher events;
    private final AuthSessionRepository sessions;
    private final com.example.flowershop.security.CaptchaService captcha;

    @Transactional
    public void forgotPassword(String email) {forgotPassword(email,null,null);}
    @Transactional
    public void forgotPassword(String email,String captchaId,String captchaAnswer) {
        users.findUserIdByEmail(normalize(email)).ifPresent(id -> issue(id, RESET_PASSWORD, captchaId, captchaAnswer));
    }

    private void issue(String userId, OtpChallenge.Purpose purpose,String captchaId,String captchaAnswer) {
        User user = users.findByIdForUpdate(userId).orElse(null);
        if (!eligible(user, purpose)) return;
        Instant now = Instant.now();
        OtpChallenge challenge = challenges.findLocked(userId, purpose).orElseGet(() -> {
            OtpChallenge fresh = new OtpChallenge();
            fresh.setId(UUID.randomUUID().toString());
            fresh.setUserId(userId);
            fresh.setPurpose(purpose);
            fresh.setWindowStart(now);
            return fresh;
        });
        if(challenge.getUsedAt() != null) challenge.setFailedAttempts(0);
        if(challenge.getFailedAttempts()>=CAPTCHA_THRESHOLD){
            captcha.verify(user.getEmail(),"reset",captchaId,captchaAnswer);
        }
        
        // Resends must not reset failed guesses. Counters persist across restarts.
        if (!now.isBefore(challenge.getWindowStart().plusSeconds(900))) {
            challenge.setWindowStart(now);
            challenge.setSendCount(0);
        }
        
        String code = String.format(Locale.ROOT, "%06d", RANDOM.nextInt(1_000_000));
        while (challenge.getCodeHash() != null && encoder.matches(boundCode(challenge, code), challenge.getCodeHash())) {
            code = String.format(Locale.ROOT, "%06d", RANDOM.nextInt(1_000_000));
        }
        challenge.setCodeHash(encoder.encode(boundCode(challenge, code)));
        challenge.setIssuedAt(now);
        challenge.setExpiresAt(now.plusSeconds(300));
        challenge.setUsedAt(null);
        challenge.setSendCount(challenge.getSendCount() + 1);
        challenges.save(challenge);
        events.publishEvent(new AuthMailEvent(userId, user.getEmail(), purpose == REGISTER ? REGISTER_OTP : RESET_OTP, code));
    }

    @Transactional(noRollbackFor = InvalidOtpException.class)
    public void resetPassword(String email, String code, String password, String confirmation) { resetPassword(email,code,password,confirmation,null,null); }

    @Transactional(noRollbackFor = InvalidOtpException.class)
    public void resetPassword(String email, String code, String password, String confirmation, String captchaId, String captchaAnswer) {
        if (!password.equals(confirmation)) throw new IllegalArgumentException("Mật khẩu xác nhận không khớp.");
        if (password.length() < 15 || password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException("Mật khẩu tối thiểu 15 ký tự và tối đa 72 byte UTF-8.");
        User user = lockUser(email);
        consume(user, RESET_PASSWORD, code, captchaId, captchaAnswer);
        user.setPasswordHash(encoder.encode(password));
        sessions.revokeAll(user.getId());
        user.setLastModifyBy(user.getId());
        events.publishEvent(new AuthMailEvent(user.getId(), user.getEmail(), PASSWORD_CHANGED, null));
    }

    private User lockUser(String email) {
        String id = users.findUserIdByEmail(normalize(email)).orElseThrow(InvalidOtpException::new);
        return users.findByIdForUpdate(id).orElseThrow(InvalidOtpException::new);
    }
    private void consume(User user, OtpChallenge.Purpose purpose, String code, String captchaId, String captchaAnswer) {
        if (!eligible(user, purpose)) throw new InvalidOtpException();
        OtpChallenge challenge = challenges.findLocked(user.getId(), purpose).orElseThrow(InvalidOtpException::new);
        if(challenge.getFailedAttempts() >= CAPTCHA_THRESHOLD) captcha.verify(user.getEmail(),"reset",captchaId,captchaAnswer);
        if (challenge.getUsedAt() != null || !Instant.now().isBefore(challenge.getExpiresAt())
                ) throw new InvalidOtpException();
        if (!encoder.matches(boundCode(challenge, code), challenge.getCodeHash())) {
            challenge.setFailedAttempts(Math.min(CAPTCHA_THRESHOLD, challenge.getFailedAttempts() + 1));
            if(challenge.getFailedAttempts() >= CAPTCHA_THRESHOLD) throw new com.example.flowershop.exception.CaptchaRequiredException();
            throw new InvalidOtpException();
        }
        challenge.setUsedAt(Instant.now());
    }
    private static boolean eligible(User user, OtpChallenge.Purpose purpose) {
        if (user == null || user.getStatus() != UserStatus.ACTIVE) return false;
        boolean verified = Boolean.TRUE.equals(user.getIsEmailVerified());
        return purpose == REGISTER ? !verified : verified && user.getPasswordHash() != null;
    }
    private static String boundCode(OtpChallenge c, String code) {
        return c.getUserId() + ":" + c.getPurpose().name() + ":" + code;
    }
    private static String normalize(String email) { return email.strip().toLowerCase(Locale.ROOT); }
}
