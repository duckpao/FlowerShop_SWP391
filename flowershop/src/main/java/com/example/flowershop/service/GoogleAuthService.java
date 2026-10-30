package com.example.flowershop.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

/** Verifies Google ID tokens (signature via Google JWKS, issuer, audience). */
@Service
public class GoogleAuthService {
    private static final String JWKS_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private final NimbusJwtDecoder decoder;
    private final String clientId;

    public record GoogleUser(String sub, String email, boolean emailVerified, String name, String picture) {}

    public GoogleAuthService(@Value("${app.google.client-id:}") String clientId) {
        this.clientId = clientId;
        this.decoder = NimbusJwtDecoder.withJwkSetUri(JWKS_URI).build();
        this.decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(),
                jwt -> {
                    String iss = jwt.getIssuer() == null ? "" : jwt.getIssuer().toString();
                    boolean okIssuer = iss.equals("https://accounts.google.com") || iss.equals("accounts.google.com");
                    boolean okAudience = jwt.getAudience() != null && jwt.getAudience().contains(clientId);
                    return okIssuer && okAudience
                            ? OAuth2TokenValidatorResult.success()
                            : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
                }));
    }

    public GoogleUser verify(String idToken) {
        if (clientId == null || clientId.isBlank())
            throw new IllegalStateException("Chưa cấu hình app.google.client-id.");
        Jwt jwt;
        try {
            jwt = decoder.decode(idToken);
        } catch (JwtException e) {
            throw new BadCredentialsException("Token Google không hợp lệ.");
        }
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) throw new BadCredentialsException("Token Google không hợp lệ.");
        return new GoogleUser(jwt.getSubject(), email,
                Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified")),
                jwt.getClaimAsString("name"), jwt.getClaimAsString("picture"));
    }
}
