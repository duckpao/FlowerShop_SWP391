USE flower_shop_db;
SET NAMES utf8mb4;
CREATE TABLE IF NOT EXISTS Product_Images (
    id NVARCHAR(36) PRIMARY KEY,
    product_id NVARCHAR(36) NOT NULL,
    image_url NVARCHAR(500) NOT NULL, -- URL từ Cloudinary
    is_primary BOOLEAN DEFAULT FALSE, -- Ảnh đại diện sản phẩm
    display_order INT DEFAULT 0, -- Thứ tự hiển thị
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by NVARCHAR(36),
    FOREIGN KEY (product_id) REFERENCES Products(id) ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS Product_Videos (
    id NVARCHAR(36) PRIMARY KEY,
    product_id NVARCHAR(36) NOT NULL,
    video_url NVARCHAR(500) NOT NULL, -- URL từ Cloudinary
    title NVARCHAR(255),
    description TEXT,
    display_order INT DEFAULT 0, -- Thứ tự hiển thị
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by NVARCHAR(36),
    FOREIGN KEY (product_id) REFERENCES Products(id) ON DELETE CASCADE
);
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='Products' AND COLUMN_NAME='admin_hidden'), 'SELECT 1', 'ALTER TABLE Products ADD COLUMN admin_hidden BOOLEAN NOT NULL DEFAULT FALSE');
PREPARE patch_statement FROM @ddl;
EXECUTE patch_statement;
DEALLOCATE PREPARE patch_statement;
SET @ddl = (SELECT CONCAT('ALTER TABLE Product_Reviews MODIFY order_id ', COLUMN_TYPE, ' CHARACTER SET ', CHARACTER_SET_NAME, ' COLLATE ', COLLATION_NAME, ' NULL') FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='Product_Reviews' AND COLUMN_NAME='order_id');
PREPARE patch_statement FROM @ddl;
EXECUTE patch_statement;
DEALLOCATE PREPARE patch_statement;
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='Product_Reviews' AND INDEX_NAME='uq_review_user_product'), 'SELECT 1', 'ALTER TABLE Product_Reviews ADD CONSTRAINT uq_review_user_product UNIQUE (user_id, product_id)');
PREPARE patch_statement FROM @ddl;
EXECUTE patch_statement;
DEALLOCATE PREPARE patch_statement;