package com.example.flowershop.dto.account;

import jakarta.validation.constraints.*;

public record ProfileRequest(
                @NotBlank(message = "Họ tên không được để trống") @Size(max = 100) String fullName,
                @NotNull @Pattern(regexp = "^(?:|0[0-9]{9})$", message = "Số điện thoại phải bắt đầu bằng 0 và có đúng 10 chữ số") String phone) {
}
        @NotBlank(message="Họ tên không được để trống") @Size(max=100) String fullName,
        @NotNull @Pattern(regexp="^(?:|0[0-9]{9})$", message="Số điện thoại phải bắt đầu bằng 0 và có đúng 10 chữ số") String phone) {}
