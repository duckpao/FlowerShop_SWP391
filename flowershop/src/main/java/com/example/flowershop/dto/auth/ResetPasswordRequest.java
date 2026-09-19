package com.example.flowershop.dto.auth;
import jakarta.validation.constraints.*;
public record ResetPasswordRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Pattern(regexp = "[0-9]{6}") String otp,
        @NotBlank @Size(min = 15, max = 72) String newPassword,
        @NotBlank @Size(min = 15, max = 72) String confirmPassword
) {
    @Override public String toString() { return "ResetPasswordRequest[redacted]"; }
}
