-- Addresses now use only the two-level model (province + ward, since 01/07/2025).
-- Old three-level data cannot be converted reliably, so existing addresses are removed first.
-- Orders reference delivery addresses, so orders and everything hanging off them go too.
DELETE FROM Refunds;
DELETE FROM Payments;
DELETE FROM Deliveries;
DELETE FROM Product_Reviews WHERE order_id IS NOT NULL;
DELETE FROM Order_Details;
DELETE FROM Orders;
DELETE FROM Addresses;

ALTER TABLE Addresses DROP COLUMN district;
ALTER TABLE Addresses DROP COLUMN ghn_ward_code;
ALTER TABLE Addresses DROP COLUMN ghn_district_id;
ALTER TABLE Addresses ADD COLUMN ghn_province_id INT NULL;
ALTER TABLE Addresses ADD COLUMN ghn_ward_id INT NULL;

ALTER TABLE Manager_Applications DROP COLUMN district;
