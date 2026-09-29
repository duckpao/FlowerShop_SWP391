package com.example.flowershop.dto.auth;
import jakarta.validation.constraints.*;
public record LoginRequest(@NotBlank @Email @Size(max=255) @ValidEmail String email,
                           @NotBlank @Size(max=72) String password, @Size(max=36) String captchaId, @Size(max=5) String captchaAnswer) {
    public LoginRequest(String email, String password) { this(email,password,null,null); }
    @Override public String toString() { return "LoginRequest[redacted]"; }
}
