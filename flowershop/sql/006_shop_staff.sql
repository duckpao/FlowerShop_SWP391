USE flower_shop_db;
CREATE TABLE IF NOT EXISTS Shop_Staff (
    id VARCHAR(36) PRIMARY KEY,
    shop_id NVARCHAR(36) NOT NULL,
    user_id NVARCHAR(36) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE KEY uq_shop_staff (shop_id,user_id),
    FOREIGN KEY (shop_id) REFERENCES Shops(id),
    FOREIGN KEY (user_id) REFERENCES Users(id)
);
