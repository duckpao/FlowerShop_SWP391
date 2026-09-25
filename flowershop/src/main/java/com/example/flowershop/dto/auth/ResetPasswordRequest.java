package com.example.flowershop.dto.auth;
import jakarta.validation.constraints.*;
public record ResetPasswordRequest(
        @NotBlank @Email @Size(max = 50) String email,
        @NotBlank @Pattern(regexp = "[0-9]{6}") String otp,
        @NotBlank @Size(min = 9, max = 15) String newPassword,
        @NotBlank @Size(min = 9, max = 15) String confirmPassword
) {
    @Override public String toString() { return "ResetPasswordRequest[redacted]"; }
}
