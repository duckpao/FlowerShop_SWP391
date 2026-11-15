-- UC23: khách tự gửi/hủy yêu cầu hoàn tiền (lý do, mô tả, ảnh bằng chứng), shop ghi phản hồi khi từ chối.
-- Chạy lại nhiều lần vẫn an toàn.
USE flower_shop_db;
SET NAMES utf8mb4;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'Refunds' AND column_name = 'description') = 0,
    'ALTER TABLE Refunds ADD COLUMN description TEXT NULL AFTER reason', 'SELECT 1');
PREPARE refund_migration FROM @ddl; EXECUTE refund_migration; DEALLOCATE PREPARE refund_migration;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'Refunds' AND column_name = 'evidence_images') = 0,
    'ALTER TABLE Refunds ADD COLUMN evidence_images TEXT NULL AFTER description', 'SELECT 1');
PREPARE refund_migration FROM @ddl; EXECUTE refund_migration; DEALLOCATE PREPARE refund_migration;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'Refunds' AND column_name = 'shop_response') = 0,
    'ALTER TABLE Refunds ADD COLUMN shop_response TEXT NULL AFTER evidence_images', 'SELECT 1');
PREPARE refund_migration FROM @ddl; EXECUTE refund_migration; DEALLOCATE PREPARE refund_migration;

ALTER TABLE Refunds
    MODIFY status ENUM('REQUESTED', 'APPROVED', 'PROCESSED', 'REJECTED', 'CANCELLED') DEFAULT 'REQUESTED';
