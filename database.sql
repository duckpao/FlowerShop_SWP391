DROP DATABASE IF EXISTS flower_shop_db; 
CREATE DATABASE flower_shop_db;
USE flower_shop_db;
-- ==========================================
-- M01: AUTHENTICATION & USER MANAGEMENT
-- ==========================================
CREATE TABLE Users (
    id NVARCHAR(36) PRIMARY KEY,
    email NVARCHAR(255) UNIQUE NOT NULL,
    password_hash NVARCHAR(255),
    google_id NVARCHAR(255) UNIQUE,
    full_name NVARCHAR(100),
    phone NVARCHAR(20),
    role ENUM('ADMIN', 'SHOP', 'CUSTOMER', 'SHOP_STAFF', 'DELIVERY') NOT NULL,
    is_email_verified BOOLEAN DEFAULT FALSE,
    status ENUM('ACTIVE', 'INACTIVE', 'BANNED') DEFAULT 'ACTIVE',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by NVARCHAR(36)
);

CREATE TABLE Shops (
    id NVARCHAR(36) PRIMARY KEY,
    owner_id NVARCHAR(36) NOT NULL,
    name NVARCHAR(255) NOT NULL,
    description TEXT,
    logo_url NVARCHAR(255),
    status ENUM('PENDING', 'ACTIVE', 'INACTIVE', 'BANNED') DEFAULT 'PENDING',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by NVARCHAR(36),
    FOREIGN KEY (owner_id) REFERENCES Users(id)
);

CREATE TABLE Addresses (
    id NVARCHAR(36) PRIMARY KEY,
    user_id NVARCHAR(36), -- Nullable nếu là địa chỉ của Shop
    shop_id NVARCHAR(36), -- Nullable nếu là địa chỉ của User
    address_line NVARCHAR(255) NOT NULL,
    ward NVARCHAR(100),
    district NVARCHAR(100),
    city NVARCHAR(100),
    ghn_ward_code NVARCHAR(20),
    ghn_district_id INT,
    is_default BOOLEAN DEFAULT FALSE,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by NVARCHAR(36),
    FOREIGN KEY (user_id) REFERENCES Users(id),
    FOREIGN KEY (shop_id) REFERENCES Shops(id)
);

-- ==========================================
-- M02: PRODUCT & CATALOG
-- ==========================================
CREATE TABLE Categories (
    id NVARCHAR(36) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL,
    description TEXT,
    status ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by NVARCHAR(36)
);

CREATE TABLE Products (
    id NVARCHAR(36) PRIMARY KEY,
    shop_id NVARCHAR(36) NOT NULL,
    category_id NVARCHAR(36) NOT NULL,
    name NVARCHAR(255) NOT NULL,
    description TEXT,
    components JSON, -- Lưu thành phần: {"roses": 5, "baby_breath": 2}
    shelf_life_days INT, -- Thời gian bảo quản (ví dụ: 3-5 ngày)
    price DECIMAL(12, 2) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    product_type ENUM('READY_MADE', 'CUSTOM') NOT NULL DEFAULT 'READY_MADE',
    status ENUM('ACTIVE', 'INACTIVE', 'OUT_OF_STOCK') DEFAULT 'ACTIVE',
    admin_hidden BOOLEAN NOT NULL DEFAULT FALSE,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by NVARCHAR(36),
    FOREIGN KEY (shop_id) REFERENCES Shops(id),
    FOREIGN KEY (category_id) REFERENCES Categories(id)
);

CREATE TABLE Product_Images (
    id NVARCHAR(36) PRIMARY KEY,
    product_id NVARCHAR(36) NOT NULL,
    image_url NVARCHAR(500) NOT NULL, -- URL từ Cloudinary
    is_primary BOOLEAN DEFAULT FALSE, -- Ảnh đại diện sản phẩm
    display_order INT DEFAULT 0, -- Thứ tự hiển thị
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by NVARCHAR(36),
    FOREIGN KEY (product_id) REFERENCES Products(id) ON DELETE CASCADE
);

CREATE TABLE Product_Videos (
    id NVARCHAR(36) PRIMARY KEY,
    product_id NVARCHAR(36) NOT NULL,
    video_url NVARCHAR(500) NOT NULL, -- URL từ Cloudinary
    title NVARCHAR(255),
    description TEXT,
    display_order INT DEFAULT 0, -- Thứ tự hiển thị
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by NVARCHAR(36),
    FOREIGN KEY (product_id) REFERENCES Products(id) ON DELETE CASCADE
);

-- ==========================================
-- NEW: VOUCHER / COUPON SYSTEM
-- ==========================================
CREATE TABLE Coupons (
    id NVARCHAR(36) PRIMARY KEY,
    shop_id NVARCHAR(36), -- Null nếu là platform voucher
    code NVARCHAR(50) UNIQUE NOT NULL,
    discount_type ENUM('PERCENTAGE', 'FIXED_AMOUNT') NOT NULL,
    discount_value DECIMAL(12,2) NOT NULL,
    min_order_value DECIMAL(12,2) DEFAULT 0,
    max_discount_value DECIMAL(12,2),
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    usage_limit INT DEFAULT 1,
    used_count INT DEFAULT 0,
    status ENUM('ACTIVE', 'EXPIRED', 'DISABLED') DEFAULT 'ACTIVE',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by NVARCHAR(36),
    FOREIGN KEY (shop_id) REFERENCES Shops(id)
);

-- ==========================================
-- M03: CART & CHECKOUT
-- ==========================================
CREATE TABLE Cart_Items (
    id NVARCHAR(36) PRIMARY KEY,
    user_id NVARCHAR(36) NOT NULL,
    product_id NVARCHAR(36) NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by NVARCHAR(36),
    FOREIGN KEY (user_id) REFERENCES Users(id),
    FOREIGN KEY (product_id) REFERENCES Products(id)
);

-- ==========================================
-- M04, M06: ORDER & FULFILLMENT MANAGEMENT
-- ==========================================
CREATE TABLE Orders (
    id NVARCHAR(36) PRIMARY KEY,
    customer_id NVARCHAR(36) NOT NULL,
    shop_id NVARCHAR(36) NOT NULL,
    delivery_address_id NVARCHAR(36) NOT NULL,
    recipient_name NVARCHAR(255),
    recipient_phone NVARCHAR(20),
    coupon_id NVARCHAR(36),
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
    created_by NVARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by NVARCHAR(36),
    FOREIGN KEY (customer_id) REFERENCES Users(id),
    FOREIGN KEY (shop_id) REFERENCES Shops(id),
    FOREIGN KEY (delivery_address_id) REFERENCES Addresses(id),
    FOREIGN KEY (coupon_id) REFERENCES Coupons(id)
);

