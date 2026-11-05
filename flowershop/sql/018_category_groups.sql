-- Groups categories into the storefront menu sections (modelled on hoayeuthuong.com).
ALTER TABLE Categories ADD COLUMN group_name VARCHAR(50) NULL;

UPDATE Categories SET group_name = 'Chủ đề' WHERE name IN ('Hoa Tình Yêu', 'Hoa Khai Trương', 'Hoa Sinh Nhật', 'Hoa Chúc Mừng', 'Hoa Chia Buồn', 'Hoa Chúc Sức Khỏe', 'Hoa Cảm Ơn', 'Hoa Mừng Tốt Nghiệp', 'Hoa Cưới', 'Hoa Ngày 20/10', 'Hoa Ngày 8/3', 'Hoa Ngày Nhà Giáo', 'Hoa Valentine', 'Hoa Tết', 'Hoa Giáng Sinh');
UPDATE Categories SET group_name = 'Hoa tươi' WHERE name IN ('Hoa Hồng', 'Hoa Hướng Dương', 'Hoa Đồng Tiền', 'Hoa Ly', 'Hoa Cúc', 'Hoa Cẩm Chướng', 'Hoa Cát Tường', 'Baby Flower', 'Lan Hồ Điệp');
UPDATE Categories SET group_name = 'Kiểu dáng' WHERE name IN ('Bó Hoa Tươi', 'Giỏ Hoa Tươi', 'Hộp Hoa Tươi', 'Bình Hoa Tươi', 'Lẵng Hoa', 'Kệ Hoa');
