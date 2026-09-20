package com.example.flowershop.dto.auth;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
public record ShopRegisterRequest(@Valid @NotNull RegisterRequest account,
        @NotBlank @Size(max=255) String shopName,@NotNull @Size(max=5000) String description) {
    @Override public String toString() { return "ShopRegisterRequest[redacted]"; }
}
