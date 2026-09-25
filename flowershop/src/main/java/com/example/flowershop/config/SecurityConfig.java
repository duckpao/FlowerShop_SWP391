package com.example.flowershop.config;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, com.example.flowershop.service.TokenAuthService tokenAuth,
            org.springframework.core.env.Environment environment, com.example.flowershop.repository.UserRepository users)
            throws Exception {

        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/payments/**", "/api/cart/**", "/api/orders/**"))
                .sessionManagement(session -> session.sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
                .addFilterBefore(new com.example.flowershop.security.JwtAuthenticationFilter(tokenAuth),
                        org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/", "/index.html", "/assets/**", "/vite.svg", "/favicon.svg", "/icons.svg",
                                "/login", "/account", "/admin", "/admin/shops", "/admin/approvals", "/admin/users", "/admin/profile",
                                "/shop-admin", "/shop-admin/staff", "/shop-admin/products", "/shop-admin/shop", "/shop-admin/profile", "/shops/*",
                                "/cart", "/home", "/payment/**").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/public/shops","/api/public/shops/*","/api/public/shops/*/products").permitAll()
                        .requestMatchers("/api/cart/**", "/api/orders/**", "/api/payments/**").permitAll()
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/auth/csrf"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/api/auth/logout",
                                "/api/auth/register",
                                "/api/auth/register-shop",
                                "/api/auth/verify-email",
                                "/api/auth/resend-verification",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password"
                        ).permitAll()

                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")
                        .requestMatchers("/api/shop/**").hasRole("SHOP")
                        .requestMatchers("/api/customer/**").hasRole("CUSTOMER")
                        .requestMatchers("/api/staff/**").hasRole("SHOP_STAFF")
                        .requestMatchers("/api/delivery/**").denyAll()

                        .anyRequest().authenticated()
                )

                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(
                                new HttpStatusEntryPoint(
                                        HttpStatus.UNAUTHORIZED
                                )
                        )
                )

                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable());

        if (environment.acceptsProfiles(org.springframework.core.env.Profiles.of("dev-admin", "dev-manager"))) {
            if (!"127.0.0.1".equals(environment.getProperty("server.address"))
                    || environment.acceptsProfiles(org.springframework.core.env.Profiles.of("prod", "production")))
                throw new IllegalStateException("Local test profiles require server.address=127.0.0.1 and cannot run with production profiles");
        }
        if (environment.acceptsProfiles(org.springframework.core.env.Profiles.of("dev-manager"))) {
            http.addFilterAfter(new com.example.flowershop.security.DevManagerFilter(users,environment.getRequiredProperty("app.dev-manager.email")),
                    com.example.flowershop.security.JwtAuthenticationFilter.class);
        }
        if (environment.acceptsProfiles(org.springframework.core.env.Profiles.of("dev-admin"))) {
            http.addFilterAfter(new com.example.flowershop.security.DevAdminFilter(),
                    com.example.flowershop.security.JwtAuthenticationFilter.class);
        }
        return http.build();
    }
}
