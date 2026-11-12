package com.example.flowershop.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
/** Immutable quota ledger: deliberately no product FK, so deleting a product never refunds a slot. */
@Entity @Table(name="Shop_Product_Postings") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ShopProductPosting {
    @Id @Column(length=36) private String id;
    @Column(name="shop_id",nullable=false,length=36) private String shopId;
    @Column(name="posted_at",nullable=false) private LocalDateTime postedAt;
}
