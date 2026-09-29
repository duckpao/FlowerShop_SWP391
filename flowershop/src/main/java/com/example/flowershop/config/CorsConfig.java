package com.example.flowershop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**") // Chỉ áp dụng cho các route bắt đầu bằng /api
                        .allowedOrigins("http://localhost:8080", "http://localhost:5173") // 8080: build production nhúng chung; 5173: Vite dev server gọi trực tiếp
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true); // Cho phép gửi cookie/token nếu có
            }
        };
    }
}