CREATE TABLE Order_Details (
    id NVARCHAR(36) PRIMARY KEY,
    order_id NVARCHAR(36) NOT NULL,
    product_id NVARCHAR(36),
    custom_materials JSON, -- Lưu tùy chọn custom: {"main_flower": "Hoa hồng đỏ", "wrapper_color": "Đen", "ribbon": "Trắng"}
    price DECIMAL(12, 2) NOT NULL,
    quantity INT NOT NULL,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by NVARCHAR(36),
    FOREIGN KEY (order_id) REFERENCES Orders(id),
    FOREIGN KEY (product_id) REFERENCES Products(id)
);

CREATE TABLE Deliveries (
    id NVARCHAR(36) PRIMARY KEY,
    order_id NVARCHAR(36) NOT NULL,
    delivery_partner_id NVARCHAR(36),
    tracking_code NVARCHAR(100),
    status ENUM('PENDING', 'PICKED_UP', 'ON_THE_WAY', 'DELIVERED', 'FAILED') DEFAULT 'PENDING',
    tracking_notes TEXT,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by NVARCHAR(36),
    FOREIGN KEY (order_id) REFERENCES Orders(id)
);

-- ==========================================
-- NEW: SHOPPEE-STYLE CHAT SYSTEM
-- ==========================================
CREATE TABLE Chat_Sessions (
    id NVARCHAR(36) PRIMARY KEY,
    customer_id NVARCHAR(36) NOT NULL,
    shop_id NVARCHAR(36) NOT NULL,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by NVARCHAR(36),
    FOREIGN KEY (customer_id) REFERENCES Users(id),
    FOREIGN KEY (shop_id) REFERENCES Shops(id)
);

CREATE TABLE Chat_Messages (
    id NVARCHAR(36) PRIMARY KEY,
    session_id NVARCHAR(36) NOT NULL,
    sender_id NVARCHAR(36) NOT NULL,
    sender_type ENUM('CUSTOMER', 'SHOP') NOT NULL,
    message TEXT,
    media JSON, -- Hình ảnh, video, voucher đính kèm trong chat
    is_read BOOLEAN DEFAULT FALSE,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by NVARCHAR(36),
    FOREIGN KEY (session_id) REFERENCES Chat_Sessions(id),
    FOREIGN KEY (sender_id) REFERENCES Users(id)
);

-- ==========================================
-- M04: CUSTOM FLOWER REQUEST (Tích hợp từ Chat)
-- ==========================================
CREATE TABLE Custom_Order_Requests (
    id NVARCHAR(36) PRIMARY KEY,
    customer_id NVARCHAR(36) NOT NULL,
    shop_id NVARCHAR(36) NOT NULL,
    budget DECIMAL(12, 2) NOT NULL,
    desired_components JSON, -- Khách chọn: {"flowers": ["Hoa hồng", "Hoa ly"], "wrapper": "Giấy Kraft"}
    description TEXT,
    status ENUM('REQUESTED', 'NEGOTIATING', 'ACCEPTED', 'REJECTED') DEFAULT 'REQUESTED',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by NVARCHAR(36),
    FOREIGN KEY (customer_id) REFERENCES Users(id),
    FOREIGN KEY (shop_id) REFERENCES Shops(id)
);

-- ==========================================
-- M05: PAYMENT & REFUND (Tích hợp API Response)
-- ==========================================
CREATE TABLE Payments (
    id NVARCHAR(36) PRIMARY KEY,
    order_id NVARCHAR(36) NOT NULL,
    payment_type ENUM('DEPOSIT', 'FINAL', 'FULL') NOT NULL,
    payment_method ENUM('ONLINE', 'COD') NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    gateway_transaction_no NVARCHAR(100), -- Mã giao dịch từ cổng thanh toán (vnp_TransactionNo)
    invoice_number NVARCHAR(100) UNIQUE, -- Mã hóa đơn duy nhất dùng cho SePay Gateway/IPN
    gateway_response JSON, -- Lưu toàn bộ payload response từ API để làm đối soát/hoàn tiền
    status ENUM('PENDING', 'SUCCESS', 'FAILED') DEFAULT 'PENDING',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by NVARCHAR(36),
    FOREIGN KEY (order_id) REFERENCES Orders(id)
);

CREATE TABLE Refunds (
    id NVARCHAR(36) PRIMARY KEY,
    order_id NVARCHAR(36) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    reason TEXT NOT NULL,
    gateway_refund_id NVARCHAR(100), -- Mã hoàn tiền từ cổng thanh toán
    gateway_response JSON, -- Lưu payload response khi gọi API Refund
    status ENUM('REQUESTED', 'APPROVED', 'PROCESSED', 'REJECTED') DEFAULT 'REQUESTED',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by NVARCHAR(36),
    FOREIGN KEY (order_id) REFERENCES Orders(id)
);

-- ==========================================
-- M07: REVIEW & CUSTOMER ENGAGEMENT
-- ==========================================
CREATE TABLE Product_Reviews (
    id NVARCHAR(36) PRIMARY KEY,
    product_id NVARCHAR(36) NOT NULL,
    user_id NVARCHAR(36) NOT NULL,
    order_id NVARCHAR(36) NULL, -- Đánh giá dựa trên đơn hàng thực tế
    rating INT CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    media JSON, -- Ảnh, video khách hàng tải lên
    shop_reply TEXT, -- Cửa hàng phản hồi đánh giá
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by NVARCHAR(36),
    FOREIGN KEY (product_id) REFERENCES Products(id),
    FOREIGN KEY (user_id) REFERENCES Users(id),
    FOREIGN KEY (order_id) REFERENCES Orders(id),
    UNIQUE KEY uq_review_user_product (user_id, product_id)
);

CREATE TABLE Favorite_Products (
    user_id NVARCHAR(36) NOT NULL,
    product_id NVARCHAR(36) NOT NULL,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by NVARCHAR(36),
    PRIMARY KEY (user_id, product_id),
    FOREIGN KEY (user_id) REFERENCES Users(id),
    FOREIGN KEY (product_id) REFERENCES Products(id)
);

CREATE TABLE Favorite_Shops (
    user_id NVARCHAR(36) NOT NULL,
    shop_id NVARCHAR(36) NOT NULL,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by NVARCHAR(36),
    PRIMARY KEY (user_id, shop_id),
    FOREIGN KEY (user_id) REFERENCES Users(id),
    FOREIGN KEY (shop_id) REFERENCES Shops(id)
);

