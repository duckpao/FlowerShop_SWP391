package com.example.flowershop.dto.auth;
import jakarta.validation.constraints.*;
public record ResetPasswordRequest(
        @NotBlank @Email @Size(max = 50) @ValidEmail String email,
        @NotBlank @Pattern(regexp = "[0-9]{6}") String otp,
        @NotBlank @Size(min = 15, max = 72) String newPassword,
        @NotBlank @Size(min = 15, max = 72) String confirmPassword
, @Size(max=36) String captchaId, @Size(max=5) String captchaAnswer) {
    public ResetPasswordRequest(String email, String otp, String newPassword, String confirmPassword) { this(email,otp,newPassword,confirmPassword,null,null); }
    @Override public String toString() { return "ResetPasswordRequest[redacted]"; }
}
