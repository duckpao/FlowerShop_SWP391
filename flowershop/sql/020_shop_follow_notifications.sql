-- Run once on the existing FlowerShop database before starting the new backend.
-- Re-running is safe: existing rows are retained; existing ACTIVE products are
-- recorded as already published so an ordinary edit does not announce old items.
CREATE TABLE IF NOT EXISTS Shop_Product_Publications (
    product_id VARCHAR(36) PRIMARY KEY,
    published_at DATETIME(6) NOT NULL
);
CREATE TABLE IF NOT EXISTS Shop_Product_Mails (
    id VARCHAR(36) PRIMARY KEY,
    product_id VARCHAR(36) NOT NULL,
    shop_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    sent_at DATETIME(6),
    CONSTRAINT uk_shop_product_mail UNIQUE (product_id, user_id),
    INDEX ix_shop_product_mail_due (status, next_attempt_at, created_at)
);
INSERT IGNORE INTO Shop_Product_Publications(product_id, published_at)
SELECT id, COALESCE(created_date, NOW()) FROM Products WHERE status = 'ACTIVE';
