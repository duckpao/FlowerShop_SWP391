-- Custom flower requests use Orders and the normal payment/delivery workflow.
-- Earlier requests may not have an address; they must supply one before a quote/payment.
ALTER TABLE Orders MODIFY COLUMN delivery_address_id VARCHAR(36) NULL;
ALTER TABLE Orders MODIFY COLUMN status ENUM(
    'PENDING','AWAITING_DEPOSIT','DEPOSIT_PAID','PROCESSING','DELIVERING',
    'COMPLETED','CANCELLED','REFUNDED','AWAITING_QUOTE','QUOTED'
) DEFAULT 'PENDING';
SET @add_payment_start = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'Orders' AND column_name = 'payment_started_at') = 0,
    'ALTER TABLE Orders ADD COLUMN payment_started_at DATETIME NULL', 'SELECT 1');
PREPARE custom_order_migration FROM @add_payment_start;
EXECUTE custom_order_migration;
DEALLOCATE PREPARE custom_order_migration;

-- Move earlier requests into Orders, keeping a missing address for customer completion.
-- Old accepted requests must be quoted again so shipping and payment are explicitly agreed.
START TRANSACTION;
INSERT INTO Orders (id, customer_id, shop_id, delivery_address_id, order_type, sub_total,
    discount_amount, shipping_fee, total_amount, deposit_amount, customer_note,
    status, created_date, created_by, last_modify_by)
SELECT r.id, r.customer_id, r.shop_id, a.id, 'CUSTOM', 0, 0, 0, 0, 0, r.description,
    IF(r.status='REJECTED', 'CANCELLED', 'AWAITING_QUOTE'), r.created_date, r.created_by, r.created_by
FROM Custom_Order_Requests r
LEFT JOIN Addresses a ON a.id=(SELECT a2.id FROM Addresses a2 WHERE a2.user_id=r.customer_id
    AND a2.ghn_district_id IS NOT NULL AND a2.ghn_ward_code IS NOT NULL
    ORDER BY a2.is_default DESC, a2.created_date, a2.id LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM Orders o WHERE o.id=r.id);

INSERT INTO Order_Details (id, order_id, custom_materials, price, quantity, created_by)
SELECT UUID(), r.id, JSON_OBJECT('budget', r.budget, 'description', r.description,
    'preferences', COALESCE(r.desired_components, JSON_OBJECT())), 0, 1, r.created_by
FROM Custom_Order_Requests r JOIN Orders o ON o.id=r.id AND o.order_type='CUSTOM'
WHERE NOT EXISTS (SELECT 1 FROM Order_Details d WHERE d.order_id=r.id);
COMMIT;
