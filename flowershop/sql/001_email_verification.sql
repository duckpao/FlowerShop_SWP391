-- Run once against the existing database; safe to rerun. No existing data is deleted.
USE flower_shop_db;

CREATE TABLE IF NOT EXISTS Email_Verification_Tokens (
    user_id NVARCHAR(36) PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    issued_at TIMESTAMP(6) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    used_at TIMESTAMP(6) NULL,
    CONSTRAINT fk_email_verification_user FOREIGN KEY (user_id) REFERENCES Users(id)
);
