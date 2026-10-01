package com.example.flowershop.dto.product;

import java.util.List;

/**
 * DTO phân trang danh sách card sản phẩm.
 */
public record ProductPageResponse(
        List<ProductCardResponse> content,
        int page,
        long totalElements,
        int totalPages
) {}
