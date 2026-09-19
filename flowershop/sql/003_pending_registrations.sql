-- Additive only. Existing Users are preserved.
USE flower_shop_db;
CREATE TABLE IF NOT EXISTS Pending_Registrations (
    email NVARCHAR(255) PRIMARY KEY,
    full_name NVARCHAR(100) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    otp_hash VARCHAR(100) NOT NULL,
    issued_at TIMESTAMP(6) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    window_start TIMESTAMP(6) NOT NULL,
    failed_attempts INT NOT NULL DEFAULT 0,
    send_count INT NOT NULL DEFAULT 0
);
