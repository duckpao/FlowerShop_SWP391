-- Select the application schema before running. Re-runnable; does not issue new vouchers.
SET NAMES utf8mb4;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='Coupons' AND column_name='target')=0,
    'ALTER TABLE Coupons ADD COLUMN target VARCHAR(20) NOT NULL DEFAULT ''ORDER''', 'SELECT 1');
PREPARE voucher_migration FROM @ddl; EXECUTE voucher_migration; DEALLOCATE PREPARE voucher_migration;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='Coupons' AND column_name='product_id')=0,
    'ALTER TABLE Coupons ADD COLUMN product_id VARCHAR(36) NULL, ADD CONSTRAINT fk_coupon_product FOREIGN KEY (product_id) REFERENCES Products(id)', 'SELECT 1');
PREPARE voucher_migration FROM @ddl; EXECUTE voucher_migration; DEALLOCATE PREPARE voucher_migration;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='Coupons' AND column_name='deleted')=0,
    'ALTER TABLE Coupons ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE', 'SELECT 1');
PREPARE voucher_migration FROM @ddl; EXECUTE voucher_migration; DEALLOCATE PREPARE voucher_migration;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='Orders' AND column_name='shipping_coupon_id')=0,
    'ALTER TABLE Orders ADD COLUMN shipping_coupon_id VARCHAR(36) NULL, ADD CONSTRAINT fk_order_shipping_coupon FOREIGN KEY (shipping_coupon_id) REFERENCES Coupons(id)', 'SELECT 1');
PREPARE voucher_migration FROM @ddl; EXECUTE voucher_migration; DEALLOCATE PREPARE voucher_migration;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='Orders' AND column_name='coupon_code')=0,
    'ALTER TABLE Orders ADD COLUMN coupon_code VARCHAR(50) NULL', 'SELECT 1');
PREPARE voucher_migration FROM @ddl; EXECUTE voucher_migration; DEALLOCATE PREPARE voucher_migration;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='Orders' AND column_name='shipping_coupon_code')=0,
    'ALTER TABLE Orders ADD COLUMN shipping_coupon_code VARCHAR(50) NULL', 'SELECT 1');
PREPARE voucher_migration FROM @ddl; EXECUTE voucher_migration; DEALLOCATE PREPARE voucher_migration;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='Orders' AND column_name='shipping_discount_amount')=0,
    'ALTER TABLE Orders ADD COLUMN shipping_discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE voucher_migration FROM @ddl; EXECUTE voucher_migration; DEALLOCATE PREPARE voucher_migration;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='Orders' AND column_name='coupons_released')=0,
    'ALTER TABLE Orders ADD COLUMN coupons_released BOOLEAN NOT NULL DEFAULT FALSE', 'SELECT 1');
PREPARE voucher_migration FROM @ddl; EXECUTE voucher_migration; DEALLOCATE PREPARE voucher_migration;
UPDATE Orders o JOIN Coupons c ON c.id=o.coupon_id SET o.coupon_code=c.code WHERE o.coupon_code IS NULL;
