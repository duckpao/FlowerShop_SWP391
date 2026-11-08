package com.example.flowershop.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
/** Entitlement history, separate from payment accounting. ADMIN_GRANT does not invent a payment. */
@Entity @Table(name="Shop_Pro_Activations") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ShopProActivation {
    @Id @Column(length=36) private String id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="shop_id",nullable=false) private Shop shop;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="plan_id",nullable=false) private ShopPlan plan;
    @Column(name="duration_months",nullable=false) private int durationMonths;
    @Column(nullable=false,length=20) private String source;
    @Column(name="payment_id",length=36,unique=true) private String paymentId;
    @Column(name="actor_id",length=36) private String actorId;
    @Column(nullable=false,length=1000) private String note;
    @Column(name="starts_at",nullable=false) private LocalDateTime startsAt;
    @Column(name="ends_at",nullable=false) private LocalDateTime endsAt;
    @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
}
