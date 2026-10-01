package com.example.flowershop.dto.category;

import com.example.flowershop.entity.Category;

/**
 * DTO lựa chọn danh mục rút gọn (id, tên) cho Shop Manager chọn khi tạo/sửa sản phẩm.
 */
public record CategoryOptionResponse(
        String id,
        String name
) {
    public static CategoryOptionResponse from(Category c) {
        return new CategoryOptionResponse(c.getId(), c.getName());
    }
}

