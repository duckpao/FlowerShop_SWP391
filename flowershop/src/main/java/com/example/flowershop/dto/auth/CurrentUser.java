package com.example.flowershop.dto.auth;
import com.example.flowershop.entity.User;
public record CurrentUser(String id, String email, String fullName, String role) {
    public static CurrentUser from(User user) {
        return new CurrentUser(user.getId(), user.getEmail(), user.getFullName(), user.getRole().name());
    }
}
