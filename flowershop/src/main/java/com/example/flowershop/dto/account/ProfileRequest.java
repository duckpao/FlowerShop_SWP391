package com.example.flowershop.dto.account;

import jakarta.validation.constraints.*;

public record ProfileRequest(
        @NotBlank(message="Họ tên không được để trống") @Size(max=100) String fullName,
        @NotNull @Pattern(regexp="^$|\\+?[0-9]{9,15}$", message="Số điện thoại cần 9–15 chữ số, có thể bắt đầu bằng +") String phone) {}
