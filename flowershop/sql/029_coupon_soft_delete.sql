-- Xóa mềm voucher: giữ lại mã để đơn cũ còn tham chiếu. Chạy lại nhiều lần vẫn an toàn.
USE flower_shop_db;
SET NAMES utf8mb4;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'Coupons' AND column_name = 'deleted') = 0,
    'ALTER TABLE Coupons ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE AFTER status', 'SELECT 1');
PREPARE coupon_migration FROM @ddl; EXECUTE coupon_migration; DEALLOCATE PREPARE coupon_migration;
