package com.example.flowershop.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
@Entity @Table(name="Shop_Plans") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ShopPlan {
    @Id @Column(length=20) private String id;
    @Column(nullable=false,length=100) private String name;
    @Column(name="duration_months",nullable=false) private int durationMonths;
    @Column(nullable=false,precision=12,scale=2) private BigDecimal price;
    @Column(name="product_limit") private Integer productLimit;
    @Column(name="featured_limit",nullable=false) private int featuredLimit;
    @Column(nullable=false) private boolean enabled;
}
