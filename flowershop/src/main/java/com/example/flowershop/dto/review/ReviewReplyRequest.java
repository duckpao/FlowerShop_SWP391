package com.example.flowershop.dto.review;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO dữ liệu Shop Manager gửi lên để phản hồi đánh giá của khách hàng.
 */
public record ReviewReplyRequest(
        @NotNull @Size(max = 2000) String reply
) {}
