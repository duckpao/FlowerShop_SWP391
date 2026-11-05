package com.example.flowershop.dto.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Pickup address of a shop: province, district and ward, with the GHN district id and ward code they were picked from. */
public record ShopAddressRequest(
        @NotBlank(message = "Vui lòng nhập số nhà, tên đường.") @Size(max = 255) String addressLine,
        @NotBlank(message = "Vui lòng chọn tỉnh/thành phố.") @Size(max = 100) String city,
        @NotBlank(message = "Vui lòng chọn quận/huyện.") @Size(max = 100) String district,
        @NotBlank(message = "Vui lòng chọn phường/xã.") @Size(max = 100) String ward,
        @NotNull(message = "Vui lòng chọn quận/huyện từ danh mục GHN.") Integer ghnDistrictId,
        @NotBlank(message = "Vui lòng chọn phường/xã từ danh mục GHN.") @Size(max = 20) String ghnWardCode) {
}
