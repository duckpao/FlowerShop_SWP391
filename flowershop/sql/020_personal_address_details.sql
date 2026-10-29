-- Additive migration; existing addresses are retained.
ALTER TABLE Addresses
 ADD COLUMN recipient_name VARCHAR(100) NULL,
 ADD COLUMN recipient_phone VARCHAR(10) NULL,
 ADD COLUMN is_pickup BOOLEAN DEFAULT FALSE,
 ADD COLUMN is_return BOOLEAN DEFAULT FALSE,
 ADD COLUMN address_type VARCHAR(10) DEFAULT 'HOME';
