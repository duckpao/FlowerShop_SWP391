-- Optional LOCAL test fixtures. Non-destructive, repeatable inserts.
-- Demo owners use reserved example.test email addresses and cannot sign in
-- until you set a password via your local admin tooling. No followers are added.
SET NAMES utf8mb4;
INSERT IGNORE INTO Users(id, email, full_name, role, is_email_verified, status, created_by)
VALUES
('follow-demo-owner-01','lotus-owner@example.test','Chủ shop Lotus','SHOP',TRUE,'ACTIVE','follow-demo'),
('follow-demo-owner-02','rose-owner@example.test','Chủ shop Rose','SHOP',TRUE,'ACTIVE','follow-demo'),
('follow-demo-owner-03','sunflower-owner@example.test','Chủ shop Sunflower','SHOP',TRUE,'ACTIVE','follow-demo');
INSERT IGNORE INTO Shops(id,owner_id,name,description,status,created_by)
VALUES
('follow-demo-shop-01','follow-demo-owner-01','Lotus Flower Studio','Hoa tươi và bó hoa theo mùa. Shop mẫu để kiểm thử theo dõi.','ACTIVE','follow-demo'),
('follow-demo-shop-02','follow-demo-owner-02','Rose Garden','Hoa hồng, hoa sinh nhật và hoa chúc mừng. Shop mẫu để kiểm thử theo dõi.','ACTIVE','follow-demo'),
('follow-demo-shop-03','follow-demo-owner-03','Sunflower House','Hoa hướng dương và giỏ hoa trang trí. Shop mẫu để kiểm thử theo dõi.','ACTIVE','follow-demo');
