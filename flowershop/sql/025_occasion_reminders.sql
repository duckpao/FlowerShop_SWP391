-- Nhắc dịp đặc biệt qua email: lặp hằng năm, số ngày nhắc trước (mặc định 7), giờ nhắc (mặc định 08:00),
-- và ngày dịp đã được nhắc gần nhất để không gửi lặp.
ALTER TABLE Customer_Occasions
    ADD COLUMN recurring_yearly BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN notify_days_before INT NOT NULL DEFAULT 7,
    ADD COLUMN notify_time TIME NOT NULL DEFAULT '08:00:00',
    ADD COLUMN last_reminded_for DATE NULL,
    ADD INDEX idx_occasions_customer (customer_id);
