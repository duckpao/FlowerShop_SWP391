package com.example.flowershop.controller;

import com.example.flowershop.dto.auth.*;
import com.example.flowershop.dto.common.MessageResponse;
import com.example.flowershop.service.TokenAuthService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.time.Duration;

@RestController @RequestMapping("/api/auth")
public class SessionController {
    private final TokenAuthService auth;
    private final boolean secure;
    private final com.example.flowershop.security.LoginRateLimiter limiter;
    public SessionController(TokenAuthService auth, com.example.flowershop.security.LoginRateLimiter limiter,
                             @Value("${server.servlet.session.cookie.secure:false}") boolean secure) {
        this.auth=auth; this.secure=secure; this.limiter=limiter;
    }
    public record LoginResponse(String accessToken,String tokenType,long expiresIn,CurrentUser user) {}
    @PostMapping("/login")
    @Parameter(name="X-CSRF-TOKEN",in=ParameterIn.HEADER,required=true)
    public LoginResponse login(@Valid @RequestBody LoginRequest request,HttpServletResponse response,
                               jakarta.servlet.http.HttpServletRequest servletRequest) {
        limiter.check(servletRequest.getRemoteAddr());
        return respond(auth.login(request),response);
    }
    @PostMapping("/refresh")
    @Parameter(name="X-CSRF-TOKEN",in=ParameterIn.HEADER,required=true)
    public ResponseEntity<LoginResponse> refresh(@CookieValue(name="refresh_token",required=false) String refresh,HttpServletResponse response) {
        if (refresh == null || refresh.isBlank()) return noSession(response);
        try {
            return ResponseEntity.ok(respond(auth.refresh(refresh),response));
        } catch (AuthenticationException | JwtException invalidSession) {
            return noSession(response);
        }
    }
    @GetMapping("/me")
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name="bearerAuth")
    public CurrentUser me(@AuthenticationPrincipal CurrentUser user) { return user; }
    @PostMapping("/logout")
    @Parameter(name="X-CSRF-TOKEN",in=ParameterIn.HEADER,required=true)
    public MessageResponse logout(@CookieValue(name="refresh_token",required=false) String refresh,HttpServletResponse response) {
        try { auth.logout(refresh); } catch (AuthenticationException | JwtException ignored) { }
        cookie(response,"",0);
        return new MessageResponse("Đã đăng xuất.");
    }
    private LoginResponse respond(TokenAuthService.Tokens tokens,HttpServletResponse response) {
        cookie(response,tokens.refreshToken(),604800);
        response.setHeader("Cache-Control","no-store");
        return new LoginResponse(tokens.accessToken(),"Bearer",tokens.expiresIn(),tokens.user());
    }
    private ResponseEntity<LoginResponse> noSession(HttpServletResponse response) {
        cookie(response,"",0);
        response.setHeader("Cache-Control","no-store");
        return ResponseEntity.noContent().build();
    }
    private void cookie(HttpServletResponse response,String value,long seconds) {
        response.addHeader(HttpHeaders.SET_COOKIE,ResponseCookie.from("refresh_token",value)
                .httpOnly(true).secure(secure).sameSite("Lax").path("/api/auth")
                .maxAge(Duration.ofSeconds(seconds)).build().toString());
    }
}
