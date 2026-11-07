package com.example.flowershop.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity @Table(name="Shop_Plan_Payments") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ShopPlanPayment {
    @Id @Column(length=36) private String id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="shop_id",nullable=false) private Shop shop;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="plan_id",nullable=false) private ShopPlan plan;
    @Column(name="duration_months",nullable=false) private int durationMonths;
    @Column(nullable=false,precision=12,scale=2) private BigDecimal amount;
    @Column(name="invoice_number",nullable=false,unique=true,length=100) private String invoiceNumber;
    @Column(nullable=false,length=20) private String status;
    @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
    @Column(name="confirmed_at") private LocalDateTime confirmedAt;
    @Column(name="last_checked_at") private LocalDateTime lastCheckedAt;
    @Column(name="transaction_no",length=100) private String transactionNo;
}