-- ==========================================
-- M08: BLOG & NOTIFICATION (Nâng cấp Cấu trúc bài viết)
-- ==========================================
CREATE TABLE Blogs (
    id NVARCHAR(36) PRIMARY KEY,
    shop_id NVARCHAR(36) NOT NULL,
    title NVARCHAR(255) NOT NULL,
    content LONGTEXT NOT NULL, -- Hỗ trợ HTML/Rich Text Format như bài báo
    thumbnail_url NVARCHAR(500), -- URL ảnh đại diện từ Cloudinary
    status ENUM('DRAFT', 'PUBLISHED', 'HIDDEN') DEFAULT 'PUBLISHED',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by NVARCHAR(36),
    FOREIGN KEY (shop_id) REFERENCES Shops(id)
);

CREATE TABLE Blog_Images (
    id NVARCHAR(36) PRIMARY KEY,
    blog_id NVARCHAR(36) NOT NULL,
    image_url NVARCHAR(500) NOT NULL, -- URL từ Cloudinary
    display_order INT DEFAULT 0, -- Thứ tự hiển thị
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by NVARCHAR(36),
    FOREIGN KEY (blog_id) REFERENCES Blogs(id) ON DELETE CASCADE
);

CREATE TABLE Blog_Interactions (
    user_id NVARCHAR(36) NOT NULL,
    blog_id NVARCHAR(36) NOT NULL,
    interaction_type ENUM('LIKE', 'SAVE') NOT NULL,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by NVARCHAR(36),
    PRIMARY KEY (user_id, blog_id, interaction_type),
    FOREIGN KEY (user_id) REFERENCES Users(id),
    FOREIGN KEY (blog_id) REFERENCES Blogs(id)
);

CREATE TABLE Blog_Comments (
    id NVARCHAR(36) PRIMARY KEY,
    blog_id NVARCHAR(36) NOT NULL,
    user_id NVARCHAR(36) NOT NULL,
    parent_comment_id NVARCHAR(36), -- Hỗ trợ Reply Comment
    content TEXT NOT NULL,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by NVARCHAR(36),
    FOREIGN KEY (blog_id) REFERENCES Blogs(id),
    FOREIGN KEY (user_id) REFERENCES Users(id),
    FOREIGN KEY (parent_comment_id) REFERENCES Blog_Comments(id)
);

CREATE TABLE Customer_Occasions (
    id NVARCHAR(36) PRIMARY KEY,
    customer_id NVARCHAR(36) NOT NULL,
    title NVARCHAR(255) NOT NULL,
    occasion_date DATE NOT NULL,
    reminder_sent BOOLEAN DEFAULT FALSE,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by NVARCHAR(36),
    FOREIGN KEY (customer_id) REFERENCES Users(id)
);
-- ==========================================
-- SCRIPT INSERT MOCK DATA - FLOWER SHOP DB
-- ==========================================

-- 1. INSERT USERS (Admin, Shop Owner, Customer, Delivery)
INSERT INTO Users (id, email, password_hash, full_name, phone, role, status, created_by) VALUES
('user-admin-01', 'admin@flowershop.com', '$2a$12$z2frH4jxzHPjasvwidmC4.GobzOtpx/Q1fhJyXokmGjsDypKXUPf2', 'System Admin', '0901234567', 'ADMIN', 'ACTIVE', 'user-admin-01'),
('user-shop-01', 'shop1@gmail.com', '$2a$12$z2frH4jxzHPjasvwidmC4.GobzOtpx/Q1fhJyXokmGjsDypKXUPf2', 'Nguyễn Đức Bảo', '0912345678', 'SHOP', 'ACTIVE', 'user-admin-01'),
('user-customer-01', 'khachhang1@gmail.com', '$2a$12$z2frH4jxzHPjasvwidmC4.GobzOtpx/Q1fhJyXokmGjsDypKXUPf2', 'Trần Thị B', '0987654321', 'CUSTOMER', 'ACTIVE', 'user-customer-01'),
('user-delivery-01', 'shipper1@ahamove.com', '$2a$12$z2frH4jxzHPjasvwidmC4.GobzOtpx/Q1fhJyXokmGjsDypKXUPf2', 'Lê Văn C', '0922334455', 'DELIVERY', 'ACTIVE', 'user-admin-01');

-- 2. INSERT SHOPS
INSERT INTO Shops (id, owner_id, name, description, logo_url, status, created_by) VALUES
('shop-01', 'user-shop-01', 'FPTU Smart Floral', 'Tiệm hoa sinh viên giá rẻ, thiết kế theo yêu cầu.', 'https://link-to-logo.com/logo1.png', 'ACTIVE', 'user-admin-01');

-- 3. INSERT ADDRESSES (1 địa chỉ cho Shop, 1 địa chỉ cho Khách)
INSERT INTO Addresses (id, user_id, shop_id, address_line, ward, district, city, is_default, created_by) VALUES
('addr-shop-01', NULL, 'shop-01', 'Khu Công Nghệ Cao Hòa Lạc', 'Thạch Hòa', 'Thạch Thất', 'Hà Nội', TRUE, 'user-shop-01'),
('addr-customer-01', 'user-customer-01', NULL, 'Số 10, Ngõ 20, Đường Cầu Giấy', 'Dịch Vọng', 'Cầu Giấy', 'Hà Nội', TRUE, 'user-customer-01');

-- 4. INSERT CATEGORIES
INSERT INTO Categories (id, name, description, status, created_by) VALUES
('cat-01', 'Hoa Tình Yêu', 'Các mẫu hoa lãng mạn dành cho cặp đôi', 'ACTIVE', 'user-admin-01'),
('cat-02', 'Hoa Khai Trương', 'Kệ hoa, lẵng hoa chúc mừng khai trương', 'ACTIVE', 'user-admin-01');

-- 5. INSERT PRODUCTS
INSERT INTO Products (id, shop_id, category_id, name, description, components, shelf_life_days, price, stock, status, created_by) VALUES
('prod-01', 'shop-01', 'cat-01', 'Bó Hồng Đỏ Mix Baby', 'Bó hoa hồng đỏ Ecuador mix hoa baby trắng', '{"red_roses": 10, "white_baby_breath": 3, "wrapper": "Giấy Kraft"}', 4, 1000.00, 50, 'ACTIVE', 'user-shop-01'),
('prod-02', 'shop-01', 'cat-02', 'Lẵng Hướng Dương Ban Mai', 'Hoa hướng dương tặng khai trương, tốt nghiệp', '{"sunflowers": 5, "yellow_roses": 5}', 5, 2000.00, 20, 'ACTIVE', 'user-shop-01');

