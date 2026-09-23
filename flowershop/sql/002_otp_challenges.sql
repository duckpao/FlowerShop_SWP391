-- Additive migration. Old link tokens remain in the database but are no longer accepted.
USE flower_shop_db;
CREATE TABLE IF NOT EXISTS Otp_Challenges (
    id VARCHAR(36) PRIMARY KEY,
    user_id NVARCHAR(36) NOT NULL,
    purpose VARCHAR(20) NOT NULL,
    code_hash VARCHAR(100) NOT NULL,
    issued_at TIMESTAMP(6) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    used_at TIMESTAMP(6) NULL,
    window_start TIMESTAMP(6) NOT NULL,
    failed_attempts INT NOT NULL DEFAULT 0,
    send_count INT NOT NULL DEFAULT 0,
    UNIQUE KEY uq_otp_user_purpose (user_id, purpose),
    CONSTRAINT fk_otp_user FOREIGN KEY (user_id) REFERENCES Users(id)
);
