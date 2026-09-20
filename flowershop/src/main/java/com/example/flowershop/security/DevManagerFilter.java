package com.example.flowershop.security;

import com.example.flowershop.dto.auth.CurrentUser;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.UserRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

/** Explicit localhost test identity; no servlet auto-registration. */
public class DevManagerFilter extends OncePerRequestFilter {
    private final UserRepository users;
    private final String email;
    public DevManagerFilter(UserRepository users,String email) { this.users=users; this.email=email; }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)
            throws ServletException,IOException {
        String path=request.getRequestURI().substring(request.getContextPath().length());
        boolean allowed=switch(request.getMethod()) {
            case "GET" -> path.equals("/api/shop/mine") || path.matches("/api/shop/mine/[^/]+/(staff|address|invitations)");
            case "PUT" -> path.matches("/api/shop/mine/[^/]+") || path.matches("/api/shop/mine/[^/]+/address") || path.matches("/api/shop/mine/[^/]+/staff/[^/]+");
            case "POST" -> path.matches("/api/shop/mine/[^/]+/(staff|invitations)");
            case "DELETE" -> path.matches("/api/shop/mine/[^/]+/invitations/[^/]+");
            default -> false;
        };
        if(allowed && List.of("127.0.0.1","::1","0:0:0:0:0:0:0:1").contains(request.getRemoteAddr())
                && request.getHeader("Authorization")==null && SecurityContextHolder.getContext().getAuthentication()==null) {
            var user=users.findByEmail(email).filter(u->u.getRole()==UserRole.SHOP && u.getStatus()==UserStatus.ACTIVE).orElse(null);
            if(user==null) {
                response.setStatus(503); response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"message\":\"Chưa có Manager ACTIVE khớp app.dev-manager.email trong DB local.\"}"); return;
            }
            var context=SecurityContextHolder.createEmptyContext();
            context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(CurrentUser.from(user),null,List.of(new SimpleGrantedAuthority("ROLE_SHOP"))));
            SecurityContextHolder.setContext(context);
        }
        chain.doFilter(request,response);
    }
}
