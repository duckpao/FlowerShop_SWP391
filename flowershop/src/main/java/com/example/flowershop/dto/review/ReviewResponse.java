package com.example.flowershop.dto.review;

import com.example.flowershop.entity.ProductReview;
import java.time.LocalDateTime;

/**
 * DTO hiển thị chi tiết một đánh giá sản phẩm.
 * Lưu ý: Tên người đánh giá (reviewerName) được ẩn danh hóa hoặc dùng họ tên, không để lộ email.
 */
public record ReviewResponse(
        String id,
        String productId,
        String reviewerName,
        int rating,
        String comment,
        String shopReply,
        LocalDateTime createdAt
) {
    public static ReviewResponse from(ProductReview r) {
        String name = r.getUser() != null ? r.getUser().getFullName() : null;
        return new ReviewResponse(
                r.getId(),
                r.getProduct() != null ? r.getProduct().getId() : null,
                name == null || name.isBlank() ? "Khách hàng" : name,
                r.getRating() != null ? r.getRating() : 0,
                r.getComment(),
                r.getShopReply(),
                r.getCreatedDate()
        );
    }
}

