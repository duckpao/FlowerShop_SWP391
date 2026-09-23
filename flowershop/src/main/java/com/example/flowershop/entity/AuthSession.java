package com.example.flowershop.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "Auth_Sessions") @Getter @Setter @NoArgsConstructor
public class AuthSession {
    @Id @Column(length = 36) private String id;
    @Column(name = "user_id", nullable = false, length = 36) private String userId;
    @Column(name = "refresh_hash", nullable = false, length = 64) private String refreshHash;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(nullable = false) private boolean revoked;
}
