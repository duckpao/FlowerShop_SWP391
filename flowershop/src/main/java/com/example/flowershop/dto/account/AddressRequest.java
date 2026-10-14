package com.example.flowershop.dto.account;

import jakarta.validation.constraints.*;

/** A saved address: province, district and ward, with the GHN district id and ward code they were picked from. */
public record AddressRequest(
        @NotBlank(message="Vui lòng nhập tên đường, tòa nhà, số nhà.") @Size(max=255) String addressLine,
        @NotBlank(message="Vui lòng chọn tỉnh/thành phố.") @Size(max=100) String city,
        @NotBlank(message="Vui lòng chọn quận/huyện.") @Size(max=100) String district,
        @NotBlank(message="Vui lòng chọn phường/xã.") @Size(max=100) String ward,
        boolean isDefault,
        @NotBlank(message="Vui lòng nhập họ và tên.") @Size(max=100) String recipientName,
        @NotBlank(message="Vui lòng nhập số điện thoại.") @Pattern(regexp="0[0-9]{9}", message="Số điện thoại gồm 10 chữ số, bắt đầu bằng 0.") String recipientPhone,
        Boolean isPickup, Boolean isReturn,
        @NotNull(message="Vui lòng chọn loại địa chỉ.") @Pattern(regexp="HOME|OFFICE") String addressType,
        @NotNull(message="Vui lòng chọn quận/huyện từ danh mục GHN.") Integer ghnDistrictId,
        @NotBlank(message="Vui lòng chọn phường/xã từ danh mục GHN.") @Size(max=20) String ghnWardCode) {
}
