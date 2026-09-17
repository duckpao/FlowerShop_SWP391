package com.example.flowershop.dto.cart;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CartItemResponse {

    private String id;
    private ProductSummary product;
    private Integer quantity;
    private BigDecimal itemTotal; // price * quantity

    @Data
    @Builder
    public static class ProductSummary {
        private String id;
        private String name;
        private BigDecimal price;
        private String images; // JSON string
        private Integer stock;
        private String shopId;
        private String shopName;
    }
}

