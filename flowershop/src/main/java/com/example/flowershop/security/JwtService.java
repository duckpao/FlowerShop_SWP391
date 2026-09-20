package com.example.flowershop.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

@Component
public class JwtService {
    private final JwtEncoder encoder;
    private final NimbusJwtDecoder decoder;
    public JwtService(@Value("${app.jwt.secret:}") String configuredSecret,
                      @Value("${app.jwt.allow-ephemeral-key:true}") boolean allowEphemeral) {
        byte[] bytes;
        if (configuredSecret.isBlank()) {
            if (!allowEphemeral) throw new IllegalStateException("JWT_SECRET is required");
            bytes = new byte[32]; new SecureRandom().nextBytes(bytes);
        } else bytes = Base64.getDecoder().decode(configuredSecret);
        if (bytes.length < 32) throw new IllegalArgumentException("JWT_SECRET must contain at least 32 random bytes, Base64 encoded");
        var key = new SecretKeySpec(bytes, "HmacSHA256");
        encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("flowershop"));
    }
    public String issue(String userId, String sessionId, String kind, Instant expires) {
        var claims = JwtClaimsSet.builder().issuer("flowershop").subject(userId)
                .audience(List.of("flowershop-api")).issuedAt(Instant.now()).expiresAt(expires)
                .id(UUID.randomUUID().toString()).claim("sid", sessionId).claim("kind", kind).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
    public Jwt decode(String token, String kind) {
        if (token == null || token.length() > 4096) throw new BadJwtException("Invalid token");
        Jwt jwt = decoder.decode(token);
        if (!kind.equals(jwt.getClaimAsString("kind")) || !jwt.getAudience().contains("flowershop-api")
                || jwt.getSubject() == null || jwt.getClaimAsString("sid") == null
                || jwt.getExpiresAt() == null || !Instant.now().isBefore(jwt.getExpiresAt()))
            throw new BadJwtException("Invalid token claims");
        return jwt;
    }
}
