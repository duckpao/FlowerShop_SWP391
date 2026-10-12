package com.example.flowershop.dto.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Pickup address of a shop in the two-level model: province + ward, each with its GHN id. */
public record ShopAddressRequest(
        @NotBlank(message = "Vui lòng nhập số nhà, tên đường.") @Size(max = 255) String addressLine,
        @NotBlank(message = "Vui lòng chọn tỉnh/thành phố.") @Size(max = 100) String city,
        @NotBlank(message = "Vui lòng chọn phường/xã.") @Size(max = 100) String ward,
        @NotNull(message = "Vui lòng chọn tỉnh/thành phố từ danh mục GHN.") Integer ghnProvinceId,
        @NotNull(message = "Vui lòng chọn phường/xã từ danh mục GHN.") Integer ghnWardId) {
}
