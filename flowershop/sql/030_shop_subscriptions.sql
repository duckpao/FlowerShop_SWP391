-- Shop Free/Pro plans. Additive migration; no existing products or orders are removed.
USE flower_shop_db;
SET NAMES utf8mb4;
SET time_zone = '+07:00'; -- Backfill TIMESTAMP creation dates into the Vietnam calendar-month ledger.
CREATE TABLE IF NOT EXISTS Shop_Plans (
    id VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    duration_months INT NOT NULL,
    price DECIMAL(12,2) NOT NULL,
    monthly_product_limit INT NULL,
    featured_limit INT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE IF NOT EXISTS Shop_Subscriptions (
    id VARCHAR(36) PRIMARY KEY,
    shop_id VARCHAR(36) NOT NULL UNIQUE,
    plan_id VARCHAR(20) NOT NULL,
    started_at DATETIME(6) NOT NULL,
    ends_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_subscription_shop FOREIGN KEY (shop_id) REFERENCES Shops(id),
    CONSTRAINT fk_subscription_plan FOREIGN KEY (plan_id) REFERENCES Shop_Plans(id),
    INDEX ix_subscription_end (ends_at)
);
CREATE TABLE IF NOT EXISTS Shop_Plan_Payments (
    id VARCHAR(36) PRIMARY KEY,
    shop_id VARCHAR(36) NOT NULL,
    plan_id VARCHAR(20) NOT NULL,
    duration_months INT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    invoice_number VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME(6) NOT NULL,
    confirmed_at DATETIME(6),
    last_checked_at DATETIME(6),
    transaction_no VARCHAR(100),
    CONSTRAINT fk_plan_payment_shop FOREIGN KEY (shop_id) REFERENCES Shops(id),
    CONSTRAINT fk_plan_payment_plan FOREIGN KEY (plan_id) REFERENCES Shop_Plans(id),
    INDEX ix_plan_payment_pending (status, last_checked_at, created_at)
);
-- No product FK: deleting a product does not refund this month's quota.
CREATE TABLE IF NOT EXISTS Shop_Product_Postings (
    id VARCHAR(36) PRIMARY KEY,
    shop_id VARCHAR(36) NOT NULL,
    posted_at DATETIME(6) NOT NULL,
    INDEX ix_product_posting_month (shop_id, posted_at),
    CONSTRAINT fk_product_posting_shop FOREIGN KEY (shop_id) REFERENCES Shops(id)
);
CREATE TABLE IF NOT EXISTS Featured_Products (
    product_id VARCHAR(36) PRIMARY KEY,
    shop_id VARCHAR(36) NOT NULL,
    featured_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_featured_product FOREIGN KEY (product_id) REFERENCES Products(id) ON DELETE CASCADE,
    CONSTRAINT fk_featured_shop FOREIGN KEY (shop_id) REFERENCES Shops(id)
);
CREATE TABLE IF NOT EXISTS Subscription_Reminders (
    id VARCHAR(36) PRIMARY KEY,
    subscription_id VARCHAR(36) NOT NULL,
    expected_end DATETIME(6) NOT NULL,
    reminder_date DATE NOT NULL,
    hour_slot INT NOT NULL,
    mail_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_reminder_subscription FOREIGN KEY (subscription_id) REFERENCES Shop_Subscriptions(id),
    CONSTRAINT uq_subscription_reminder UNIQUE (subscription_id,expected_end,reminder_date,hour_slot),
    INDEX ix_subscription_reminder_mail (mail_status,next_attempt_at)
);
-- 3-month discount = 1 - 149000/(59000*3) = 15.8192%.
-- Same discount for 6/12 months: 298000 / 596000 VND.
INSERT INTO Shop_Plans(id,name,duration_months,price,monthly_product_limit,featured_limit,enabled) VALUES
('FREE','Free',0,0,5,0,TRUE),
('PRO1','Pro 1 tháng',1,59000,NULL,10,TRUE),
('PRO3','Pro 3 tháng',3,149000,NULL,10,TRUE),
('PRO6','Pro 6 tháng',6,298000,NULL,10,TRUE),
('PRO12','Pro 1 năm',12,596000,NULL,10,TRUE)
ON DUPLICATE KEY UPDATE id=Shop_Plans.id;
INSERT IGNORE INTO Shop_Product_Postings(id,shop_id,posted_at)
SELECT id,shop_id,COALESCE(created_date,NOW()) FROM Products;
