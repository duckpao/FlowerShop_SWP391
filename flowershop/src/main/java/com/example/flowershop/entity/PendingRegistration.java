package com.example.flowershop.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "Pending_Registrations")
@Getter @Setter @NoArgsConstructor
public class PendingRegistration {
    @Column(name="shop_name",length=255) private String shopName;
    @Column(name="shop_description",length=5000) private String shopDescription;
    @Id @Column(length = 255) private String email;
    @Column(name = "full_name", nullable = false, length = 100) private String fullName;
    @Column(name = "password_hash", nullable = false, length = 100) private String passwordHash;
    @Column(name = "otp_hash", nullable = false, length = 100) private String otpHash;
    @Column(name = "issued_at", nullable = false) private Instant issuedAt;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "window_start", nullable = false) private Instant windowStart;
    @Column(name = "failed_attempts", nullable = false) private int failedAttempts;
    @Column(name = "send_count", nullable = false) private int sendCount;
}
