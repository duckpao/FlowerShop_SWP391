package com.example.flowershop.dto.auth;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
public record ShopRegisterRequest(@Valid @NotNull RegisterRequest account,
        @Size(max=255) String shopName,
        @NotBlank(message="Vui lòng nhập mô tả cửa hàng.") @Size(max=5000,message="Mô tả tối đa 5000 ký tự.") String description,
        @Valid @NotNull(message="Vui lòng nhập địa chỉ cửa hàng.") ShopAddress address) {
    public record ShopAddress(
            @NotBlank(message="Vui lòng nhập tên đường, tòa nhà, số nhà.") @Size(max=255) String addressLine,
            @NotBlank(message="Vui lòng chọn tỉnh/thành phố.") @Size(max=100) String city,
            @NotBlank(message="Vui lòng chọn quận/huyện.") @Size(max=100) String district,
            @NotBlank(message="Vui lòng chọn phường/xã.") @Size(max=100) String ward,
            @NotBlank(message="Vui lòng chọn phường/xã.") @Size(max=20) String ghnWardCode,
            @NotNull(message="Vui lòng chọn quận/huyện.") Integer ghnDistrictId) {}
    @Override public String toString() { return "ShopRegisterRequest[redacted]"; }
}
