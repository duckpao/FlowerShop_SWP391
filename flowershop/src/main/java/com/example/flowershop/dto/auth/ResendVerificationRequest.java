package com.example.flowershop.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResendVerificationRequest(
        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không hợp lệ")
        @Size(max = 50, message = "Email tối đa 50 ký tự")
        @ValidEmail String email, @Size(max=36) String captchaId, @Size(max=5) String captchaAnswer
) {
    public ResendVerificationRequest(String email) {this(email,null,null);}
}
