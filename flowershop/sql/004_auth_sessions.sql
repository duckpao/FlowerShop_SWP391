USE flower_shop_db;
CREATE TABLE IF NOT EXISTS Auth_Sessions (
    id VARCHAR(36) PRIMARY KEY,
    user_id NVARCHAR(36) NOT NULL,
    refresh_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_auth_session_user (user_id),
    FOREIGN KEY (user_id) REFERENCES Users(id)
);
