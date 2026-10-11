package com.example.flowershop.dto.product;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO chi tiết sản phẩm cho trang công khai, bao gồm danh sách ảnh và điểm đánh giá.
 */
public record ProductDetailResponse(
        String id,
        String name,
        BigDecimal price,
        int stock,
        String description,
        String shopId,
        String shopName,
        String categoryId,
        String categoryName,
        List<String> images,
        Double rating,
        int reviewCount
) {}
