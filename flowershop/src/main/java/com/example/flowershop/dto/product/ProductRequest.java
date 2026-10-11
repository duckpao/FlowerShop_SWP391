package com.example.flowershop.dto.product;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * DTO dữ liệu gửi lên từ Shop Manager để tạo hoặc cập nhật thông tin sản phẩm.
 */
public record ProductRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull @PositiveOrZero BigDecimal price,
        @NotNull @Min(0) Integer stock,
        @Size(max = 5000) String description,
        @NotBlank String categoryId
) {}
