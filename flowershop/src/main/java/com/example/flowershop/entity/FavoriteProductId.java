package com.example.flowershop.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

// Khóa chính ghép (userId, productId) của Favorite_Products; EqualsAndHashCode so sánh theo cả hai trường.
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class FavoriteProductId implements Serializable {
    private String userId;
    private String productId;
}
