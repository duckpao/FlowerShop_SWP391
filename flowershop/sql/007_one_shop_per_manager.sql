USE flower_shop_db;
-- Fails without deleting/reassigning data if an owner already has multiple shops.
ALTER TABLE Shops ADD CONSTRAINT uq_shop_owner UNIQUE (owner_id);
