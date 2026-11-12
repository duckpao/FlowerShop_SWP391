-- Run once after 030/031. Keep existing payments, subscriptions and admin prices.
USE flower_shop_db;
SET NAMES utf8mb4;
SET @legacy_plan_limit = EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
           AND table_name='Shop_Plans' AND column_name='monthly_product_limit');
SET @rename_plan_limit = IF(@legacy_plan_limit,
    'ALTER TABLE Shop_Plans CHANGE COLUMN monthly_product_limit product_limit INT NULL',
    'SELECT 1');
PREPARE plan_limit_stmt FROM @rename_plan_limit;
EXECUTE plan_limit_stmt;
DEALLOCATE PREPARE plan_limit_stmt;
-- Upgrade only original seed limits; do not overwrite later admin configuration on rerun.
UPDATE Shop_Plans SET product_limit=CASE id WHEN 'PRO1' THEN 15 WHEN 'PRO3' THEN 25 WHEN 'PRO6' THEN 40 ELSE NULL END,
    featured_limit=CASE id WHEN 'PRO1' THEN 3 WHEN 'PRO3' THEN 5 WHEN 'PRO6' THEN 8 WHEN 'PRO12' THEN 15 END
WHERE @legacy_plan_limit AND id IN ('PRO1','PRO3','PRO6','PRO12') AND product_limit IS NULL AND featured_limit=10;
UPDATE Shop_Plans SET product_limit=5, featured_limit=0 WHERE id='FREE';
SET @add_product_deleted = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
           AND table_name='Products' AND column_name='deleted'),
    'SELECT 1','ALTER TABLE Products ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE');
PREPARE product_deleted_stmt FROM @add_product_deleted;
EXECUTE product_deleted_stmt;
DEALLOCATE PREPARE product_deleted_stmt;
