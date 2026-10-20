package com.example.flowershop.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Shop_Product_Mails", uniqueConstraints = @UniqueConstraint(
        name = "uk_shop_product_mail", columnNames = {"product_id", "user_id"}))
@Getter @Setter @NoArgsConstructor
public class ShopProductMail {
    @Id @Column(length = 36) private String id;
    @Column(name = "product_id", nullable = false, length = 36) private String productId;
    @Column(name = "shop_id", nullable = false, length = 36) private String shopId;
    @Column(name = "user_id", nullable = false, length = 36) private String userId;
    @Column(nullable = false, length = 16) private String status = "PENDING";
    @Column(nullable = false) private int attempts;
    @Column(name = "next_attempt_at", nullable = false) private LocalDateTime nextAttemptAt;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "sent_at") private LocalDateTime sentAt;
}
