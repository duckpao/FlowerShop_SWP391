CREATE TABLE IF NOT EXISTS Staff_Applications (
 id VARCHAR(36) PRIMARY KEY,
 shop_id NVARCHAR(36) NOT NULL,
 user_id NVARCHAR(36) NOT NULL,
 full_name VARCHAR(100) NOT NULL,
 phone VARCHAR(20) NOT NULL,
 introduction VARCHAR(1000) NOT NULL,
 status VARCHAR(16) NOT NULL,
 submitted_at DATETIME(6) NOT NULL,
 CONSTRAINT uq_staff_application UNIQUE(shop_id,user_id),
 CONSTRAINT fk_application_shop FOREIGN KEY(shop_id) REFERENCES Shops(id),
 CONSTRAINT fk_application_user FOREIGN KEY(user_id) REFERENCES Users(id)
);
