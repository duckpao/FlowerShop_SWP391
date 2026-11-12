-- Safe replacement for 016_pending_shop_address.sql on an existing database.
-- Other migrations also use 016: choose by full filename, never by number alone.
USE flower_shop_db;
SET NAMES utf8mb4;
DELIMITER $$
DROP PROCEDURE IF EXISTS sync_pending_shop_address $$
CREATE PROCEDURE sync_pending_shop_address(IN column_name_value VARCHAR(64), IN definition_value VARCHAR(255))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA=DATABASE() AND LOWER(TABLE_NAME)='pending_registrations'
                     AND LOWER(COLUMN_NAME)=LOWER(column_name_value)) THEN
        SET @pending_address_sql=CONCAT('ALTER TABLE Pending_Registrations ADD COLUMN `',column_name_value,'` ',definition_value);
        PREPARE pending_address_stmt FROM @pending_address_sql;
        EXECUTE pending_address_stmt;
        DEALLOCATE PREPARE pending_address_stmt;
    END IF;
END $$
DELIMITER ;
CALL sync_pending_shop_address('shop_address_line','VARCHAR(255) NULL');
CALL sync_pending_shop_address('shop_city','VARCHAR(100) NULL');
CALL sync_pending_shop_address('shop_district','VARCHAR(100) NULL');
CALL sync_pending_shop_address('shop_ward','VARCHAR(100) NULL');
CALL sync_pending_shop_address('shop_ghn_ward_code','VARCHAR(20) NULL');
CALL sync_pending_shop_address('shop_ghn_district_id','INT NULL');
DROP PROCEDURE sync_pending_shop_address;
