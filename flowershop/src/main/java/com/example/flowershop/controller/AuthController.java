package com.example.flowershop.controller;

import com.example.flowershop.dto.auth.RegisterRequest;
import com.example.flowershop.dto.auth.ResetPasswordRequest;
import com.example.flowershop.dto.auth.VerifyEmailRequest;
import com.example.flowershop.dto.auth.ResendVerificationRequest;
import com.example.flowershop.dto.common.MessageResponse;
import com.example.flowershop.service.AuthService;
import com.example.flowershop.service.OtpService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

//Do Van Quang auth
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

        private final AuthService authService;
        private final OtpService otpService;
        private final com.example.flowershop.security.CaptchaService captcha;

        public record CaptchaRequest(
                        @com.example.flowershop.dto.auth.ValidEmail @jakarta.validation.constraints.Size(max = 255) String email,
                        @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Pattern(regexp = "login|register|reset") String purpose) {
        }

        @PostMapping("/captcha")
        public ResponseEntity<com.example.flowershop.security.CaptchaService.Image> captcha(
                        @Valid @RequestBody CaptchaRequest r) {
                return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore())
                                .body(captcha.create(r.email(), r.purpose()));
        }

        @PostMapping("/register-shop")
        @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
        public ResponseEntity<MessageResponse> registerShop(
                        @Valid @RequestBody com.example.flowershop.dto.auth.ShopRegisterRequest request) {
                authService.registerShop(request);
                return ResponseEntity.accepted().body(new MessageResponse(
                                "Nếu email đủ điều kiện, mã OTP sẽ được gửi để xác thực tài khoản và tạo cửa hàng chờ duyệt."));
        }

        @GetMapping("/csrf")
        public Map<String, String> csrf(CsrfToken csrfToken) {
                return Map.of(
                                "headerName", csrfToken.getHeaderName(),
                                "token", csrfToken.getToken());
        }

        @PostMapping("/register")
        @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true, description = "Copy the token from GET /api/auth/csrf in the same browser session.", schema = @Schema(type = "string"))
        public ResponseEntity<MessageResponse> register(
                        @Valid @RequestBody RegisterRequest request) {
                authService.registerCustomer(request);

                return ResponseEntity.accepted().body(
                                new MessageResponse(
                                                "Yêu cầu đã được tiếp nhận. Nếu email đủ điều kiện, hãy kiểm tra OTP để hoàn tất đăng ký."));
        }

        @PostMapping("/verify-email")
        @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true, description = "Copy the token from GET /api/auth/csrf in the same browser session.", schema = @Schema(type = "string"))
        public MessageResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
                authService.verify(request.email(), request.otp(), request.captchaId(), request.captchaAnswer());
                return new MessageResponse("Xác thực email và đăng ký thành công.");
        }

        @PostMapping("/resend-verification")
        @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true, description = "Copy the token from GET /api/auth/csrf in the same browser session.", schema = @Schema(type = "string"))
        public ResponseEntity<MessageResponse> resendVerification(
                        @Valid @RequestBody ResendVerificationRequest request) {
                authService.resend(request.email(), request.captchaId(), request.captchaAnswer());
                return ResponseEntity.accepted().body(new MessageResponse(
                                "Nếu tài khoản đủ điều kiện, email xác thực sẽ được gửi. Vui lòng kiểm tra hộp thư."));
        }

        @PostMapping("/forgot-password")
        @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true, schema = @Schema(type = "string"))
        public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ResendVerificationRequest request) {
                otpService.forgotPassword(request.email(), request.captchaId(), request.captchaAnswer());
                return ResponseEntity.accepted().body(new MessageResponse(
                                "Nếu tài khoản đủ điều kiện, mã đặt lại mật khẩu sẽ được gửi qua email."));
        }

        @PostMapping("/reset-password")
        @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true, schema = @Schema(type = "string"))
        public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
                otpService.resetPassword(request.email(), request.otp(), request.newPassword(),
                                request.confirmPassword(), request.captchaId(), request.captchaAnswer());
                return new MessageResponse("Đặt lại mật khẩu thành công.");
        }
}
