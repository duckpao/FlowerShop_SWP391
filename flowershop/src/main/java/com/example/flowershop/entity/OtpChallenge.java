package com.example.flowershop.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "Otp_Challenges", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "purpose"}))
@Getter @Setter @NoArgsConstructor
public class OtpChallenge {
    @Id @Column(length = 36) private String id;
    @Column(name = "user_id", nullable = false, length = 36) private String userId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Purpose purpose;
    @Column(name = "code_hash", nullable = false, length = 100) private String codeHash;
    @Column(name = "issued_at", nullable = false) private Instant issuedAt;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "used_at") private Instant usedAt;
    @Column(name = "window_start", nullable = false) private Instant windowStart;
    @Column(name = "failed_attempts", nullable = false) private int failedAttempts;
    @Column(name = "send_count", nullable = false) private int sendCount;
    public enum Purpose { REGISTER, RESET_PASSWORD }
}
