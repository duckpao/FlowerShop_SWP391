package com.example.flowershop.security;

import com.example.flowershop.service.TokenAuthService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtException;
import java.io.IOException;
import java.util.List;

// Registered only in SecurityFilterChain, not as a servlet component.
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final TokenAuthService auth;
    public JwtAuthenticationFilter(TokenAuthService auth) { this.auth=auth; }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)
            throws ServletException,IOException {
        String header=request.getHeader("Authorization");
        if (header != null) {
            try {
                if (!header.startsWith("Bearer ")) throw new org.springframework.security.authentication.BadCredentialsException("Invalid authorization");
                var user=auth.authenticate(header.substring(7));
                var authentication=UsernamePasswordAuthenticationToken.authenticated(user,null,
                        List.of(new SimpleGrantedAuthority("ROLE_"+user.role())));
                var context=SecurityContextHolder.createEmptyContext(); context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
            } catch (AuthenticationException | JwtException e) {
                SecurityContextHolder.clearContext(); response.setStatus(401);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"message\":\"Phiên đăng nhập không hợp lệ hoặc đã hết hạn.\"}"); return;
            }
        }
        chain.doFilter(request,response);
    }
}
