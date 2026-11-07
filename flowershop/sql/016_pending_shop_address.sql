-- Địa chỉ cửa hàng bắt buộc khi đăng ký role Manager shop.
ALTER TABLE Pending_Registrations
 ADD COLUMN shop_address_line VARCHAR(255) NULL,
 ADD COLUMN shop_city VARCHAR(100) NULL,
 ADD COLUMN shop_district VARCHAR(100) NULL,
 ADD COLUMN shop_ward VARCHAR(100) NULL,
 ADD COLUMN shop_ghn_ward_code VARCHAR(20) NULL,
 ADD COLUMN shop_ghn_district_id INT NULL;