-- 5.1 INSERT PRODUCT IMAGES (Từ Cloudinary)
INSERT INTO Product_Images (id, product_id, image_url, is_primary, display_order, created_by) VALUES
('img-prod-01-01', 'prod-01', 'https://res.cloudinary.com/[your-cloud]/image/upload/[public-id-1].jpg', TRUE, 1, 'user-shop-01'),
('img-prod-01-02', 'prod-01', 'https://res.cloudinary.com/[your-cloud]/image/upload/[public-id-2].jpg', FALSE, 2, 'user-shop-01'),
('img-prod-02-01', 'prod-02', 'https://res.cloudinary.com/[your-cloud]/image/upload/[public-id-3].jpg', TRUE, 1, 'user-shop-01');

-- 5.2 INSERT PRODUCT VIDEOS (Từ Cloudinary)
INSERT INTO Product_Videos (id, product_id, video_url, title, description, display_order, created_by) VALUES
('vid-prod-01-01', 'prod-01', 'https://res.cloudinary.com/[your-cloud]/video/upload/[video-id-1].mp4', 'Hướng dẫn cắm hoa', 'Cách cắm bó hồng đỏ mix baby', 1, 'user-shop-01'),
('vid-prod-02-01', 'prod-02', 'https://res.cloudinary.com/[your-cloud]/video/upload/[video-id-2].mp4', 'Unboxing lẵng hoa', 'Xem cách lẵng hoa được gói', 1, 'user-shop-01');

-- 6. INSERT COUPONS
INSERT INTO Coupons (id, shop_id, code, discount_type, discount_value, min_order_value, max_discount_value, start_date, end_date, usage_limit, created_by) VALUES
('coupon-01', 'shop-01', 'WELCOME10', 'PERCENTAGE', 10.00, 200000.00, 50000.00, '2023-01-01 00:00:00', '2026-12-31 23:59:59', 100, 'user-shop-01');

-- 7. INSERT CART ITEMS
INSERT INTO Cart_Items (id, user_id, product_id, quantity, created_by) VALUES
('cart-01', 'user-customer-01', 'prod-01', 2, 'user-customer-01');

-- 8. INSERT ORDERS (Đơn hàng Standard)
INSERT INTO Orders (id, customer_id, shop_id, delivery_address_id, coupon_id, order_type, sub_total, discount_amount, total_amount, deposit_amount, status, created_by) VALUES
('order-01', 'user-customer-01', 'shop-01', 'addr-customer-01', 'coupon-01', 'STANDARD', 700000.00, 50000.00, 650000.00, 0.00, 'DELIVERING', 'user-customer-01');

-- 9. INSERT ORDER DETAILS
INSERT INTO Order_Details (id, order_id, product_id, price, quantity, created_by) VALUES
('ord-dtl-01', 'order-01', 'prod-01', 1000.00, 2, 'user-customer-01');

-- 10. INSERT PAYMENTS (Khách đã thanh toán online)
INSERT INTO Payments (id, order_id, payment_type, payment_method, amount, gateway_transaction_no, gateway_response, status, created_by) VALUES
('pay-01', 'order-01', 'FULL', 'ONLINE', 650000.00, 'VNP123456789', '{"vnp_ResponseCode":"00", "vnp_BankCode":"NCB"}', 'SUCCESS', 'user-customer-01');

-- 11. INSERT DELIVERIES (Đã giao cho đối tác)
INSERT INTO Deliveries (id, order_id, delivery_partner_id, tracking_code, tracking_notes, status, created_by) VALUES
('del-01', 'order-01', 'GHTK_PARTNER', 'GHTK987654321', 'Giao trong giờ hành chính', 'ON_THE_WAY', 'user-shop-01');

-- 12. INSERT CHAT SYSTEM
INSERT INTO Chat_Sessions (id, customer_id, shop_id, created_by) VALUES
('session-01', 'user-customer-01', 'shop-01', 'user-customer-01');

INSERT INTO Chat_Messages (id, session_id, sender_id, sender_type, message, is_read, created_by) VALUES
('msg-01', 'session-01', 'user-customer-01', 'CUSTOMER', 'Shop ơi bó hồng đỏ còn hàng không ạ?', TRUE, 'user-customer-01'),
('msg-02', 'session-01', 'user-shop-01', 'SHOP', 'Dạ tiệm em còn nhiều hoa tươi mới nhập sáng nay ạ!', FALSE, 'user-shop-01');

-- 13. INSERT CUSTOM ORDER REQUESTS
INSERT INTO Custom_Order_Requests (id, customer_id, shop_id, budget, desired_components, description, status, created_by) VALUES
('req-01', 'user-customer-01', 'shop-01', 1000000.00, '{"main_flowers": ["Hồng Ân", "Lan Hồ Điệp"], "color_tone": "Pastel"}', 'Làm cho mình một lẵng hoa tặng kỷ niệm 10 năm ngày cưới.', 'NEGOTIATING', 'user-customer-01');

-- 14. INSERT PRODUCT REVIEWS
INSERT INTO Product_Reviews (id, product_id, user_id, order_id, rating, comment, shop_reply, created_by) VALUES
('rev-01', 'prod-01', 'user-customer-01', 'order-01', 5, 'Hoa rất tươi, giao hàng siêu nhanh. Sẽ ủng hộ shop tiếp!', 'Cảm ơn bạn đã tin tưởng FPTU Smart Floral ạ!', 'user-customer-01');

-- 15. INSERT BLOGS & COMMENTS
INSERT INTO Blogs (id, shop_id, title, content, thumbnail_url, status, created_by) VALUES
('blog-01', 'shop-01', 'Cách Giữ Hoa Hồng Tươi Lâu', '<p>Bí quyết giữ hoa hồng tươi lâu đến 7 ngày...</p>', 'https://res.cloudinary.com/[your-cloud]/image/upload/[blog-thumb-id].jpg', 'PUBLISHED', 'user-shop-01');

-- 15.1 INSERT BLOG IMAGES (Từ Cloudinary)
INSERT INTO Blog_Images (id, blog_id, image_url, display_order, created_by) VALUES
('blog-img-01', 'blog-01', 'https://res.cloudinary.com/[your-cloud]/image/upload/[blog-img-id-1].jpg', 1, 'user-shop-01');

INSERT INTO Blog_Comments (id, blog_id, user_id, parent_comment_id, content, created_by) VALUES
('cmt-01', 'blog-01', 'user-customer-01', NULL, 'Bài viết rất hữu ích, cảm ơn shop!', 'user-customer-01'),
('cmt-02', 'blog-01', 'user-shop-01', 'cmt-01', 'Dạ shop cảm ơn bạn ạ.', 'user-shop-01'); -- Reply comment

-- 16. INSERT FAVORITES & OCCASIONS
INSERT INTO Favorite_Products (user_id, product_id, created_by) VALUES
('user-customer-01', 'prod-01', 'user-customer-01');

