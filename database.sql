CREATE DATABASE IF NOT EXISTS flower_shop_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE flower_shop_db;
SET NAMES utf8mb4;

-- ==========================================
-- M01: AUTHENTICATION & USER MANAGEMENT
-- ==========================================
CREATE TABLE Users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255),
    google_id VARCHAR(255) UNIQUE,
    full_name VARCHAR(100),
    phone VARCHAR(20),
    role ENUM('ADMIN', 'SHOP', 'CUSTOMER', 'SHOP_STAFF') NOT NULL,
    is_email_verified BOOLEAN DEFAULT FALSE,
    status ENUM('ACTIVE', 'INACTIVE', 'BANNED') DEFAULT 'ACTIVE',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by VARCHAR(36)
);

CREATE TABLE Shops (
    id VARCHAR(36) PRIMARY KEY,
    owner_id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    logo_url VARCHAR(255),
    status ENUM('PENDING', 'ACTIVE', 'INACTIVE', 'BANNED') DEFAULT 'PENDING',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by VARCHAR(36),
    FOREIGN KEY (owner_id) REFERENCES Users(id)
);

CREATE TABLE Addresses (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36), -- NULL nếu là địa chỉ của Shop
    shop_id VARCHAR(36), -- NULL nếu là địa chỉ của User
    address_line VARCHAR(255) NOT NULL,
    ward VARCHAR(100),
    district VARCHAR(100),
    city VARCHAR(100),
    ghn_ward_code VARCHAR(20),
    ghn_district_id INT,
    is_default BOOLEAN DEFAULT FALSE,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by VARCHAR(36),
    FOREIGN KEY (user_id) REFERENCES Users(id),
    FOREIGN KEY (shop_id) REFERENCES Shops(id),
    CONSTRAINT chk_address_owner CHECK ((user_id IS NULL) <> (shop_id IS NULL)) -- đúng 1 trong 2
);

CREATE TABLE Auth_Sessions (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    refresh_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked BOOLEAN DEFAULT FALSE,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by VARCHAR(36),
    FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE
);

CREATE TABLE Otp_Challenges (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    purpose VARCHAR(30) NOT NULL, -- ví dụ: RESET_PASSWORD
    code_hash VARCHAR(255) NOT NULL,
    issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    window_start TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    failed_attempts INT NOT NULL DEFAULT 0,
    send_count INT NOT NULL DEFAULT 1,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by VARCHAR(36),
    FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE
);

