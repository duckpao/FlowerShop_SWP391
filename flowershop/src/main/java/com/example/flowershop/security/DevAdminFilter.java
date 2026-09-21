package com.example.flowershop.security;

import com.example.flowershop.dto.auth.CurrentUser;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

/** Installed explicitly only in dev-admin; never registered as a servlet filter. */
public class DevAdminFilter extends OncePerRequestFilter {
    public static final String ACTOR_ID = "local-dev-admin";
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path=request.getRequestURI().substring(request.getContextPath().length());
        boolean allowed=(request.getMethod().equals("GET") &&
                (path.equals("/api/admin/customers") || path.matches("/api/admin/customers/[^/]+")))
                || (request.getMethod().equals("PUT") && path.matches("/api/admin/customers/[^/]+/blocked"))
                || (request.getMethod().equals("GET") && (path.equals("/api/admin/shops") || path.matches("/api/admin/shops/[^/]+")))
                || (request.getMethod().equals("PUT") && path.matches("/api/admin/shops/[^/]+/(approve|block|unblock)"))
                || (request.getMethod().equals("PUT") && path.matches("/api/admin/shops/applications/[^/]+"));
        boolean loopback=List.of("127.0.0.1","::1","0:0:0:0:0:0:0:1").contains(request.getRemoteAddr());
        if (allowed && loopback && request.getHeader("Authorization")==null
                && SecurityContextHolder.getContext().getAuthentication()==null) {
            var context=SecurityContextHolder.createEmptyContext();
            context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                    new CurrentUser(ACTOR_ID,"dev-admin@localhost","Local test Admin","ADMIN"),null,
                    List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
            SecurityContextHolder.setContext(context);
        }
        chain.doFilter(request,response);
    }
}
