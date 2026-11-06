-- In-app notifications (bell dropdown). Re-running is safe.
CREATE TABLE IF NOT EXISTS Notifications (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT,
    link VARCHAR(255),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_date DATETIME(6) NOT NULL,
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES Users(id),
    INDEX ix_notification_user (user_id, created_date)
);
