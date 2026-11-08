-- Safe additive reconciliation after merging master and feature/dung.
-- Run 027_sync_schema.sql first. Keep historical migration filenames unchanged.
USE flower_shop_db;
SET NAMES utf8mb4;
SET @ddl=IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND LOWER(TABLE_NAME)='orders' AND COLUMN_NAME='custom_request_id')=0,
    'ALTER TABLE Orders ADD COLUMN custom_request_id VARCHAR(36) NULL', 'SELECT 1');
PREPARE merged_schema FROM @ddl; EXECUTE merged_schema; DEALLOCATE PREPARE merged_schema;
SET @ddl=IF((SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND LOWER(TABLE_NAME)='orders' AND INDEX_NAME='idx_orders_custom_request')=0,
    'CREATE INDEX idx_orders_custom_request ON Orders (custom_request_id)', 'SELECT 1');
PREPARE merged_schema FROM @ddl; EXECUTE merged_schema; DEALLOCATE PREPARE merged_schema;
SET @ddl=IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND LOWER(TABLE_NAME)='orders' AND COLUMN_NAME='payment_started_at')=0,
    'ALTER TABLE Orders ADD COLUMN payment_started_at DATETIME NULL', 'SELECT 1');
PREPARE merged_schema FROM @ddl; EXECUTE merged_schema; DEALLOCATE PREPARE merged_schema;
-- Superset of both workflows; no status values are removed or rewritten.
ALTER TABLE Orders MODIFY COLUMN delivery_address_id VARCHAR(36) NULL;
ALTER TABLE Orders MODIFY COLUMN status ENUM('PENDING','AWAITING_DEPOSIT','DEPOSIT_PAID','PROCESSING','DELIVERING','COMPLETED','CANCELLED','REFUNDED','AWAITING_QUOTE','QUOTED') DEFAULT 'PENDING';
