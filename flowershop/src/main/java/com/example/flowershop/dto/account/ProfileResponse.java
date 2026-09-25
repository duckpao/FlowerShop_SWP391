package com.example.flowershop.dto.account;

import com.example.flowershop.entity.User;

public record ProfileResponse(String id, String email, String fullName, String phone, String role, boolean emailVerified) {
    public static ProfileResponse from(User user) {
        return new ProfileResponse(user.getId(), user.getEmail(), user.getFullName(), user.getPhone(),
                user.getRole().name(), Boolean.TRUE.equals(user.getIsEmailVerified()));
    }
}
