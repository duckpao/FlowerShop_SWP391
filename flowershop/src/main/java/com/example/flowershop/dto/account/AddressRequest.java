package com.example.flowershop.dto.account;

import jakarta.validation.constraints.*;

public record AddressRequest(
        @NotBlank @Size(max=255) String addressLine,
        @NotBlank @Size(max=100) String city,
        @Size(max=100) String district,
        @NotBlank @Size(max=100) String ward,
        @NotBlank @Size(max=20) String ghnWardCode,
        @NotNull Integer ghnDistrictId,
        boolean isDefault) {}
