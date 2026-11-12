-- SRS 3.3 Unique Identity: không có hai tài khoản trùng số điện thoại.
-- Chuyển chuỗi rỗng thành NULL (MySQL cho phép nhiều NULL trong cột UNIQUE).
UPDATE Users SET phone = NULL WHERE phone = '';

-- Kiểm tra trước: câu lệnh này phải trả về 0 dòng, nếu không phải sửa số trùng rồi mới chạy ALTER bên dưới.
--   SELECT phone, COUNT(*) FROM Users WHERE phone IS NOT NULL GROUP BY phone HAVING COUNT(*) > 1;

ALTER TABLE Users ADD CONSTRAINT uq_users_phone UNIQUE (phone);