INSERT INTO Customer_Occasions (id, customer_id, title, occasion_date, reminder_sent, created_by) VALUES
('60b10412-9153-48a7-857d-2faeb68a08c5', 'dcca63c5-cff9-4dc7-b27a-6a80d5cee5b5', 'Kỷ niệm ngày cưới', '2026-10-15', FALSE, 'dcca63c5-cff9-4dc7-b27a-6a80d5cee5b5');

-- 17. INSERT AUTH & SHOP/STAFF ONBOARDING DEMO DATA
-- (Không có mock data cho Email_Verification_Tokens vì bảng này chưa được ứng dụng sử dụng.)
INSERT INTO Otp_Challenges (id, user_id, purpose, code_hash, issued_at, expires_at, window_start, failed_attempts, send_count) VALUES
('a55527ce-6330-4570-ae62-52af795c4b70', 'dcca63c5-cff9-4dc7-b27a-6a80d5cee5b5', 'RESET_PASSWORD', '$2a$12$z2frH4jxzHPjasvwidmC4.GobzOtpx/Q1fhJyXokmGjsDypKXUPf2', '2026-09-27 09:00:00', '2026-09-27 09:05:00', '2026-09-27 09:00:00', 0, 1);

INSERT INTO Pending_Registrations (email, full_name, password_hash, otp_hash, shop_name, shop_description, issued_at, expires_at, window_start, failed_attempts, send_count) VALUES
('hoaxinh.owner@gmail.com', 'Đỗ Thị Hoa', '$2a$12$z2frH4jxzHPjasvwidmC4.GobzOtpx/Q1fhJyXokmGjsDypKXUPf2', '$2a$12$z2frH4jxzHPjasvwidmC4.GobzOtpx/Q1fhJyXokmGjsDypKXUPf2', 'Hoa Xinh Corner', 'Tiệm hoa nhỏ chuyên hoa cưới và hoa sự kiện.', '2026-09-27 08:00:00', '2026-09-27 08:05:00', '2026-09-27 08:00:00', 0, 1);

INSERT INTO Auth_Sessions (id, user_id, refresh_hash, expires_at, revoked) VALUES
('cf15b58e-86a5-4e1c-b5f4-fb49eef8431e', 'dcca63c5-cff9-4dc7-b27a-6a80d5cee5b5', '120c7fd6bccd463cc455a83865ca1e395c080dfbf1c3068fd16739cda0e2e381', '2026-10-04 09:00:00', FALSE);

INSERT INTO Shop_Staff (id, shop_id, user_id, active) VALUES
('9b016711-02fb-4325-a59d-9e0222483be9', '47bf0de6-8c7a-4a7e-9e45-f69f723dafd7', '8da640f4-6292-4bc5-8ff3-57806c7e0d0d', TRUE);

INSERT INTO Staff_Applications (id, shop_id, user_id, fullName, phone, introduction, status, submittedAt) VALUES
('3f64c11d-979b-4739-9075-8cf2ae40aadc', '47bf0de6-8c7a-4a7e-9e45-f69f723dafd7', '8da640f4-6292-4bc5-8ff3-57806c7e0d0d', 'Phạm Văn D', '0933445566', 'Em có kinh nghiệm 1 năm bán hoa, mong được hỗ trợ shop.', 'APPROVED', '2026-09-20 10:00:00');

INSERT INTO Staff_Invitations (id, shop_id, email, token_hash, expires_at, issued_at, window_start, send_count, status) VALUES
('bcca8057-01f7-4c72-b62d-b74e7003af4d', '47bf0de6-8c7a-4a7e-9e45-f69f723dafd7', 'staff2@gmail.com', 'fdb7ea0d1c4c9c541c51ea56199e1784310feae640c5ed70f465ddb1c113c336', '2026-10-04 09:00:00', '2026-09-27 09:00:00', '2026-09-27 09:00:00', 1, 'PENDING');

INSERT INTO Manager_Applications (id, user_id, full_name, phone, shop_name, description, address_line, city, district, ward, status, submitted_at) VALUES
('57fa5524-687a-4464-a8a0-846690b9d696', 'dcca63c5-cff9-4dc7-b27a-6a80d5cee5b5', 'Trần Thị B', '0987654321', 'B Flower House', 'Xin mở shop hoa online phục vụ khu vực Cầu Giấy.', 'Số 10, Ngõ 20, Đường Cầu Giấy', 'Hà Nội', 'Cầu Giấy', 'Dịch Vọng', 'PENDING', '2026-09-25 14:00:00');
USE flower_shop_db;


UPDATE Users SET full_name = CONVERT(BINARY(CONVERT(full_name USING latin1)) USING utf8mb4)
 WHERE full_name IS NOT NULL
   AND CONVERT(BINARY(CONVERT(full_name USING latin1)) USING utf8mb4) IS NOT NULL
   AND CONVERT(BINARY(CONVERT(full_name USING latin1)) USING utf8mb4) <> full_name
   AND (CHAR_LENGTH(CONVERT(full_name USING latin1)) - CHAR_LENGTH(REPLACE(CONVERT(full_name USING latin1), '?', ''))) = (CHAR_LENGTH(full_name) - CHAR_LENGTH(REPLACE(full_name, '?', '')));
UPDATE Shops SET name = CONVERT(BINARY(CONVERT(name USING latin1)) USING utf8mb4)
 WHERE name IS NOT NULL
   AND CONVERT(BINARY(CONVERT(name USING latin1)) USING utf8mb4) IS NOT NULL
   AND CONVERT(BINARY(CONVERT(name USING latin1)) USING utf8mb4) <> name
   AND (CHAR_LENGTH(CONVERT(name USING latin1)) - CHAR_LENGTH(REPLACE(CONVERT(name USING latin1), '?', ''))) = (CHAR_LENGTH(name) - CHAR_LENGTH(REPLACE(name, '?', '')));
UPDATE Shops SET description = CONVERT(BINARY(CONVERT(description USING latin1)) USING utf8mb4)
 WHERE description IS NOT NULL
   AND CONVERT(BINARY(CONVERT(description USING latin1)) USING utf8mb4) IS NOT NULL
   AND CONVERT(BINARY(CONVERT(description USING latin1)) USING utf8mb4) <> description
   AND (CHAR_LENGTH(CONVERT(description USING latin1)) - CHAR_LENGTH(REPLACE(CONVERT(description USING latin1), '?', ''))) = (CHAR_LENGTH(description) - CHAR_LENGTH(REPLACE(description, '?', '')));
