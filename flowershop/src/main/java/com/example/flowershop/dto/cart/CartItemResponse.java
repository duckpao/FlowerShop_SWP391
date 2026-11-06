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
        private java.util.List<String> images;
        private String image;
        private Integer stock;
        private String shopId;
        private String shopName;
    }
}

