package com.example.flowershop.chat;

import com.example.flowershop.entity.enums.ChatContextType;
import com.example.flowershop.entity.enums.SenderType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Dữ liệu vào và ra của chat khách và shop. */
public final class ChatDtos {
    private ChatDtos() {
    }

    public record ContextInput(@NotNull ChatContextType type, @Size(max = 36) String id) {
    }

    public record SendInput(@Size(max = 2000) String message, @Size(max = 4) List<@Size(max = 500) String> images,
                            @Valid ContextInput context) {
    }

    public record OpenForCustomer(@NotNull @Size(max = 36) String shopId) {
    }

    public record OpenForShop(@NotNull @Size(max = 36) String customOrderId) {
    }

    public record MessageDto(String id, String sessionId, SenderType senderType, String senderId, String senderName,
                             String message, List<String> images, ChatContextType contextType, String contextId,
                             Map<String, Object> context, LocalDateTime createdDate, boolean read) {
    }

    public record SessionDto(String id, String shopId, String shopName, String shopLogoUrl, String ownerAvatarUrl,
                             String customerId, String customerName, String customerAvatarUrl,
                             LocalDateTime lastMessageAt, String lastMessagePreview, int unread) {
    }

    /** Đẩy tới hộp thư của một người khi có tin mới hoặc khi đã đọc, để danh sách và huy hiệu cập nhật. */
    public record InboxEvent(String sessionId, String preview, LocalDateTime lastMessageAt, int unread, long totalUnread) {
    }

    public record MessageEvent(MessageDto message, String customerId, String ownerId, int customerUnread, int shopUnread,
                               long customerTotal, long shopTotal) {
    }

    public record ReadEvent(String sessionId, String customerId, String ownerId, boolean customerRead,
                            long customerTotal, long shopTotal) {
    }
}
