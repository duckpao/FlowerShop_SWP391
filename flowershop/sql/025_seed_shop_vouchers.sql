-- Apply after 024_voucher_management.sql, with the application's schema selected.
-- Top up each existing shop to three usable ORDER vouchers; preserve existing codes/usage.
SET NAMES utf8mb4;
INSERT INTO Coupons (id, shop_id, target, product_id, code, discount_type, discount_value,
    min_order_value, max_discount_value, start_date, end_date, usage_limit, used_count,
    status, deleted, created_by, last_modify_by)
SELECT UUID(), s.id, 'ORDER', NULL,
    CONCAT('SHOP', UPPER(LEFT(MD5(s.id), 12)), '_', LPAD(v.percent_off, 2, '0')),
    'PERCENTAGE', v.percent_off, 0, v.discount_cap,
    DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 1 YEAR), 1000, 0,
    'ACTIVE', FALSE, s.owner_id, s.owner_id
FROM Shops s
CROSS JOIN (
    SELECT 1 AS slot, 5 AS percent_off, 20000 AS discount_cap
    UNION ALL SELECT 2, 15, 70000
    UNION ALL SELECT 3, 10, 50000
) v
LEFT JOIN (
    SELECT shop_id, COUNT(*) AS usable_count
    FROM Coupons
    WHERE shop_id IS NOT NULL AND target='ORDER' AND deleted=FALSE AND status='ACTIVE'
      AND start_date<=NOW() AND end_date>NOW() AND used_count<usage_limit
      AND discount_type='PERCENTAGE' AND discount_value>0 AND discount_value<=100
      AND max_discount_value > 0 AND max_discount_value <= 100000
    GROUP BY shop_id
) existing ON existing.shop_id=s.id
WHERE v.slot<=GREATEST(0, 3-COALESCE(existing.usable_count, 0))
  AND NOT EXISTS (
    SELECT 1 FROM Coupons c
    WHERE c.code=CONCAT('SHOP', UPPER(LEFT(MD5(s.id), 12)), '_', LPAD(v.percent_off, 2, '0'))
  );
