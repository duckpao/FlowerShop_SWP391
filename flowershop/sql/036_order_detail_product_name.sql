-- SRS 3.3 Order Integrity: đơn lưu tên sản phẩm lúc mua, đổi tên sản phẩm sau này không làm đổi đơn cũ.
ALTER TABLE Order_Details ADD COLUMN product_name VARCHAR(255) NULL AFTER product_id;

-- Đơn cũ: lấy tên hiện tại của sản phẩm làm tên lúc mua (gần đúng nhất có thể).
UPDATE Order_Details od JOIN Products p ON p.id = od.product_id
SET od.product_name = p.name
WHERE od.product_name IS NULL;
