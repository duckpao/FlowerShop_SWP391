package com.example.flowershop.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity @Table(name="Featured_Products") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FeaturedProduct {
    @Id @Column(name="product_id",length=36) private String id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="product_id",insertable=false,updatable=false) private Product product;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="shop_id",nullable=false) private Shop shop;
    @Column(name="featured_at",nullable=false) private LocalDateTime featuredAt;
}