UPDATE Addresses SET address_line = CONVERT(BINARY(CONVERT(address_line USING latin1)) USING utf8mb4)
 WHERE address_line IS NOT NULL
   AND CONVERT(BINARY(CONVERT(address_line USING latin1)) USING utf8mb4) IS NOT NULL
   AND CONVERT(BINARY(CONVERT(address_line USING latin1)) USING utf8mb4) <> address_line
   AND (CHAR_LENGTH(CONVERT(address_line USING latin1)) - CHAR_LENGTH(REPLACE(CONVERT(address_line USING latin1), '?', ''))) = (CHAR_LENGTH(address_line) - CHAR_LENGTH(REPLACE(address_line, '?', '')));
UPDATE Addresses SET city = CONVERT(BINARY(CONVERT(city USING latin1)) USING utf8mb4)
 WHERE city IS NOT NULL
   AND CONVERT(BINARY(CONVERT(city USING latin1)) USING utf8mb4) IS NOT NULL
   AND CONVERT(BINARY(CONVERT(city USING latin1)) USING utf8mb4) <> city
   AND (CHAR_LENGTH(CONVERT(city USING latin1)) - CHAR_LENGTH(REPLACE(CONVERT(city USING latin1), '?', ''))) = (CHAR_LENGTH(city) - CHAR_LENGTH(REPLACE(city, '?', '')));
UPDATE Addresses SET district = CONVERT(BINARY(CONVERT(district USING latin1)) USING utf8mb4)
 WHERE district IS NOT NULL
   AND CONVERT(BINARY(CONVERT(district USING latin1)) USING utf8mb4) IS NOT NULL
   AND CONVERT(BINARY(CONVERT(district USING latin1)) USING utf8mb4) <> district
   AND (CHAR_LENGTH(CONVERT(district USING latin1)) - CHAR_LENGTH(REPLACE(CONVERT(district USING latin1), '?', ''))) = (CHAR_LENGTH(district) - CHAR_LENGTH(REPLACE(district, '?', '')));
UPDATE Addresses SET ward = CONVERT(BINARY(CONVERT(ward USING latin1)) USING utf8mb4)
 WHERE ward IS NOT NULL
   AND CONVERT(BINARY(CONVERT(ward USING latin1)) USING utf8mb4) IS NOT NULL
   AND CONVERT(BINARY(CONVERT(ward USING latin1)) USING utf8mb4) <> ward
   AND (CHAR_LENGTH(CONVERT(ward USING latin1)) - CHAR_LENGTH(REPLACE(CONVERT(ward USING latin1), '?', ''))) = (CHAR_LENGTH(ward) - CHAR_LENGTH(REPLACE(ward, '?', '')));
UPDATE Categories SET name = CONVERT(BINARY(CONVERT(name USING latin1)) USING utf8mb4)
 WHERE name IS NOT NULL
   AND CONVERT(BINARY(CONVERT(name USING latin1)) USING utf8mb4) IS NOT NULL
   AND CONVERT(BINARY(CONVERT(name USING latin1)) USING utf8mb4) <> name
   AND (CHAR_LENGTH(CONVERT(name USING latin1)) - CHAR_LENGTH(REPLACE(CONVERT(name USING latin1), '?', ''))) = (CHAR_LENGTH(name) - CHAR_LENGTH(REPLACE(name, '?', '')));
UPDATE Categories SET description = CONVERT(BINARY(CONVERT(description USING latin1)) USING utf8mb4)
 WHERE description IS NOT NULL
   AND CONVERT(BINARY(CONVERT(description USING latin1)) USING utf8mb4) IS NOT NULL
   AND CONVERT(BINARY(CONVERT(description USING latin1)) USING utf8mb4) <> description
   AND (CHAR_LENGTH(CONVERT(description USING latin1)) - CHAR_LENGTH(REPLACE(CONVERT(description USING latin1), '?', ''))) = (CHAR_LENGTH(description) - CHAR_LENGTH(REPLACE(description, '?', '')));
UPDATE Products SET name = CONVERT(BINARY(CONVERT(name USING latin1)) USING utf8mb4)
 WHERE name IS NOT NULL
   AND CONVERT(BINARY(CONVERT(name USING latin1)) USING utf8mb4) IS NOT NULL
   AND CONVERT(BINARY(CONVERT(name USING latin1)) USING utf8mb4) <> name
   AND (CHAR_LENGTH(CONVERT(name USING latin1)) - CHAR_LENGTH(REPLACE(CONVERT(name USING latin1), '?', ''))) = (CHAR_LENGTH(name) - CHAR_LENGTH(REPLACE(name, '?', '')));
UPDATE Products SET description = CONVERT(BINARY(CONVERT(description USING latin1)) USING utf8mb4)
 WHERE description IS NOT NULL
   AND CONVERT(BINARY(CONVERT(description USING latin1)) USING utf8mb4) IS NOT NULL
   AND CONVERT(BINARY(CONVERT(description USING latin1)) USING utf8mb4) <> description
   AND (CHAR_LENGTH(CONVERT(description USING latin1)) - CHAR_LENGTH(REPLACE(CONVERT(description USING latin1), '?', ''))) = (CHAR_LENGTH(description) - CHAR_LENGTH(REPLACE(description, '?', '')));
UPDATE Product_Videos SET title = CONVERT(BINARY(CONVERT(title USING latin1)) USING utf8mb4)
 WHERE title IS NOT NULL
   AND CONVERT(BINARY(CONVERT(title USING latin1)) USING utf8mb4) IS NOT NULL
   AND CONVERT(BINARY(CONVERT(title USING latin1)) USING utf8mb4) <> title
   AND (CHAR_LENGTH(CONVERT(title USING latin1)) - CHAR_LENGTH(REPLACE(CONVERT(title USING latin1), '?', ''))) = (CHAR_LENGTH(title) - CHAR_LENGTH(REPLACE(title, '?', '')));
UPDATE Product_Videos SET description = CONVERT(BINARY(CONVERT(description USING latin1)) USING utf8mb4)
 WHERE description IS NOT NULL
   AND CONVERT(BINARY(CONVERT(description USING latin1)) USING utf8mb4) IS NOT NULL
   AND CONVERT(BINARY(CONVERT(description USING latin1)) USING utf8mb4) <> description
   AND (CHAR_LENGTH(CONVERT(description USING latin1)) - CHAR_LENGTH(REPLACE(CONVERT(description USING latin1), '?', ''))) = (CHAR_LENGTH(description) - CHAR_LENGTH(REPLACE(description, '?', '')));
