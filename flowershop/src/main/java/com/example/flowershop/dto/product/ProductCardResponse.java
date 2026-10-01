package com.example.flowershop.dto.product;

import com.example.flowershop.entity.enums.ProductStatus;
import java.math.BigDecimal;

/**
 * DTO đại diện cho card hiển thị sản phẩm trong catalog, tìm kiếm và danh sách yêu thích.
 */
public record ProductCardResponse(
        String id,
        String name,
        BigDecimal price,
        Integer stock,
        ProductStatus status,
        String shopId,
        String shopName,
        String categoryId,
        String categoryName,
        String imageUrl,
        double rating,
        long reviewCount,
        boolean available,
        boolean adminHidden
) {}

