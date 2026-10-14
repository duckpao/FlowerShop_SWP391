-- Addresses use the three-level model: province (city), district and ward, with the GHN district id and ward code.
-- Old rows cannot be mapped to GHN codes reliably, so existing addresses are removed first.
-- Orders reference delivery addresses, so orders and everything hanging off them go too.
DELETE FROM Refunds;
DELETE FROM Payments;
DELETE FROM Deliveries;
DELETE FROM Product_Reviews WHERE order_id IS NOT NULL;
DELETE FROM Order_Details;
DELETE FROM Orders;
DELETE FROM Addresses;

ALTER TABLE Addresses ADD COLUMN district VARCHAR(100) NULL AFTER ward;
ALTER TABLE Addresses ADD COLUMN ghn_district_id INT NULL;
ALTER TABLE Addresses ADD COLUMN ghn_ward_code VARCHAR(20) NULL;

ALTER TABLE Manager_Applications ADD COLUMN district VARCHAR(100) NOT NULL DEFAULT '' AFTER city;
