USE flower_shop_db;

-- Existing catalog products are ready-made unless a shop explicitly marks them as custom.
ALTER TABLE Products
    ADD COLUMN product_type VARCHAR(20) NOT NULL DEFAULT 'READY_MADE';
