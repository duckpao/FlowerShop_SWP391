-- Đồng bộ database cũ với database.sql: chỉ thêm cột, index và bảng còn thiếu, chạy lại nhiều lần vẫn an toàn.
-- Không xóa hay sửa dữ liệu hiện có.
USE flower_shop_db;
SET NAMES utf8mb4;

DELIMITER $$

DROP PROCEDURE IF EXISTS sync_add_column $$
CREATE PROCEDURE sync_add_column(IN t VARCHAR(64), IN c VARCHAR(64), IN definition VARCHAR(255))
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.TABLES
               WHERE TABLE_SCHEMA = DATABASE() AND LOWER(TABLE_NAME) = LOWER(t))
       AND NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND LOWER(TABLE_NAME) = LOWER(t) AND LOWER(COLUMN_NAME) = LOWER(c)) THEN
        SET @sync_sql = CONCAT('ALTER TABLE `', t, '` ADD COLUMN `', c, '` ', definition);
        PREPARE sync_stmt FROM @sync_sql;
        EXECUTE sync_stmt;
        DEALLOCATE PREPARE sync_stmt;
    END IF;
END $$

DROP PROCEDURE IF EXISTS sync_add_index $$
CREATE PROCEDURE sync_add_index(IN t VARCHAR(64), IN idx VARCHAR(64), IN ddl VARCHAR(500))
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.TABLES
               WHERE TABLE_SCHEMA = DATABASE() AND LOWER(TABLE_NAME) = LOWER(t))
       AND NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
               WHERE TABLE_SCHEMA = DATABASE() AND LOWER(TABLE_NAME) = LOWER(t) AND LOWER(INDEX_NAME) = LOWER(idx)) THEN
        SET @sync_sql = ddl;
        PREPARE sync_stmt FROM @sync_sql;
        EXECUTE sync_stmt;
        DEALLOCATE PREPARE sync_stmt;
    END IF;
END $$

DELIMITER ;

-- Blogs: danh sách ảnh và index cho bảng tin (026_floral_care_blogs)
CALL sync_add_column('Blogs', 'images', 'JSON NULL AFTER content');
CALL sync_add_index('Blogs', 'idx_blogs_public_feed', 'CREATE INDEX idx_blogs_public_feed ON Blogs (status, created_date)');
CALL sync_add_index('Blogs', 'idx_blogs_shop_feed', 'CREATE INDEX idx_blogs_shop_feed ON Blogs (shop_id, created_date)');

-- Danh mục: nhóm trên menu (018_category_groups)
CALL sync_add_column('Categories', 'group_name', 'VARCHAR(50) NULL');

-- Sản phẩm: loại sản phẩm và cờ ẩn của admin (018_product_type, 016_product_catalog)
CALL sync_add_column('Products', 'product_type', "ENUM('READY_MADE', 'CUSTOM') NOT NULL DEFAULT 'READY_MADE'");
CALL sync_add_column('Products', 'admin_hidden', 'BOOLEAN NOT NULL DEFAULT FALSE');

-- Người dùng: ảnh đại diện (021_user_avatar)
CALL sync_add_column('Users', 'avatar_url', 'VARCHAR(1024) NULL');

-- Đơn hàng và thanh toán: checkout nhiều shop (022, 023)
CALL sync_add_column('Orders', 'checkout_group', 'VARCHAR(36) NULL');
CALL sync_add_index('Orders', 'idx_orders_checkout_group', 'CREATE INDEX idx_orders_checkout_group ON Orders (checkout_group)');
CALL sync_add_column('Payments', 'group_invoice', 'VARCHAR(100) NULL');
CALL sync_add_index('Payments', 'idx_payments_group_invoice', 'CREATE INDEX idx_payments_group_invoice ON Payments (group_invoice)');

-- Thông báo trong ứng dụng (024_notifications)
CREATE TABLE IF NOT EXISTS Notifications (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT,
    link VARCHAR(255),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_date DATETIME(6) NOT NULL,
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES Users(id),
    INDEX ix_notification_user (user_id, created_date)
);

-- Nhắc dịp đặc biệt (025_occasion_reminders)
CALL sync_add_column('Customer_Occasions', 'recurring_yearly', 'BOOLEAN NOT NULL DEFAULT FALSE');
CALL sync_add_column('Customer_Occasions', 'notify_days_before', 'INT NOT NULL DEFAULT 7');
CALL sync_add_column('Customer_Occasions', 'notify_time', "TIME NOT NULL DEFAULT '08:00:00'");
CALL sync_add_column('Customer_Occasions', 'last_reminded_for', 'DATE NULL');
CALL sync_add_index('Customer_Occasions', 'idx_occasions_customer', 'CREATE INDEX idx_occasions_customer ON Customer_Occasions (customer_id)');

-- Chat khách và shop (026_chat)
CALL sync_add_column('Chat_Sessions', 'last_message_at', 'DATETIME(6) NULL');
CALL sync_add_column('Chat_Sessions', 'last_message_preview', 'VARCHAR(255) NULL');
CALL sync_add_column('Chat_Sessions', 'customer_unread_count', 'INT NOT NULL DEFAULT 0');
CALL sync_add_column('Chat_Sessions', 'shop_unread_count', 'INT NOT NULL DEFAULT 0');
-- Nếu lệnh UNIQUE dưới đây báo Duplicate entry, đã có hai phiên chat trùng cặp (khách, shop): xóa dòng trùng rồi chạy lại.
CALL sync_add_index('Chat_Sessions', 'uq_chat_customer_shop', 'CREATE UNIQUE INDEX uq_chat_customer_shop ON Chat_Sessions (customer_id, shop_id)');
CALL sync_add_index('Chat_Sessions', 'idx_chat_shop_last', 'CREATE INDEX idx_chat_shop_last ON Chat_Sessions (shop_id, last_message_at)');
CALL sync_add_column('Chat_Messages', 'context_type', "ENUM('NONE', 'CUSTOM_ORDER', 'PRODUCT') NOT NULL DEFAULT 'NONE'");
CALL sync_add_column('Chat_Messages', 'context_id', 'VARCHAR(36) NULL');
CALL sync_add_column('Chat_Messages', 'context_snapshot', 'JSON NULL');
CALL sync_add_index('Chat_Messages', 'idx_chat_messages_session', 'CREATE INDEX idx_chat_messages_session ON Chat_Messages (session_id, created_date)');

DROP PROCEDURE IF EXISTS sync_add_column;
DROP PROCEDURE IF EXISTS sync_add_index;

-- Kiểm tra: mọi dòng dưới đây phải có kết quả (present = 1)
SELECT 'Blogs.images' AS item, COUNT(*) AS present FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND LOWER(TABLE_NAME) = 'blogs' AND COLUMN_NAME = 'images'
UNION ALL SELECT 'Customer_Occasions.last_reminded_for', COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND LOWER(TABLE_NAME) = 'customer_occasions' AND COLUMN_NAME = 'last_reminded_for'
UNION ALL SELECT 'Chat_Sessions.customer_unread_count', COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND LOWER(TABLE_NAME) = 'chat_sessions' AND COLUMN_NAME = 'customer_unread_count'
UNION ALL SELECT 'Chat_Messages.context_snapshot', COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND LOWER(TABLE_NAME) = 'chat_messages' AND COLUMN_NAME = 'context_snapshot'
UNION ALL SELECT 'Notifications table', COUNT(*) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE() AND LOWER(TABLE_NAME) = 'notifications';
