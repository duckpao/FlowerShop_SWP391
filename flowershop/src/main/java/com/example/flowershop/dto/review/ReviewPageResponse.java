package com.example.flowershop.dto.review;

import java.util.List;

/**
 * DTO phân trang danh sách các đánh giá của một sản phẩm.
 */
public record ReviewPageResponse(
        List<ReviewResponse> content,
        int page,
        long totalElements,
        int totalPages
) {}
