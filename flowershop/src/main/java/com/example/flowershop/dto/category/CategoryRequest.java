package com.example.flowershop.dto.category;

import com.example.flowershop.entity.enums.CategoryStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO dữ liệu Admin gửi lên để tạo hoặc cập nhật danh mục hệ thống.
 */
public record CategoryRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 1000) String description,
        @NotNull CategoryStatus status
) {}
