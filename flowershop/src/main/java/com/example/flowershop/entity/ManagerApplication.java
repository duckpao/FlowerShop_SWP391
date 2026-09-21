package com.example.flowershop.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
@Entity @Table(name="Manager_Applications") @Getter @Setter @NoArgsConstructor
public class ManagerApplication {
    @Id @Column(length=36) private String id;
    @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id",nullable=false,unique=true) private User user;
    @Column(name="full_name",nullable=false,length=100) private String fullName;
    @Column(nullable=false,length=20) private String phone;
    @Column(name="shop_name",nullable=false,length=255) private String shopName;
    @Column(nullable=false,length=5000) private String description;
    @Column(name="address_line",nullable=false,length=255) private String addressLine;
    @Column(nullable=false,length=100) private String city;
    @Column(nullable=false,length=100) private String district;
    @Column(nullable=false,length=100) private String ward;
    @Column(nullable=false,length=16) private String status;
    @Column(name="review_note",length=1000) private String reviewNote;
    @Column(name="submitted_at",nullable=false) private Instant submittedAt;
    @Column(name="reviewed_at") private Instant reviewedAt;
    @Column(name="reviewed_by",length=36) private String reviewedBy;
    @Column(name="shop_id",length=36) private String shopId;
}
