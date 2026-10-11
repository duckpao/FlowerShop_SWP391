package com.example.flowershop.dto.category;

import com.example.flowershop.entity.Category;
import com.example.flowershop.entity.enums.CategoryStatus;

/**
 * DTO dữ liệu trả về cho Admin khi xem/quản lý danh mục hệ thống.
 */
public record CategoryResponse(
        String id,
        String name,
        String description,
        CategoryStatus status,
        long productCount
) {
    public static CategoryResponse from(Category c, long productCount) {
        return new CategoryResponse(c.getId(), c.getName(), c.getDescription(), c.getStatus(), productCount);
    }
}

