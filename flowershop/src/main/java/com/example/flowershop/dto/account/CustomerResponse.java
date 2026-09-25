package com.example.flowershop.dto.account;

import com.example.flowershop.entity.User;
import java.time.LocalDateTime;

public record CustomerResponse(String id, String email, String fullName, String phone,
                               String status, boolean emailVerified, LocalDateTime createdAt) {
    public static CustomerResponse from(User u) {
        return new CustomerResponse(u.getId(),u.getEmail(),u.getFullName(),u.getPhone(),
                u.getStatus().name(),Boolean.TRUE.equals(u.getIsEmailVerified()),u.getCreatedDate());
    }
}
