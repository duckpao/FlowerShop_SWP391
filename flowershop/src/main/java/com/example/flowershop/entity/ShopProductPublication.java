package com.example.flowershop.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "Shop_Product_Publications")
@Getter @Setter @NoArgsConstructor
public class ShopProductPublication {
    @Id @Column(name = "product_id", length = 36)
    private String productId;
    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;
}
