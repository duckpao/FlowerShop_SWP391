package com.example.flowershop.service;

import com.example.flowershop.dto.auth.*;
import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.UserStatus;
import com.example.flowershop.repository.*;
import com.example.flowershop.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Service
public class TokenAuthService {
    private final UserRepository users;
    private final AuthSessionRepository sessions;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final com.example.flowershop.security.CaptchaService captcha;
    public TokenAuthService(UserRepository users, AuthSessionRepository sessions, PasswordEncoder encoder, JwtService jwt, com.example.flowershop.security.CaptchaService captcha) {
        this.captcha=captcha; this.users=users; this.sessions=sessions; this.encoder=encoder; this.jwt=jwt;
    }
    public record Tokens(String accessToken, long expiresIn, CurrentUser user, String refreshToken) {
        @Override public String toString() { return "Tokens[redacted]"; }
    }
    @Transactional
    public Tokens login(LoginRequest request) {
        if(captcha.required(request.email())) captcha.verify(request.email(),"login",request.captchaId(),request.captchaAnswer());
        String id=users.findUserIdByEmail(request.email().strip().toLowerCase(Locale.ROOT)).orElse(null);
        User user=id == null ? null : users.findByIdForUpdate(id).orElse(null);
        if (user == null || user.getPasswordHash() == null) {
            captcha.failed(request.email());
            throw invalid();
        }
        String hash = user.getPasswordHash();
        boolean matches=request.password().getBytes(StandardCharsets.UTF_8).length <=72 && encoder.matches(request.password(), hash);
        if (!matches || !eligible(user)) { captcha.failed(request.email()); throw invalid(); }
        captcha.clear(request.email());
        AuthSession session=new AuthSession(); session.setId(UUID.randomUUID().toString());
        session.setUserId(user.getId()); session.setExpiresAt(Instant.now().plusSeconds(604800));
        return rotate(user, session);
    }
    @Transactional(noRollbackFor=BadCredentialsException.class)
    public Tokens refresh(String raw) {
        var claims=jwt.decode(raw,"refresh");
        User user=users.findByIdForUpdate(claims.getSubject()).orElseThrow(TokenAuthService::invalid);
        AuthSession session=sessions.findLocked(claims.getClaimAsString("sid")).orElseThrow(TokenAuthService::invalid);
        if (!session.getUserId().equals(user.getId()) || session.isRevoked()
                || !Instant.now().isBefore(session.getExpiresAt()) || !eligible(user)) throw invalid();
        if (!MessageDigest.isEqual(hash(raw).getBytes(StandardCharsets.US_ASCII),session.getRefreshHash().getBytes(StandardCharsets.US_ASCII))) {
            session.setRevoked(true); // Replay of an already rotated refresh token revokes its family.
            throw invalid();
        }
        return rotate(user,session);
    }
    private Tokens rotate(User user, AuthSession session) {
        String refresh=jwt.issue(user.getId(),session.getId(),"refresh",session.getExpiresAt());
        session.setRefreshHash(hash(refresh)); sessions.saveAndFlush(session);
        return new Tokens(jwt.issue(user.getId(),session.getId(),"access",Instant.now().plusSeconds(600)),600,CurrentUser.from(user),refresh);
    }
    @Transactional(readOnly=true)
    public CurrentUser authenticate(String raw) {
        var claims=jwt.decode(raw,"access");
        AuthSession session=sessions.findById(claims.getClaimAsString("sid")).orElseThrow(TokenAuthService::invalid);
        User user=users.findById(claims.getSubject()).orElseThrow(TokenAuthService::invalid);
        if (!session.getUserId().equals(user.getId()) || session.isRevoked()
                || !Instant.now().isBefore(session.getExpiresAt()) || !eligible(user)) throw invalid();
        return CurrentUser.from(user); // Current role from DB, not a client-supplied claim.
    }
    @Transactional
    public void logout(String raw) {
        if (raw == null) return;
        var claims=jwt.decode(raw,"refresh");
        users.findByIdForUpdate(claims.getSubject());
        sessions.findLocked(claims.getClaimAsString("sid")).ifPresent(session -> {
            if (session.getUserId().equals(claims.getSubject())) session.setRevoked(true);
        });
    }
    private static boolean eligible(User u) { return u != null
            && u.getRole() != com.example.flowershop.entity.enums.UserRole.DELIVERY
            && u.getStatus()==UserStatus.ACTIVE && Boolean.TRUE.equals(u.getIsEmailVerified()); }
    private static BadCredentialsException invalid() { return new BadCredentialsException("Thông tin đăng nhập hoặc phiên không hợp lệ."); }
    private static String hash(String s) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