UPDATE Deliveries SET tracking_notes = CONVERT(BINARY(CONVERT(tracking_notes USING latin1)) USING utf8mb4)
 WHERE tracking_notes IS NOT NULL
   AND CONVERT(BINARY(CONVERT(tracking_notes USING latin1)) USING utf8mb4) IS NOT NULL
   AND CONVERT(BINARY(CONVERT(tracking_notes USING latin1)) USING utf8mb4) <> tracking_notes
   AND (CHAR_LENGTH(CONVERT(tracking_notes USING latin1)) - CHAR_LENGTH(REPLACE(CONVERT(tracking_notes USING latin1), '?', ''))) = (CHAR_LENGTH(tracking_notes) - CHAR_LENGTH(REPLACE(tracking_notes, '?', '')));
UPDATE Chat_Messages SET message = CONVERT(BINARY(CONVERT(message USING latin1)) USING utf8mb4)
 WHERE message IS NOT NULL
   AND CONVERT(BINARY(CONVERT(message USING latin1)) USING utf8mb4) IS NOT NULL
   AND CONVERT(BINARY(CONVERT(message USING latin1)) USING utf8mb4) <> message
   AND (CHAR_LENGTH(CONVERT(message USING latin1)) - CHAR_LENGTH(REPLACE(CONVERT(message USING latin1), '?', ''))) = (CHAR_LENGTH(message) - CHAR_LENGTH(REPLACE(message, '?', '')));
UPDATE Custom_Order_Requests SET description = CONVERT(BINARY(CONVERT(description USING latin1)) USING utf8mb4)
 WHERE description IS NOT NULL
   AND CONVERT(BINARY(CONVERT(description USING latin1)) USING utf8mb4) IS NOT NULL
   AND CONVERT(BINARY(CONVERT(description USING latin1)) USING utf8mb4) <> description
   AND (CHAR_LENGTH(CONVERT(description USING latin1)) - CHAR_LENGTH(REPLACE(CONVERT(description USING latin1), '?', ''))) = (CHAR_LENGTH(description) - CHAR_LENGTH(REPLACE(description, '?', '')));
UPDATE Product_Reviews SET comment = CONVERT(BINARY(CONVERT(comment USING latin1)) USING utf8mb4)
 WHERE comment IS NOT NULL
   AND CONVERT(BINARY(CONVERT(comment USING latin1)) USING utf8mb4) IS NOT NULL
   AND CONVERT(BINARY(CONVERT(comment USING latin1)) USING utf8mb4) <> comment
   AND (CHAR_LENGTH(CONVERT(comment USING latin1)) - CHAR_LENGTH(REPLACE(CONVERT(comment USING latin1), '?', ''))) = (CHAR_LENGTH(comment) - CHAR_LENGTH(REPLACE(comment, '?', '')));
UPDATE Product_Reviews SET shop_reply = CONVERT(BINARY(CONVERT(shop_reply USING latin1)) USING utf8mb4)
 WHERE shop_reply IS NOT NULL
   AND CONVERT(BINARY(CONVERT(shop_reply USING latin1)) USING utf8mb4) IS NOT NULL
   AND CONVERT(BINARY(CONVERT(shop_reply USING latin1)) USING utf8mb4) <> shop_reply
   AND (CHAR_LENGTH(CONVERT(shop_reply USING latin1)) - CHAR_LENGTH(REPLACE(CONVERT(shop_reply USING latin1), '?', ''))) = (CHAR_LENGTH(shop_reply) - CHAR_LENGTH(REPLACE(shop_reply, '?', '')));
UPDATE Blogs SET title = CONVERT(BINARY(CONVERT(title USING latin1)) USING utf8mb4)
 WHERE title IS NOT NULL
   AND CONVERT(BINARY(CONVERT(title USING latin1)) USING utf8mb4) IS NOT NULL
   AND CONVERT(BINARY(CONVERT(title USING latin1)) USING utf8mb4) <> title
   AND (CHAR_LENGTH(CONVERT(title USING latin1)) - CHAR_LENGTH(REPLACE(CONVERT(title USING latin1), '?', ''))) = (CHAR_LENGTH(title) - CHAR_LENGTH(REPLACE(title, '?', '')));
UPDATE Blogs SET content = CONVERT(BINARY(CONVERT(content USING latin1)) USING utf8mb4)
 WHERE content IS NOT NULL
   AND CONVERT(BINARY(CONVERT(content USING latin1)) USING utf8mb4) IS NOT NULL
   AND CONVERT(BINARY(CONVERT(content USING latin1)) USING utf8mb4) <> content
   AND (CHAR_LENGTH(CONVERT(content USING latin1)) - CHAR_LENGTH(REPLACE(CONVERT(content USING latin1), '?', ''))) = (CHAR_LENGTH(content) - CHAR_LENGTH(REPLACE(content, '?', '')));
UPDATE Blog_Comments SET content = CONVERT(BINARY(CONVERT(content USING latin1)) USING utf8mb4)
 WHERE content IS NOT NULL
   AND CONVERT(BINARY(CONVERT(content USING latin1)) USING utf8mb4) IS NOT NULL
   AND CONVERT(BINARY(CONVERT(content USING latin1)) USING utf8mb4) <> content
   AND (CHAR_LENGTH(CONVERT(content USING latin1)) - CHAR_LENGTH(REPLACE(CONVERT(content USING latin1), '?', ''))) = (CHAR_LENGTH(content) - CHAR_LENGTH(REPLACE(content, '?', '')));
UPDATE Customer_Occasions SET title = CONVERT(BINARY(CONVERT(title USING latin1)) USING utf8mb4)
 WHERE title IS NOT NULL
   AND CONVERT(BINARY(CONVERT(title USING latin1)) USING utf8mb4) IS NOT NULL
   AND CONVERT(BINARY(CONVERT(title USING latin1)) USING utf8mb4) <> title
   AND (CHAR_LENGTH(CONVERT(title USING latin1)) - CHAR_LENGTH(REPLACE(CONVERT(title USING latin1), '?', ''))) = (CHAR_LENGTH(title) - CHAR_LENGTH(REPLACE(title, '?', '')));
UPDATE Refunds SET reason = CONVERT(BINARY(CONVERT(reason USING latin1)) USING utf8mb4)
 WHERE reason IS NOT NULL
   AND CONVERT(BINARY(CONVERT(reason USING latin1)) USING utf8mb4) IS NOT NULL
   AND CONVERT(BINARY(CONVERT(reason USING latin1)) USING utf8mb4) <> reason
   AND (CHAR_LENGTH(CONVERT(reason USING latin1)) - CHAR_LENGTH(REPLACE(CONVERT(reason USING latin1), '?', ''))) = (CHAR_LENGTH(reason) - CHAR_LENGTH(REPLACE(reason, '?', '')));

