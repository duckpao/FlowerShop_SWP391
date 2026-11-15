-- Báo giá hoa tùy chỉnh: giá, % cọc, hạn báo giá, lý do từ chối và trạng thái QUOTED. Chạy lại nhiều lần vẫn an toàn.
USE flower_shop_db;
SET NAMES utf8mb4;

SET @ddl0 = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'Custom_Order_Requests' AND column_name = 'quoted_price') = 0,
    'ALTER TABLE Custom_Order_Requests ADD COLUMN quoted_price DECIMAL(12, 2) NULL', 'SELECT 1');
PREPARE custom_quote_migration FROM @ddl0; EXECUTE custom_quote_migration; DEALLOCATE PREPARE custom_quote_migration;

SET @ddl1 = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'Custom_Order_Requests' AND column_name = 'deposit_percent') = 0,
    'ALTER TABLE Custom_Order_Requests ADD COLUMN deposit_percent INT NULL', 'SELECT 1');
PREPARE custom_quote_migration FROM @ddl1; EXECUTE custom_quote_migration; DEALLOCATE PREPARE custom_quote_migration;

SET @ddl2 = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'Custom_Order_Requests' AND column_name = 'quote_valid_until') = 0,
    'ALTER TABLE Custom_Order_Requests ADD COLUMN quote_valid_until DATETIME NULL', 'SELECT 1');
PREPARE custom_quote_migration FROM @ddl2; EXECUTE custom_quote_migration; DEALLOCATE PREPARE custom_quote_migration;

SET @ddl3 = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'Custom_Order_Requests' AND column_name = 'quoted_date') = 0,
    'ALTER TABLE Custom_Order_Requests ADD COLUMN quoted_date DATETIME NULL', 'SELECT 1');
PREPARE custom_quote_migration FROM @ddl3; EXECUTE custom_quote_migration; DEALLOCATE PREPARE custom_quote_migration;

SET @ddl4 = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'Custom_Order_Requests' AND column_name = 'reject_reason') = 0,
    'ALTER TABLE Custom_Order_Requests ADD COLUMN reject_reason VARCHAR(2000) NULL', 'SELECT 1');
PREPARE custom_quote_migration FROM @ddl4; EXECUTE custom_quote_migration; DEALLOCATE PREPARE custom_quote_migration;

SET @ddl5 = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'Custom_Order_Requests' AND column_name = 'rejected_by') = 0,
    'ALTER TABLE Custom_Order_Requests ADD COLUMN rejected_by VARCHAR(20) NULL', 'SELECT 1');
PREPARE custom_quote_migration FROM @ddl5; EXECUTE custom_quote_migration; DEALLOCATE PREPARE custom_quote_migration;

ALTER TABLE Custom_Order_Requests
    MODIFY COLUMN status ENUM('REQUESTED', 'NEGOTIATING', 'QUOTED', 'ACCEPTED', 'REJECTED') DEFAULT 'REQUESTED';
