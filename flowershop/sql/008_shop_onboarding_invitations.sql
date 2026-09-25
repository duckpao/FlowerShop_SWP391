USE flower_shop_db;
ALTER TABLE Pending_Registrations ADD COLUMN shop_name VARCHAR(255) NULL,
    ADD COLUMN shop_description VARCHAR(5000) NULL;
CREATE TABLE Staff_Invitations (
    id VARCHAR(36) PRIMARY KEY,
    shop_id NVARCHAR(36) NOT NULL,
    email VARCHAR(255) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    issued_at TIMESTAMP(6) NOT NULL,
    window_start TIMESTAMP(6) NOT NULL,
    send_count INT NOT NULL,
    status VARCHAR(16) NOT NULL,
    UNIQUE KEY uq_staff_invite(shop_id,email),
    FOREIGN KEY (shop_id) REFERENCES Shops(id)
);
