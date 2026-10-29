package com.example.flowershop.chat;

import com.example.flowershop.chat.ChatDtos.InboxEvent;
import com.example.flowershop.chat.ChatDtos.MessageEvent;
import com.example.flowershop.chat.ChatDtos.ReadEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Phát tin mới và số chưa đọc qua WebSocket sau khi giao dịch lưu tin đã commit. */
@Component
public class ChatBroadcaster {

    private final SimpMessagingTemplate template;

    public ChatBroadcaster(SimpMessagingTemplate template) {
        this.template = template;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onMessage(MessageEvent event) {
        var m = event.message();
        template.convertAndSend("/topic/chat/" + m.sessionId(), m);
        template.convertAndSend("/topic/chat-inbox/" + event.customerId(),
                new InboxEvent(m.sessionId(), preview(m), m.createdDate(), event.customerUnread(), event.customerTotal()));
        template.convertAndSend("/topic/chat-inbox/" + event.ownerId(),
                new InboxEvent(m.sessionId(), preview(m), m.createdDate(), event.shopUnread(), event.shopTotal()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onRead(ReadEvent event) {
        String reader = event.customerRead() ? event.customerId() : event.ownerId();
        long total = event.customerRead() ? event.customerTotal() : event.shopTotal();
        template.convertAndSend("/topic/chat-inbox/" + reader, new InboxEvent(event.sessionId(), null, null, 0, total));
        template.convertAndSend("/topic/chat/" + event.sessionId() + "/read", event.customerRead() ? "CUSTOMER" : "SHOP");
    }

    private static String preview(ChatDtos.MessageDto m) {
        String text = m.message() == null || m.message().isBlank() ? "[Hình ảnh]" : m.message();
        return text.length() > 80 ? text.substring(0, 80) : text;
    }
}
