package com.example.flowershop.service;

import com.example.flowershop.entity.Notification;
import com.example.flowershop.entity.Order;
import com.example.flowershop.entity.User;
import com.example.flowershop.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class NotificationService {

    public record NotificationResponse(
            String id,
            String title,
            String message,
            String link,
            boolean read,
            LocalDateTime createdDate) {
    }

    private final NotificationRepository notifications;

    public NotificationService(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    /**
     * Notify both the customer and the shop owner that an order was placed
     * (COD confirmed, or online payment succeeded).
     */
    @Transactional
    public void notifyOrderPlaced(Order order) {
        create(
                order.getCustomer(),
                "Đặt hàng thành công",
                "Đơn hàng #" + shortId(order) + " đã được đặt thành công.",
                "/orders/" + order.getId());

        create(
                order.getShop().getOwner(),
                "Có đơn hàng mới",
                "Cửa hàng của bạn vừa nhận đơn hàng #" + shortId(order) + ".",
                "/shop-admin/orders/" + order.getId());
    }

    /** Thông báo chuông cho một người dùng (nhắc dịp đặc biệt...). */
    @Transactional
    public void notifyUser(User user, String title, String message, String link) {
        create(user, title, message, link);
    }

    private void create(User user, String title, String message, String link) {
        notifications.save(Notification.builder()
                .id(UUID.randomUUID().toString())
                .user(user)
                .title(title)
                .message(message)
                .link(link)
                .build());
    }

    private static String shortId(Order order) {
        return order.getId().replace("-", "").substring(0, 8).toUpperCase();
    }

    public List<NotificationResponse> mine(String userId) {
        return notifications.findByUserIdOrderByCreatedDateDesc(userId)
                .stream()
                .map(NotificationService::toResponse)
                .toList();
    }

    public long unreadCount(String userId) {
        return notifications.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markAllAsRead(String userId) {
        notifications.markAllAsRead(userId);
    }

    private static NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getTitle(),
                n.getMessage(),
                n.getLink(),
                n.isRead(),
                n.getCreatedDate());
    }
}
