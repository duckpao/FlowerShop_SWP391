package com.example.flowershop.dto.account;

import jakarta.validation.constraints.*;

public record AddressRequest(
        @NotBlank(message="Vui lòng nhập tên đường, tòa nhà, số nhà.") @Size(max=255) String addressLine,
        @NotBlank(message="Vui lòng chọn tỉnh/thành phố.") @Size(max=100) String city,
        @Size(max=100) String district,
        @NotBlank(message="Vui lòng chọn phường/xã.") @Size(max=100) String ward,
        boolean isDefault,
        @NotBlank(message="Vui lòng nhập họ và tên.") @Size(max=100) String recipientName,
        @NotBlank(message="Vui lòng nhập số điện thoại.") @Pattern(regexp="0[0-9]{9}", message="Số điện thoại gồm 10 chữ số, bắt đầu bằng 0.") String recipientPhone,
        Boolean isPickup, Boolean isReturn,
        @NotNull(message="Vui lòng chọn loại địa chỉ.") @Pattern(regexp="HOME|OFFICE") String addressType,
        @NotBlank @Size(max=20) String ghnWardCode, @NotNull Integer ghnDistrictId) {
    public AddressRequest(String addressLine, String city, String district, String ward, String ghnWardCode, Integer ghnDistrictId, boolean isDefault) {
        this(addressLine, city, district, ward, isDefault, null, null, false, false, "HOME", ghnWardCode, ghnDistrictId);
    }
}