CREATE TABLE Pending_Registrations (
    email VARCHAR(255) PRIMARY KEY,
    full_name VARCHAR(100),
    password_hash VARCHAR(255) NOT NULL,
    otp_hash VARCHAR(255) NOT NULL,
    shop_name VARCHAR(255),
    shop_description TEXT,
    issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    window_start TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    failed_attempts INT NOT NULL DEFAULT 0,
    send_count INT NOT NULL DEFAULT 1,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE Manager_Applications (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
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
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by VARCHAR(36),
    FOREIGN KEY (user_id) REFERENCES Users(id)
);

CREATE TABLE Shop_Staff (
    id VARCHAR(36) PRIMARY KEY,
    shop_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by VARCHAR(36),
    FOREIGN KEY (shop_id) REFERENCES Shops(id),
    FOREIGN KEY (user_id) REFERENCES Users(id),
    UNIQUE KEY uq_shop_staff (shop_id, user_id)
);

CREATE TABLE Staff_Applications (
    id VARCHAR(36) PRIMARY KEY,
    shop_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    full_name VARCHAR(100),
    phone VARCHAR(20),
    introduction TEXT,
    status ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING',
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by VARCHAR(36),
    FOREIGN KEY (shop_id) REFERENCES Shops(id),
    FOREIGN KEY (user_id) REFERENCES Users(id)
);

CREATE TABLE Staff_Invitations (
    id VARCHAR(36) PRIMARY KEY,
    shop_id VARCHAR(36) NOT NULL,
    email VARCHAR(255) NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    window_start TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    send_count INT NOT NULL DEFAULT 1,
    status ENUM('PENDING', 'ACCEPTED', 'EXPIRED', 'REVOKED') DEFAULT 'PENDING',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by VARCHAR(36),
    FOREIGN KEY (shop_id) REFERENCES Shops(id)
);

-- ==========================================
-- M02: PRODUCT & CATALOG
-- ==========================================
CREATE TABLE Categories (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    status ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by VARCHAR(36)
);

CREATE TABLE Products (
    id VARCHAR(36) PRIMARY KEY,
    shop_id VARCHAR(36) NOT NULL,
    category_id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    components JSON, -- Thành phần: {"roses": 5, "baby_breath": 2}
    shelf_life_days INT, -- Thời gian bảo quản
    price DECIMAL(12, 2) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    product_type ENUM('READY_MADE', 'CUSTOM') NOT NULL DEFAULT 'READY_MADE',
    status ENUM('ACTIVE', 'INACTIVE', 'OUT_OF_STOCK') DEFAULT 'ACTIVE',
    admin_hidden BOOLEAN NOT NULL DEFAULT FALSE,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by VARCHAR(36),
    FOREIGN KEY (shop_id) REFERENCES Shops(id),
    FOREIGN KEY (category_id) REFERENCES Categories(id)
);

CREATE TABLE Product_Images (
    id VARCHAR(36) PRIMARY KEY,
    product_id VARCHAR(36) NOT NULL,
    image_url VARCHAR(500) NOT NULL, -- URL từ Cloudinary
    is_primary BOOLEAN DEFAULT FALSE,
    display_order INT DEFAULT 0,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by VARCHAR(36),
    FOREIGN KEY (product_id) REFERENCES Products(id) ON DELETE CASCADE
);

CREATE TABLE Product_Videos (
    id VARCHAR(36) PRIMARY KEY,
    product_id VARCHAR(36) NOT NULL,
    video_url VARCHAR(500) NOT NULL, -- URL từ Cloudinary
    title VARCHAR(255),
    description TEXT,
    display_order INT DEFAULT 0,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by VARCHAR(36),
    FOREIGN KEY (product_id) REFERENCES Products(id) ON DELETE CASCADE
);

-- ==========================================
-- VOUCHER / COUPON SYSTEM
-- ==========================================
CREATE TABLE Coupons (
    id VARCHAR(36) PRIMARY KEY,
    shop_id VARCHAR(36), -- NULL nếu là platform voucher
    code VARCHAR(50) UNIQUE NOT NULL,
    discount_type ENUM('PERCENTAGE', 'FIXED_AMOUNT') NOT NULL,
    discount_value DECIMAL(12,2) NOT NULL,
    min_order_value DECIMAL(12,2) DEFAULT 0,
    max_discount_value DECIMAL(12,2),
    start_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ,
    end_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usage_limit INT DEFAULT 1,
    used_count INT DEFAULT 0,
    status ENUM('ACTIVE', 'EXPIRED', 'DISABLED') DEFAULT 'ACTIVE',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by VARCHAR(36),
    FOREIGN KEY (shop_id) REFERENCES Shops(id)
);

-- ==========================================
-- M03: CART & CHECKOUT
-- ==========================================
CREATE TABLE Cart_Items (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    product_id VARCHAR(36) NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by VARCHAR(36),
    FOREIGN KEY (user_id) REFERENCES Users(id),
    FOREIGN KEY (product_id) REFERENCES Products(id),
    UNIQUE KEY uq_cart_user_product (user_id, product_id)
);

-- ==========================================
-- M04, M06: ORDER & FULFILLMENT MANAGEMENT
-- ==========================================
CREATE TABLE Orders (
    id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(36) NOT NULL,
    shop_id VARCHAR(36) NOT NULL,
    delivery_address_id VARCHAR(36) NOT NULL,
    recipient_name VARCHAR(255),
    recipient_phone VARCHAR(20),
    coupon_id VARCHAR(36),
    order_type ENUM('STANDARD', 'CUSTOM') DEFAULT 'STANDARD',
    sub_total DECIMAL(12, 2) NOT NULL,
    discount_amount DECIMAL(12, 2) DEFAULT 0.00,
    shipping_fee DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    total_amount DECIMAL(12, 2) NOT NULL,
    deposit_amount DECIMAL(12, 2) DEFAULT 0.00,
    customer_note TEXT,
    cancel_reason TEXT,
    shop_note TEXT,
    status ENUM('PENDING', 'AWAITING_DEPOSIT', 'DEPOSIT_PAID', 'PROCESSING', 'DELIVERING', 'COMPLETED', 'CANCELLED', 'REFUNDED') DEFAULT 'PENDING',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by VARCHAR(36),
    FOREIGN KEY (customer_id) REFERENCES Users(id),
    FOREIGN KEY (shop_id) REFERENCES Shops(id),
    FOREIGN KEY (delivery_address_id) REFERENCES Addresses(id),
    FOREIGN KEY (coupon_id) REFERENCES Coupons(id)
);

CREATE TABLE Order_Details (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    product_id VARCHAR(36),
    custom_materials JSON, -- {"main_flower": "Hoa hồng đỏ", "wrapper_color": "Đen", "ribbon": "Trắng"}
    price DECIMAL(12, 2) NOT NULL, -- Đơn giá tại thời điểm mua
    quantity INT NOT NULL,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by VARCHAR(36),
    FOREIGN KEY (order_id) REFERENCES Orders(id),
    FOREIGN KEY (product_id) REFERENCES Products(id)
);

-- Giao hàng thông qua đối tác bên ngoài (GHN/GHTK...), không có tài khoản người giao hàng trong hệ thống.
-- delivery_partner_id lưu mã/tên đối tác dạng chuỗi, không phải khóa ngoại.
CREATE TABLE Deliveries (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    delivery_partner_id VARCHAR(36),
    tracking_code VARCHAR(100),
    status ENUM('PENDING', 'PICKED_UP', 'ON_THE_WAY', 'DELIVERED', 'FAILED') DEFAULT 'PENDING',
    tracking_notes TEXT,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by VARCHAR(36),
    FOREIGN KEY (order_id) REFERENCES Orders(id)
);

-- ==========================================
-- CHAT SYSTEM
-- ==========================================
CREATE TABLE Chat_Sessions (
    id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(36) NOT NULL,
    shop_id VARCHAR(36) NOT NULL,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by VARCHAR(36),
    FOREIGN KEY (customer_id) REFERENCES Users(id),
    FOREIGN KEY (shop_id) REFERENCES Shops(id)
);

CREATE TABLE Chat_Messages (
    id VARCHAR(36) PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL,
    sender_id VARCHAR(36) NOT NULL,
    sender_type ENUM('CUSTOMER', 'SHOP') NOT NULL,
    message TEXT,
    media JSON, -- Hình ảnh, video, voucher đính kèm
    is_read BOOLEAN DEFAULT FALSE,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by VARCHAR(36),
    FOREIGN KEY (session_id) REFERENCES Chat_Sessions(id),
    FOREIGN KEY (sender_id) REFERENCES Users(id)
);

-- ==========================================
-- M04: CUSTOM FLOWER REQUEST
-- ==========================================
CREATE TABLE Custom_Order_Requests (
    id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(36) NOT NULL,
    shop_id VARCHAR(36) NOT NULL,
    budget DECIMAL(12, 2) NOT NULL,
    desired_components JSON, -- {"flowers": ["Hoa hồng", "Hoa ly"], "wrapper": "Giấy Kraft"}
    description TEXT,
    status ENUM('REQUESTED', 'NEGOTIATING', 'ACCEPTED', 'REJECTED') DEFAULT 'REQUESTED',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by VARCHAR(36),
    FOREIGN KEY (customer_id) REFERENCES Users(id),
    FOREIGN KEY (shop_id) REFERENCES Shops(id)
);

-- ==========================================
-- M05: PAYMENT & REFUND
-- ==========================================
CREATE TABLE Payments (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    payment_type ENUM('DEPOSIT', 'FINAL', 'FULL') NOT NULL,
    payment_method ENUM('ONLINE', 'COD') NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    gateway_transaction_no VARCHAR(100), -- vnp_TransactionNo
    invoice_number VARCHAR(100) UNIQUE, -- Mã hóa đơn duy nhất dùng cho SePay/IPN
    gateway_response JSON,
    status ENUM('PENDING', 'SUCCESS', 'FAILED') DEFAULT 'PENDING',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by VARCHAR(36),
    FOREIGN KEY (order_id) REFERENCES Orders(id)
);

CREATE TABLE Refunds (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    reason TEXT NOT NULL,
    gateway_refund_id VARCHAR(100),
    gateway_response JSON,
    status ENUM('REQUESTED', 'APPROVED', 'PROCESSED', 'REJECTED') DEFAULT 'REQUESTED',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by VARCHAR(36),
    FOREIGN KEY (order_id) REFERENCES Orders(id)
);

-- ==========================================
-- M07: REVIEW & CUSTOMER ENGAGEMENT
-- ==========================================
CREATE TABLE Product_Reviews (
    id VARCHAR(36) PRIMARY KEY,
    product_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    order_id VARCHAR(36) NULL,
    rating INT CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    media JSON,
    shop_reply TEXT,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by VARCHAR(36),
    FOREIGN KEY (product_id) REFERENCES Products(id),
    FOREIGN KEY (user_id) REFERENCES Users(id),
    FOREIGN KEY (order_id) REFERENCES Orders(id),
    UNIQUE KEY uq_review_user_product (user_id, product_id)
);

CREATE TABLE Favorite_Products (
    user_id VARCHAR(36) NOT NULL,
    product_id VARCHAR(36) NOT NULL,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by VARCHAR(36),
    PRIMARY KEY (user_id, product_id),
    FOREIGN KEY (user_id) REFERENCES Users(id),
    FOREIGN KEY (product_id) REFERENCES Products(id)
);

CREATE TABLE Favorite_Shops (
    user_id VARCHAR(36) NOT NULL,
    shop_id VARCHAR(36) NOT NULL,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by VARCHAR(36),
    PRIMARY KEY (user_id, shop_id),
    FOREIGN KEY (user_id) REFERENCES Users(id),
    FOREIGN KEY (shop_id) REFERENCES Shops(id)
);

-- ==========================================
-- M08: BLOG & NOTIFICATION
-- ==========================================
CREATE TABLE Blogs (
    id VARCHAR(36) PRIMARY KEY,
    shop_id VARCHAR(36) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content LONGTEXT NOT NULL, -- HTML/Rich Text
    thumbnail_url VARCHAR(500),
    status ENUM('DRAFT', 'PUBLISHED', 'HIDDEN') DEFAULT 'PUBLISHED',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by VARCHAR(36),
    FOREIGN KEY (shop_id) REFERENCES Shops(id)
);

CREATE TABLE Blog_Images (
    id VARCHAR(36) PRIMARY KEY,
    blog_id VARCHAR(36) NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    display_order INT DEFAULT 0,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by VARCHAR(36),
    FOREIGN KEY (blog_id) REFERENCES Blogs(id) ON DELETE CASCADE
);

CREATE TABLE Blog_Interactions (
    user_id VARCHAR(36) NOT NULL,
    blog_id VARCHAR(36) NOT NULL,
    interaction_type ENUM('LIKE', 'SAVE') NOT NULL,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by VARCHAR(36),
    PRIMARY KEY (user_id, blog_id, interaction_type),
    FOREIGN KEY (user_id) REFERENCES Users(id),
    FOREIGN KEY (blog_id) REFERENCES Blogs(id)
);

CREATE TABLE Blog_Comments (
    id VARCHAR(36) PRIMARY KEY,
    blog_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    parent_comment_id VARCHAR(36), -- Reply comment
    content TEXT NOT NULL,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by VARCHAR(36),
    FOREIGN KEY (blog_id) REFERENCES Blogs(id),
    FOREIGN KEY (user_id) REFERENCES Users(id),
    FOREIGN KEY (parent_comment_id) REFERENCES Blog_Comments(id)
);

CREATE TABLE Customer_Occasions (
    id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(36) NOT NULL,
    title VARCHAR(255) NOT NULL,
    occasion_date DATE NOT NULL,
    reminder_sent BOOLEAN DEFAULT FALSE,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by VARCHAR(36),
    FOREIGN KEY (customer_id) REFERENCES Users(id)
);

-- ==========================================
-- MOCK DATA
-- ==========================================

-- 1. USERS (Admin, Shop Owner, Shop Staff, Customer)
INSERT INTO Users (id, email, password_hash, full_name, phone, role, is_email_verified, status, created_by) VALUES
('user-admin-01', 'admin@flowershop.com', '$2a$12$z2frH4jxzHPjasvwidmC4.GobzOtpx/Q1fhJyXokmGjsDypKXUPf2', 'System Admin', '0901234567', 'ADMIN', TRUE, 'ACTIVE', 'user-admin-01'),
('user-shop-01', 'shop1@gmail.com', '$2a$12$z2frH4jxzHPjasvwidmC4.GobzOtpx/Q1fhJyXokmGjsDypKXUPf2', 'Nguyễn Đức Bảo', '0912345678', 'SHOP', TRUE, 'ACTIVE', 'user-admin-01'),
('user-staff-01', 'staff1@gmail.com', '$2a$12$z2frH4jxzHPjasvwidmC4.GobzOtpx/Q1fhJyXokmGjsDypKXUPf2', 'Phạm Văn D', '0933445566', 'SHOP_STAFF', TRUE, 'ACTIVE', 'user-shop-01'),
('user-customer-01', 'khachhang1@gmail.com', '$2a$12$z2frH4jxzHPjasvwidmC4.GobzOtpx/Q1fhJyXokmGjsDypKXUPf2', 'Trần Thị B', '0987654321', 'CUSTOMER', TRUE, 'ACTIVE', 'user-customer-01');

-- 2. SHOPS
INSERT INTO Shops (id, owner_id, name, description, logo_url, status, created_by) VALUES
('shop-01', 'user-shop-01', 'FPTU Smart Floral', 'Tiệm hoa sinh viên giá rẻ, thiết kế theo yêu cầu.', 'https://link-to-logo.com/logo1.png', 'ACTIVE', 'user-admin-01');

-- 3. ADDRESSES
INSERT INTO Addresses (id, user_id, shop_id, address_line, ward, district, city, is_default, created_by) VALUES
('addr-shop-01', NULL, 'shop-01', 'Khu Công Nghệ Cao Hòa Lạc', 'Thạch Hòa', 'Thạch Thất', 'Hà Nội', TRUE, 'user-shop-01'),
('addr-customer-01', 'user-customer-01', NULL, 'Số 10, Ngõ 20, Đường Cầu Giấy', 'Dịch Vọng', 'Cầu Giấy', 'Hà Nội', TRUE, 'user-customer-01');

-- 4. CATEGORIES
INSERT INTO Categories (id, name, description, status, created_by) VALUES
('cat-01', 'Hoa Tình Yêu', 'Các mẫu hoa lãng mạn dành cho cặp đôi', 'ACTIVE', 'user-admin-01'),
('cat-02', 'Hoa Khai Trương', 'Kệ hoa, lẵng hoa chúc mừng khai trương', 'ACTIVE', 'user-admin-01');

-- 5. PRODUCTS
INSERT INTO Products (id, shop_id, category_id, name, description, components, shelf_life_days, price, stock, status, created_by) VALUES
('prod-01', 'shop-01', 'cat-01', 'Bó Hồng Đỏ Mix Baby', 'Bó hoa hồng đỏ Ecuador mix hoa baby trắng', '{"red_roses": 10, "white_baby_breath": 3, "wrapper": "Giấy Kraft"}', 4, 350000.00, 50, 'ACTIVE', 'user-shop-01'),
('prod-02', 'shop-01', 'cat-02', 'Lẵng Hướng Dương Ban Mai', 'Hoa hướng dương tặng khai trương, tốt nghiệp', '{"sunflowers": 5, "yellow_roses": 5}', 5, 650000.00, 20, 'ACTIVE', 'user-shop-01');

-- 5.1 PRODUCT IMAGES
INSERT INTO Product_Images (id, product_id, image_url, is_primary, display_order, created_by) VALUES
('img-prod-01-01', 'prod-01', 'https://res.cloudinary.com/your-cloud/image/upload/public-id-1.jpg', TRUE, 1, 'user-shop-01'),
('img-prod-01-02', 'prod-01', 'https://res.cloudinary.com/your-cloud/image/upload/public-id-2.jpg', FALSE, 2, 'user-shop-01'),
('img-prod-02-01', 'prod-02', 'https://res.cloudinary.com/your-cloud/image/upload/public-id-3.jpg', TRUE, 1, 'user-shop-01');

-- 5.2 PRODUCT VIDEOS
INSERT INTO Product_Videos (id, product_id, video_url, title, description, display_order, created_by) VALUES
('vid-prod-01-01', 'prod-01', 'https://res.cloudinary.com/your-cloud/video/upload/video-id-1.mp4', 'Hướng dẫn cắm hoa', 'Cách cắm bó hồng đỏ mix baby', 1, 'user-shop-01'),
('vid-prod-02-01', 'prod-02', 'https://res.cloudinary.com/your-cloud/video/upload/video-id-2.mp4', 'Unboxing lẵng hoa', 'Xem cách lẵng hoa được gói', 1, 'user-shop-01');

-- 6. COUPONS
INSERT INTO Coupons (id, shop_id, code, discount_type, discount_value, min_order_value, max_discount_value, start_date, end_date, usage_limit, used_count, created_by) VALUES
('coupon-01', 'shop-01', 'WELCOME10', 'PERCENTAGE', 10.00, 200000.00, 50000.00, '2023-01-01 00:00:00', '2026-12-31 23:59:59', 100, 1, 'user-shop-01');

-- 7. CART ITEMS
INSERT INTO Cart_Items (id, user_id, product_id, quantity, created_by) VALUES
('cart-01', 'user-customer-01', 'prod-02', 1, 'user-customer-01');

-- 8. ORDERS: 2 x 350000 = 700000, giảm 10% tối đa 50000 -> tổng 650000
INSERT INTO Orders (id, customer_id, shop_id, delivery_address_id, recipient_name, recipient_phone, coupon_id, order_type, sub_total, discount_amount, shipping_fee, total_amount, deposit_amount, status, created_by) VALUES
('order-01', 'user-customer-01', 'shop-01', 'addr-customer-01', 'Trần Thị B', '0987654321', 'coupon-01', 'STANDARD', 700000.00, 50000.00, 0.00, 650000.00, 0.00, 'COMPLETED', 'user-customer-01');

-- 9. ORDER DETAILS
INSERT INTO Order_Details (id, order_id, product_id, price, quantity, created_by) VALUES
('ord-dtl-01', 'order-01', 'prod-01', 350000.00, 2, 'user-customer-01');

-- 10. PAYMENTS
INSERT INTO Payments (id, order_id, payment_type, payment_method, amount, gateway_transaction_no, invoice_number, gateway_response, status, created_by) VALUES
('pay-01', 'order-01', 'FULL', 'ONLINE', 650000.00, 'VNP123456789', 'INV-0001', '{"vnp_ResponseCode":"00", "vnp_BankCode":"NCB"}', 'SUCCESS', 'user-customer-01');

-- 11. DELIVERIES (đối tác giao hàng bên ngoài)
INSERT INTO Deliveries (id, order_id, delivery_partner_id, tracking_code, tracking_notes, status, created_by) VALUES
('del-01', 'order-01', 'GHTK', 'GHTK987654321', 'Giao trong giờ hành chính', 'DELIVERED', 'user-shop-01');

-- 12. CHAT
INSERT INTO Chat_Sessions (id, customer_id, shop_id, created_by) VALUES
('session-01', 'user-customer-01', 'shop-01', 'user-customer-01');

INSERT INTO Chat_Messages (id, session_id, sender_id, sender_type, message, is_read, created_by) VALUES
('msg-01', 'session-01', 'user-customer-01', 'CUSTOMER', 'Shop ơi bó hồng đỏ còn hàng không ạ?', TRUE, 'user-customer-01'),
('msg-02', 'session-01', 'user-shop-01', 'SHOP', 'Dạ tiệm em còn nhiều hoa tươi mới nhập sáng nay ạ!', FALSE, 'user-shop-01');

-- 13. CUSTOM ORDER REQUESTS
INSERT INTO Custom_Order_Requests (id, customer_id, shop_id, budget, desired_components, description, status, created_by) VALUES
('req-01', 'user-customer-01', 'shop-01', 1000000.00, '{"main_flowers": ["Hồng Ân", "Lan Hồ Điệp"], "color_tone": "Pastel"}', 'Làm cho mình một lẵng hoa tặng kỷ niệm 10 năm ngày cưới.', 'NEGOTIATING', 'user-customer-01');

-- 14. PRODUCT REVIEWS
INSERT INTO Product_Reviews (id, product_id, user_id, order_id, rating, comment, shop_reply, created_by) VALUES
('rev-01', 'prod-01', 'user-customer-01', 'order-01', 5, 'Hoa rất tươi, giao hàng siêu nhanh. Sẽ ủng hộ shop tiếp!', 'Cảm ơn bạn đã tin tưởng FPTU Smart Floral ạ!', 'user-customer-01');

-- 15. BLOGS & COMMENTS
INSERT INTO Blogs (id, shop_id, title, content, thumbnail_url, status, created_by) VALUES
('blog-01', 'shop-01', 'Cách Giữ Hoa Hồng Tươi Lâu', '<p>Bí quyết giữ hoa hồng tươi lâu đến 7 ngày...</p>', 'https://res.cloudinary.com/your-cloud/image/upload/blog-thumb-id.jpg', 'PUBLISHED', 'user-shop-01');

INSERT INTO Blog_Images (id, blog_id, image_url, display_order, created_by) VALUES
('blog-img-01', 'blog-01', 'https://res.cloudinary.com/your-cloud/image/upload/blog-img-id-1.jpg', 1, 'user-shop-01');

INSERT INTO Blog_Comments (id, blog_id, user_id, parent_comment_id, content, created_by) VALUES
('cmt-01', 'blog-01', 'user-customer-01', NULL, 'Bài viết rất hữu ích, cảm ơn shop!', 'user-customer-01'),
('cmt-02', 'blog-01', 'user-shop-01', 'cmt-01', 'Dạ shop cảm ơn bạn ạ.', 'user-shop-01');

-- 16. FAVORITES & OCCASIONS
INSERT INTO Favorite_Products (user_id, product_id, created_by) VALUES
('user-customer-01', 'prod-01', 'user-customer-01');

INSERT INTO Customer_Occasions (id, customer_id, title, occasion_date, reminder_sent, created_by) VALUES
('occ-01', 'user-customer-01', 'Kỷ niệm ngày cưới', '2026-10-15', FALSE, 'user-customer-01');

-- 17. AUTH & SHOP/STAFF ONBOARDING
INSERT INTO Otp_Challenges (id, user_id, purpose, code_hash, issued_at, expires_at, window_start, failed_attempts, send_count) VALUES
('otp-01', 'user-customer-01', 'RESET_PASSWORD', '$2a$12$z2frH4jxzHPjasvwidmC4.GobzOtpx/Q1fhJyXokmGjsDypKXUPf2', '2026-09-27 09:00:00', '2026-09-27 09:05:00', '2026-09-27 09:00:00', 0, 1);

INSERT INTO Pending_Registrations (email, full_name, password_hash, otp_hash, shop_name, shop_description, issued_at, expires_at, window_start, failed_attempts, send_count) VALUES
('hoaxinh.owner@gmail.com', 'Đỗ Thị Hoa', '$2a$12$z2frH4jxzHPjasvwidmC4.GobzOtpx/Q1fhJyXokmGjsDypKXUPf2', '$2a$12$z2frH4jxzHPjasvwidmC4.GobzOtpx/Q1fhJyXokmGjsDypKXUPf2', 'Hoa Xinh Corner', 'Tiệm hoa nhỏ chuyên hoa cưới và hoa sự kiện.', '2026-09-27 08:00:00', '2026-09-27 08:05:00', '2026-09-27 08:00:00', 0, 1);

INSERT INTO Auth_Sessions (id, user_id, refresh_hash, expires_at, revoked) VALUES
('sess-01', 'user-customer-01', '120c7fd6bccd463cc455a83865ca1e395c080dfbf1c3068fd16739cda0e2e381', '2026-10-04 09:00:00', FALSE);

INSERT INTO Shop_Staff (id, shop_id, user_id, active) VALUES
('staff-01', 'shop-01', 'user-staff-01', TRUE);

INSERT INTO Staff_Applications (id, shop_id, user_id, full_name, phone, introduction, status, submitted_at) VALUES
('staff-app-01', 'shop-01', 'user-staff-01', 'Phạm Văn D', '0933445566', 'Em có kinh nghiệm 1 năm bán hoa, mong được hỗ trợ shop.', 'APPROVED', '2026-09-20 10:00:00');

INSERT INTO Staff_Invitations (id, shop_id, email, token_hash, issued_at, expires_at, window_start, send_count, status) VALUES
('staff-inv-01', 'shop-01', 'staff2@gmail.com', 'fdb7ea0d1c4c9c541c51ea56199e1784310feae640c5ed70f465ddb1c113c336', '2026-09-27 09:00:00', '2026-10-04 09:00:00', '2026-09-27 09:00:00', 1, 'PENDING');

INSERT INTO Manager_Applications (id, user_id, full_name, phone, shop_name, description, address_line, city, district, ward, status, submitted_at) VALUES
('mgr-app-01', 'user-customer-01', 'Trần Thị B', '0987654321', 'B Flower House', 'Xin mở shop hoa online phục vụ khu vực Cầu Giấy.', 'Số 10, Ngõ 20, Đường Cầu Giấy', 'Hà Nội', 'Cầu Giấy', 'Dịch Vọng', 'PENDING', '2026-09-25 14:00:00');