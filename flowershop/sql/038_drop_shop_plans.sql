-- Bỏ toàn bộ gói đăng ký Pro của shop: gói, giao dịch mua gói, lịch sử kích hoạt, nhắc hạn,
-- sổ đếm lượt đăng sản phẩm và sản phẩm nổi bật. Sản phẩm, đơn hàng và Products.deleted được giữ nguyên.
-- Thứ tự xóa theo khóa ngoại. Chạy lại được.
USE flower_shop_db;
DROP TABLE IF EXISTS Shop_Pro_Activations;
DROP TABLE IF EXISTS Subscription_Reminders;
DROP TABLE IF EXISTS Shop_Plan_Payments;
DROP TABLE IF EXISTS Shop_Subscriptions;
DROP TABLE IF EXISTS Featured_Products;
DROP TABLE IF EXISTS Shop_Product_Postings;
DROP TABLE IF EXISTS Shop_Plans;
DROP TABLE IF EXISTS Blog_Images; -- không có entity, ảnh blog lưu ở cột Blogs.images (JSON)
