package com.example.flowershop.dto.category;

/**
 * DTO đại diện cho danh mục trên trang khám phá công khai kèm số lượng sản phẩm đang bán.
 */
public record CategoryCardResponse(
        String id,
        String name,
        String description,
        long productCount
) {}
