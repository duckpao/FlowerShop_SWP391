-- Chat khách và shop: một luồng cho mỗi cặp (khách, shop); tin có thể đính kèm thẻ đơn custom hoặc sản phẩm.
-- Nếu đã có nhiều phiên trùng cặp, gộp trước khi chạy vì UNIQUE sẽ lỗi.
ALTER TABLE Chat_Sessions
    ADD COLUMN last_message_at DATETIME(6) NULL,
    ADD COLUMN last_message_preview VARCHAR(255) NULL,
    ADD COLUMN customer_unread_count INT NOT NULL DEFAULT 0,
    ADD COLUMN shop_unread_count INT NOT NULL DEFAULT 0,
    ADD UNIQUE KEY uq_chat_customer_shop (customer_id, shop_id),
    ADD INDEX idx_chat_shop_last (shop_id, last_message_at);

ALTER TABLE Chat_Messages
    ADD COLUMN context_type ENUM('NONE', 'CUSTOM_ORDER', 'PRODUCT') NOT NULL DEFAULT 'NONE',
    ADD COLUMN context_id VARCHAR(36) NULL,
    ADD COLUMN context_snapshot JSON NULL,
    ADD INDEX idx_chat_messages_session (session_id, created_date);
