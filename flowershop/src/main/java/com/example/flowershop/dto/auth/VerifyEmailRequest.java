package com.example.flowershop.dto.auth;
import jakarta.validation.constraints.*;
public record VerifyEmailRequest(
        @NotBlank @Email @Size(max = 50) String email,
        @NotBlank @Pattern(regexp = "[0-9]{6}", message = "OTP phải gồm 6 chữ số") String otp
) {
    @Override public String toString() { return "VerifyEmailRequest[redacted]"; }
}
