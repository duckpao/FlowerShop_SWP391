package com.example.flowershop.dto.account;

import com.example.flowershop.entity.Shop;
import java.time.LocalDateTime;

public record ShopResponse(String id,String name,String description,String logoUrl,String status,
        Owner owner,LocalDateTime createdAt,LocalDateTime updatedAt) {
    public record Owner(String id,String email,String fullName,String phone,String role,String status,boolean emailVerified) {}
    public static ShopResponse from(Shop s) {
        var u=s.getOwner();
        return new ShopResponse(s.getId(),s.getName(),s.getDescription(),s.getLogoUrl(),s.getStatus().name(),
                new Owner(u.getId(),u.getEmail(),u.getFullName(),u.getPhone(),u.getRole().name(),u.getStatus().name(),
                        Boolean.TRUE.equals(u.getIsEmailVerified())),s.getCreatedDate(),s.getLastModifyDate());
    }
}
