package com.example.flowershop.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO dữ liệu gửi lên từ Khách hàng để viết hoặc cập nhật đánh giá sản phẩm.
 */
public record ReviewRequest(
        @NotNull @Min(1) @Max(5) Integer rating,
        @Size(max = 2000) String comment
) {}
