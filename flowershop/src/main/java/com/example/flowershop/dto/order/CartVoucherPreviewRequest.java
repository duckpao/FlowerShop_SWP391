package com.example.flowershop.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Xem trước voucher cho một shop: các món đã chọn và (tùy chọn) mã đang chọn. Không giữ lượt dùng. */
public record CartVoucherPreviewRequest(
        @NotBlank String shopId,
        @NotEmpty List<String> cartItemIds,
        @Size(max = 50) String couponCode) {
}
