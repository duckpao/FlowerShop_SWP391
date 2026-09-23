USE flower_shop_db;
-- Keep legacy DELIVERY accounts unchanged for later implementation.
ALTER TABLE Users MODIFY role ENUM('ADMIN','SHOP','CUSTOMER','DELIVERY','SHOP_STAFF') NOT NULL;
DROP PROCEDURE IF EXISTS migrate_context_diagram_roles;
