package com.example.flowershop.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
@Entity @Table(name="Staff_Invitations",uniqueConstraints=@UniqueConstraint(columnNames={"shop_id","email"}))
@Getter @Setter @NoArgsConstructor
public class StaffInvitation {
    @Id @Column(length=36) private String id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="shop_id",nullable=false) private Shop shop;
    @Column(nullable=false,length=255) private String email;
    @Column(name="token_hash",nullable=false,length=64) private String tokenHash;
    @Column(name="expires_at",nullable=false) private Instant expiresAt;
    @Column(name="issued_at",nullable=false) private Instant issuedAt;
    @Column(name="window_start",nullable=false) private Instant windowStart;
    @Column(name="send_count",nullable=false) private int sendCount;
    @Column(nullable=false,length=16) private String status;
}