-- Cá»™t JSON
UPDATE Products SET components = CONVERT(BINARY(CONVERT(CAST(components AS CHAR) USING latin1)) USING utf8mb4)
 WHERE components IS NOT NULL
   AND CONVERT(BINARY(CONVERT(CAST(components AS CHAR) USING latin1)) USING utf8mb4) IS NOT NULL
   AND CONVERT(BINARY(CONVERT(CAST(components AS CHAR) USING latin1)) USING utf8mb4) <> CAST(components AS CHAR)
   AND (CHAR_LENGTH(CONVERT(CAST(components AS CHAR) USING latin1)) - CHAR_LENGTH(REPLACE(CONVERT(CAST(components AS CHAR) USING latin1), '?', ''))) = (CHAR_LENGTH(CAST(components AS CHAR)) - CHAR_LENGTH(REPLACE(CAST(components AS CHAR), '?', '')));
UPDATE Custom_Order_Requests SET desired_components = CONVERT(BINARY(CONVERT(CAST(desired_components AS CHAR) USING latin1)) USING utf8mb4)
 WHERE desired_components IS NOT NULL
   AND CONVERT(BINARY(CONVERT(CAST(desired_components AS CHAR) USING latin1)) USING utf8mb4) IS NOT NULL
   AND CONVERT(BINARY(CONVERT(CAST(desired_components AS CHAR) USING latin1)) USING utf8mb4) <> CAST(desired_components AS CHAR)
   AND (CHAR_LENGTH(CONVERT(CAST(desired_components AS CHAR) USING latin1)) - CHAR_LENGTH(REPLACE(CONVERT(CAST(desired_components AS CHAR) USING latin1), '?', ''))) = (CHAR_LENGTH(CAST(desired_components AS CHAR)) - CHAR_LENGTH(REPLACE(CAST(desired_components AS CHAR), '?', '')));
UPDATE Order_Details SET custom_materials = CONVERT(BINARY(CONVERT(CAST(custom_materials AS CHAR) USING latin1)) USING utf8mb4)
 WHERE custom_materials IS NOT NULL
   AND CONVERT(BINARY(CONVERT(CAST(custom_materials AS CHAR) USING latin1)) USING utf8mb4) IS NOT NULL
   AND CONVERT(BINARY(CONVERT(CAST(custom_materials AS CHAR) USING latin1)) USING utf8mb4) <> CAST(custom_materials AS CHAR)
   AND (CHAR_LENGTH(CONVERT(CAST(custom_materials AS CHAR) USING latin1)) - CHAR_LENGTH(REPLACE(CONVERT(CAST(custom_materials AS CHAR) USING latin1), '?', ''))) = (CHAR_LENGTH(CAST(custom_materials AS CHAR)) - CHAR_LENGTH(REPLACE(CAST(custom_materials AS CHAR), '?', '')));
UPDATE Chat_Messages SET media = CONVERT(BINARY(CONVERT(CAST(media AS CHAR) USING latin1)) USING utf8mb4)
 WHERE media IS NOT NULL
   AND CONVERT(BINARY(CONVERT(CAST(media AS CHAR) USING latin1)) USING utf8mb4) IS NOT NULL
   AND CONVERT(BINARY(CONVERT(CAST(media AS CHAR) USING latin1)) USING utf8mb4) <> CAST(media AS CHAR)
   AND (CHAR_LENGTH(CONVERT(CAST(media AS CHAR) USING latin1)) - CHAR_LENGTH(REPLACE(CONVERT(CAST(media AS CHAR) USING latin1), '?', ''))) = (CHAR_LENGTH(CAST(media AS CHAR)) - CHAR_LENGTH(REPLACE(CAST(media AS CHAR), '?', '')));
UPDATE Product_Reviews SET media = CONVERT(BINARY(CONVERT(CAST(media AS CHAR) USING latin1)) USING utf8mb4)
 WHERE media IS NOT NULL
   AND CONVERT(BINARY(CONVERT(CAST(media AS CHAR) USING latin1)) USING utf8mb4) IS NOT NULL
   AND CONVERT(BINARY(CONVERT(CAST(media AS CHAR) USING latin1)) USING utf8mb4) <> CAST(media AS CHAR)
   AND (CHAR_LENGTH(CONVERT(CAST(media AS CHAR) USING latin1)) - CHAR_LENGTH(REPLACE(CONVERT(CAST(media AS CHAR) USING latin1), '?', ''))) = (CHAR_LENGTH(CAST(media AS CHAR)) - CHAR_LENGTH(REPLACE(CAST(media AS CHAR), '?', '')));
UPDATE Payments SET gateway_response = CONVERT(BINARY(CONVERT(CAST(gateway_response AS CHAR) USING latin1)) USING utf8mb4)
 WHERE gateway_response IS NOT NULL
   AND CONVERT(BINARY(CONVERT(CAST(gateway_response AS CHAR) USING latin1)) USING utf8mb4) IS NOT NULL
   AND CONVERT(BINARY(CONVERT(CAST(gateway_response AS CHAR) USING latin1)) USING utf8mb4) <> CAST(gateway_response AS CHAR)
   AND (CHAR_LENGTH(CONVERT(CAST(gateway_response AS CHAR) USING latin1)) - CHAR_LENGTH(REPLACE(CONVERT(CAST(gateway_response AS CHAR) USING latin1), '?', ''))) = (CHAR_LENGTH(CAST(gateway_response AS CHAR)) - CHAR_LENGTH(REPLACE(CAST(gateway_response AS CHAR), '?', '')));
UPDATE Refunds SET gateway_response = CONVERT(BINARY(CONVERT(CAST(gateway_response AS CHAR) USING latin1)) USING utf8mb4)
 WHERE gateway_response IS NOT NULL
   AND CONVERT(BINARY(CONVERT(CAST(gateway_response AS CHAR) USING latin1)) USING utf8mb4) IS NOT NULL
   AND CONVERT(BINARY(CONVERT(CAST(gateway_response AS CHAR) USING latin1)) USING utf8mb4) <> CAST(gateway_response AS CHAR)
   AND (CHAR_LENGTH(CONVERT(CAST(gateway_response AS CHAR) USING latin1)) - CHAR_LENGTH(REPLACE(CONVERT(CAST(gateway_response AS CHAR) USING latin1), '?', ''))) = (CHAR_LENGTH(CAST(gateway_response AS CHAR)) - CHAR_LENGTH(REPLACE(CAST(gateway_response AS CHAR), '?', '')));

-- Dá»n báº£ng thÄƒm dÃ² náº¿u cÃ²n sÃ³t tá»« lÃºc cháº©n Ä‘oÃ¡n.
DROP TABLE IF EXISTS _charset_probe;
