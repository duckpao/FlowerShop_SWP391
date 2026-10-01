package com.example.flowershop.dto.product;

import com.example.flowershop.entity.Product;
import com.example.flowershop.entity.enums.ProductStatus;
import java.math.BigDecimal;
import java.util.List;

/**
 * DTO trả về cho Shop Manager xem chi tiết sản phẩm thuộc shop mình (kèm cờ adminHidden và trạng thái quản trị).
 */
public record ProductResponse(
        String id,
        String shopId,
        String categoryId,
        String name,
        BigDecimal price,
        int stock,
        String description,
        ProductStatus status,
        boolean adminHidden,
        List<String> images
) {
    public static ProductResponse from(Product p, List<String> images) {
        return new ProductResponse(
                p.getId(),
                p.getShop() != null ? p.getShop().getId() : null,
                p.getCategory() != null ? p.getCategory().getId() : null,
                p.getName(),
                p.getPrice(),
                p.getStock(),
                p.getDescription(),
                p.getStatus(),
                p.isAdminHidden(),
                images != null ? images : List.of()
        );
    }
}

