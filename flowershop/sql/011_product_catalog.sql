-- Schema hỗ trợ kiểm duyệt sản phẩm và review. File SQL là migration, không được gọi theo từng request FE.
USE flower_shop_db;

-- Thứ tự quan trọng: `mysql < file` dừng ở lệnh lỗi đầu tiên, nên lệnh bắt buộc phải đứng trước
-- các lệnh có thể thất bại vì dữ liệu sẵn có. Thiếu cột admin_hidden thì MỌI truy vấn sản phẩm trả 500.

-- 1. Bắt buộc: cờ kiểm duyệt chỉ Admin ghi được; không nằm trong DTO của Manager.
ALTER TABLE Products ADD COLUMN admin_hidden BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. Cho phép đánh giá không gắn đơn hàng. Giá trị order_id hiện có được giữ nguyên.
--    NVARCHAR khớp lineage của database.sql (file mà docker-compose nạp). Nếu database của bạn
--    được tạo từ db.sql (VARCHAR + utf8mb4), hãy đổi thành VARCHAR(36) để không lệch charset với Orders.id.
ALTER TABLE Product_Reviews MODIFY order_id NVARCHAR(36) NULL;

-- 3. Mỗi tài khoản chỉ một đánh giá cho một sản phẩm.
--    Lệnh này dừng lại nếu DB đã có cặp (user_id, product_id) trùng; xử lý dữ liệu trùng rồi chạy lại
--    riêng lệnh này. Hai thay đổi ở trên đã được áp dụng nên ứng dụng vẫn chạy được.
ALTER TABLE Product_Reviews ADD CONSTRAINT uq_review_user_product UNIQUE (user_id, product_id);
