package com.example.flowershop.dto.product;

import com.example.flowershop.entity.ProductVideo;

/**
 * DTO dữ liệu trả về cho video của sản phẩm.
 */
public record ProductVideoResponse(
        String id,
        String videoUrl,
        String title,
        String description,
        int displayOrder
) {
    public static ProductVideoResponse from(ProductVideo v) {
        return new ProductVideoResponse(
                v.getId(),
                v.getVideoUrl(),
                v.getTitle(),
                v.getDescription(),
                v.getDisplayOrder() != null ? v.getDisplayOrder() : 0
        );
    }
}

