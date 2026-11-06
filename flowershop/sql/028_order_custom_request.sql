-- Đơn đặt cọc (order_type = CUSTOM) sinh ra từ một yêu cầu hoa tùy chỉnh đã được shop báo giá.
-- Lưu id yêu cầu để không tạo 2 đơn đang hoạt động cho cùng một yêu cầu; đơn bị hủy thì khách đặt lại được.
USE flower_shop_db;

ALTER TABLE Orders
    ADD COLUMN custom_request_id VARCHAR(36) NULL AFTER checkout_group,
    ADD INDEX idx_orders_custom_request (custom_request_id);
