package com.example.flowershop.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity @Table(name="Shop_Subscriptions") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ShopSubscription {
    @Id @Column(length=36) private String id;
    @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="shop_id",nullable=false,unique=true) private Shop shop;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="plan_id",nullable=false) private ShopPlan plan;
    @Column(name="started_at",nullable=false) private LocalDateTime startedAt;
    @Column(name="ends_at",nullable=false) private LocalDateTime endsAt;
}
