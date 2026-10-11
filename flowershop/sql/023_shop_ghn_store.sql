-- Mỗi shop có một cửa hàng (điểm lấy hàng) riêng trên GHN; NULL = chưa đăng ký, dùng cửa hàng mặc định của server.
ALTER TABLE Shops ADD COLUMN ghn_shop_id INT NULL;
