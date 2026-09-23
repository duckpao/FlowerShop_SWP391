CREATE TABLE IF NOT EXISTS Manager_Applications (
 id VARCHAR(36) PRIMARY KEY,
 user_id NVARCHAR(36) NOT NULL UNIQUE,
 full_name VARCHAR(100) NOT NULL,
 phone VARCHAR(20) NOT NULL,
 shop_name VARCHAR(255) NOT NULL,
 description VARCHAR(5000) NOT NULL,
 address_line VARCHAR(255) NOT NULL,
 city VARCHAR(100) NOT NULL,
 district VARCHAR(100) NOT NULL,
 ward VARCHAR(100) NOT NULL,
 status VARCHAR(16) NOT NULL,
 review_note VARCHAR(1000),
 submitted_at DATETIME(6) NOT NULL,
 reviewed_at DATETIME(6),
 reviewed_by VARCHAR(36),
 shop_id VARCHAR(36),
 CONSTRAINT fk_manager_application_user FOREIGN KEY(user_id) REFERENCES Users(id)
);
