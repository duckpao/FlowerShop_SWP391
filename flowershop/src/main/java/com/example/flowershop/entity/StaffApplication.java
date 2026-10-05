package com.example.flowershop.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "Staff_Applications", uniqueConstraints = @UniqueConstraint(columnNames = { "shop_id", "user_id" }))
@Getter
@Setter
@NoArgsConstructor
public class StaffApplication {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "introduction", columnDefinition = "TEXT")
    private String introduction;

    @Column(name = "status", length = 16)
    private String status;

    @Column(name = "submitted_at")
    private Instant submittedAt;
}