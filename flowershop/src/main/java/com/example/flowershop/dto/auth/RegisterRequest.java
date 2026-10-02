package com.example.flowershop.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record RegisterRequest(

        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không hợp lệ")
        @Size(max = 50, message = "Email tối đa 50 ký tự")
        @ValidEmail String email,

        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(min = 9, max = 15,
                message = "Mật khẩu phải có từ 9 đến 15 ký tự")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).*$",
                message = "Mật khẩu phải có ít nhất 1 chữ viết hoa, 1 chữ thường và 1 số"
        )
        String password,

        @NotBlank(message = "Họ tên không được để trống")
        @Size(max = 50, message = "Họ tên tối đa 50 ký tự")
        String fullName
) {
}
