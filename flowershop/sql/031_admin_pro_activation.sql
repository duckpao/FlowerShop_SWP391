-- Apply after 030_shop_subscriptions.sql. Entitlement audit is separate from payment accounting.
USE flower_shop_db;
SET NAMES utf8mb4;
CREATE TABLE IF NOT EXISTS Shop_Pro_Activations (
    id VARCHAR(36) PRIMARY KEY,
    shop_id VARCHAR(36) NOT NULL,
    plan_id VARCHAR(20) NOT NULL,
    duration_months INT NOT NULL,
    source VARCHAR(20) NOT NULL,
    payment_id VARCHAR(36) NULL UNIQUE,
    actor_id VARCHAR(36) NULL,
    note VARCHAR(1000) NOT NULL,
    starts_at DATETIME(6) NOT NULL,
    ends_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_pro_activation_shop FOREIGN KEY (shop_id) REFERENCES Shops(id),
    CONSTRAINT fk_pro_activation_plan FOREIGN KEY (plan_id) REFERENCES Shop_Plans(id),
    CONSTRAINT fk_pro_activation_payment FOREIGN KEY (payment_id) REFERENCES Shop_Plan_Payments(id),
    CONSTRAINT fk_pro_activation_actor FOREIGN KEY (actor_id) REFERENCES Users(id),
    INDEX ix_pro_activation_shop (shop_id,created_at)
);
