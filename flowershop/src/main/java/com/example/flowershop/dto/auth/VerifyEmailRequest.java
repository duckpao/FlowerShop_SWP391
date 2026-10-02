package com.example.flowershop.dto.auth;
import jakarta.validation.constraints.*;
public record VerifyEmailRequest(
        @NotBlank @Email @Size(max = 50) @ValidEmail String email,
        @NotBlank @Pattern(regexp = "[0-9]{6}", message = "OTP phải gồm 6 chữ số") String otp
, @Size(max=36) String captchaId, @Size(max=5) String captchaAnswer) {
    public VerifyEmailRequest(String email, String otp) { this(email,otp,null,null); }
    @Override public String toString() { return "VerifyEmailRequest[redacted]"; }
}
