package com.example.flowershop.dto.product;

import jakarta.validation.constraints.NotNull;

/**
 * DTO dữ liệu Admin gửi lên để bật/tắt cờ admin_hidden đối với sản phẩm vi phạm.
 */
public record ProductHiddenRequest(
        @NotNull Boolean hidden
) {}
