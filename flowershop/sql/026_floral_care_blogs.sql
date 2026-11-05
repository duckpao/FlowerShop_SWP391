USE flower_shop_db;
SET NAMES utf8mb4;

-- Older db.sql snapshots did not include the JSON image list used by the Blog entity.
SET @blogs_images_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'Blogs' AND COLUMN_NAME = 'images'
);
SET @add_blogs_images = IF(@blogs_images_exists = 0,
    'ALTER TABLE Blogs ADD COLUMN images JSON NULL AFTER content', 'SELECT 1');
PREPARE blogs_images_statement FROM @add_blogs_images;
EXECUTE blogs_images_statement;
DEALLOCATE PREPARE blogs_images_statement;

-- Supports public and shop feeds without scanning the full table. Safe to rerun.
SET @blogs_public_index_exists = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'Blogs' AND INDEX_NAME = 'idx_blogs_public_feed'
);
SET @add_blogs_public_index = IF(@blogs_public_index_exists = 0,
    'CREATE INDEX idx_blogs_public_feed ON Blogs (status, created_date)', 'SELECT 1');
PREPARE blogs_public_index_statement FROM @add_blogs_public_index;
EXECUTE blogs_public_index_statement;
DEALLOCATE PREPARE blogs_public_index_statement;

SET @blogs_shop_index_exists = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'Blogs' AND INDEX_NAME = 'idx_blogs_shop_feed'
);
SET @add_blogs_shop_index = IF(@blogs_shop_index_exists = 0,
    'CREATE INDEX idx_blogs_shop_feed ON Blogs (shop_id, created_date)', 'SELECT 1');
PREPARE blogs_shop_index_statement FROM @add_blogs_shop_index;
EXECUTE blogs_shop_index_statement;
DEALLOCATE PREPARE blogs_shop_index_statement;
