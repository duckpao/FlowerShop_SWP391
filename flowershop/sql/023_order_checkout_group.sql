-- Các đơn (mỗi shop một đơn) tạo trong cùng một lần thanh toán giỏ hàng có chung checkout_group; NULL với đơn đặt lẻ.
ALTER TABLE Orders
    ADD COLUMN checkout_group VARCHAR(36) NULL,
    ADD INDEX idx_orders_checkout_group (checkout_group);
