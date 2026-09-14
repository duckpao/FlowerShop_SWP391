package com.example.flowershop.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TestController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/hello")
    public Map<String, String> sayHello() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Kết nối Backend Spring Boot và Frontend React thành công! 🌸");
        response.put("status", "OK");
        return response;
    }

    @GetMapping("/db-check")
    public Map<String, Object> checkDatabaseConnection() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");

        try {
            // Thực hiện truy vấn đơn giản để kiểm tra kết nối database
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            if (result != null && result == 1) {
                response.put("database", "CONNECTED");
                response.put("message", "Kết nối cơ sở dữ liệu MySQL thành công! 🚀");

                // Lấy thông tin metadata của Database
                try (Connection connection = jdbcTemplate.getDataSource().getConnection()) {
                    response.put("database_product", connection.getMetaData().getDatabaseProductName());
                    response.put("database_version", connection.getMetaData().getDatabaseProductVersion());
                }
            } else {
                response.put("database", "FAILED");
                response.put("message", "Phản hồi truy vấn kiểm tra không hợp lệ.");
            }
        } catch (Exception e) {
            response.put("database", "DISCONNECTED");
            response.put("message", "Không thể kết nối cơ sở dữ liệu: " + e.getMessage());
        }

        return response;
    }
}