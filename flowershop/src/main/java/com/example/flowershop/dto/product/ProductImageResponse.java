package com.example.flowershop.dto.product;

import com.example.flowershop.entity.ProductImage;

/**
 * DTO dữ liệu trả về cho ảnh của sản phẩm.
 */
public record ProductImageResponse(
        String id,
        String imageUrl,
        boolean isPrimary,
        int displayOrder
) {
    public static ProductImageResponse from(ProductImage img) {
        return new ProductImageResponse(
                img.getId(),
                img.getImageUrl(),
                Boolean.TRUE.equals(img.getIsPrimary()),
                img.getDisplayOrder() != null ? img.getDisplayOrder() : 0
        );
    }
}

