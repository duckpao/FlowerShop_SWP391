package com.example.flowershop.repository;

import com.example.flowershop.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, String> {

    List<Notification> findByUserIdOrderByCreatedDateDesc(String userId);

    java.util.Optional<com.example.flowershop.entity.Notification> findFirstByUserIdAndLinkOrderByCreatedDateDesc(String userId, String link);

    long countByUserIdAndReadFalse(String userId);

    @Modifying
    @Query("update Notification n set n.read = true where n.user.id = :userId and n.read = false")
    void markAllAsRead(String userId);
}
