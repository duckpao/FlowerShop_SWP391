CREATE DATABASE IF NOT EXISTS flower_shop_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE flower_shop_db;
SET NAMES utf8mb4;

-- ==========================================
-- M01: AUTHENTICATION & USER MANAGEMENT
-- ==========================================
CREATE TABLE Users (
    avatar_url VARCHAR(1024),
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255),
    google_id VARCHAR(255) UNIQUE,
    full_name VARCHAR(100),
    phone VARCHAR(20),
    role ENUM('ADMIN', 'SHOP', 'CUSTOMER', 'DELIVERY', 'SHOP_STAFF') NOT NULL, -- DELIVERY: giá trị cũ, giữ lại cho chức năng giao hàng sau này
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
    FOREIGN KEY (owner_id) REFERENCES Users(id),
    UNIQUE KEY uq_shop_owner (owner_id) -- mỗi chủ shop chỉ có một shop
);

CREATE TABLE Addresses (
    recipient_name VARCHAR(100),
    recipient_phone VARCHAR(10),
    is_pickup BOOLEAN DEFAULT FALSE,
    is_return BOOLEAN DEFAULT FALSE,
    address_type VARCHAR(10) DEFAULT 'HOME',
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36), -- NULL nếu là địa chỉ của Shop
    shop_id VARCHAR(36), -- NULL nếu là địa chỉ của User
    address_line VARCHAR(255) NOT NULL,
    ward VARCHAR(100),
    district VARCHAR(100),
    city VARCHAR(100),
    ghn_district_id INT, -- mã quận/huyện và mã phường/xã của GHN, dùng để tính phí và tạo vận đơn
    ghn_ward_code VARCHAR(20),
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
    used_at TIMESTAMP NULL, -- thời điểm mã đã được dùng
    window_start TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    failed_attempts INT NOT NULL DEFAULT 0,
    send_count INT NOT NULL DEFAULT 1,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by VARCHAR(36),
    FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE,
    UNIQUE KEY uq_otp_user_purpose (user_id, purpose) -- mỗi user chỉ một mã đang hiệu lực cho mỗi mục đích
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
    user_id VARCHAR(36) NOT NULL UNIQUE, -- mỗi user chỉ một đơn xin mở shop (gửi lại thì ghi đè đơn cũ)
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
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    introduction VARCHAR(1000) NOT NULL,
    status VARCHAR(16) NOT NULL, -- PENDING, APPROVED, REJECTED
    submitted_at DATETIME(6) NOT NULL,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by VARCHAR(36),
    FOREIGN KEY (shop_id) REFERENCES Shops(id),
    FOREIGN KEY (user_id) REFERENCES Users(id),
    UNIQUE KEY uq_staff_application (shop_id, user_id)
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
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING', -- PENDING, ACCEPTED, EXPIRED, CANCELLED (service ghi CANCELLED nên không dùng ENUM)
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by VARCHAR(36),
    FOREIGN KEY (shop_id) REFERENCES Shops(id),
    UNIQUE KEY uq_staff_invite (shop_id, email)
);

-- ==========================================
-- M02: PRODUCT & CATALOG
-- ==========================================
CREATE TABLE Categories (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    group_name VARCHAR(50), -- nhóm trên menu: Chủ đề, Hoa tươi, Kiểu dáng
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
    deleted BOOLEAN NOT NULL DEFAULT FALSE, -- xóa mềm: giữ lại để đơn và đánh giá cũ còn tham chiếu
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
    deleted BOOLEAN NOT NULL DEFAULT FALSE, -- xóa mềm: đơn cũ vẫn tham chiếu được mã
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
    checkout_group VARCHAR(36), -- chung cho các đơn (mỗi shop một đơn) tạo trong cùng một lần thanh toán giỏ; NULL với đơn lẻ
    custom_request_id VARCHAR(36), -- đơn đặt cọc (CUSTOM) sinh từ Custom_Order_Requests; NULL với đơn thường
    status ENUM('PENDING', 'AWAITING_DEPOSIT', 'DEPOSIT_PAID', 'PROCESSING', 'DELIVERING', 'COMPLETED', 'CANCELLED', 'REFUNDED') DEFAULT 'PENDING',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by VARCHAR(36),
    FOREIGN KEY (customer_id) REFERENCES Users(id),
    FOREIGN KEY (shop_id) REFERENCES Shops(id),
    FOREIGN KEY (delivery_address_id) REFERENCES Addresses(id),
    FOREIGN KEY (coupon_id) REFERENCES Coupons(id),
    INDEX idx_orders_checkout_group (checkout_group),
    INDEX idx_orders_custom_request (custom_request_id)
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
    status ENUM('PENDING', 'PICKED_UP', 'ON_THE_WAY', 'DELIVERED', 'FAILED', 'RETURNING', 'RETURNED') DEFAULT 'PENDING',
    ghn_status VARCHAR(40),
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
    last_message_at DATETIME(6) NULL,
    last_message_preview VARCHAR(255) NULL,
    customer_unread_count INT NOT NULL DEFAULT 0, -- tin shop gửi mà khách chưa đọc
    shop_unread_count INT NOT NULL DEFAULT 0, -- tin khách gửi mà shop chưa đọc
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by VARCHAR(36),
    FOREIGN KEY (customer_id) REFERENCES Users(id),
    FOREIGN KEY (shop_id) REFERENCES Shops(id),
    UNIQUE KEY uq_chat_customer_shop (customer_id, shop_id),
    INDEX idx_chat_shop_last (shop_id, last_message_at)
);

CREATE TABLE Chat_Messages (
    id VARCHAR(36) PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL,
    sender_id VARCHAR(36) NOT NULL,
    sender_type ENUM('CUSTOMER', 'SHOP') NOT NULL,
    message TEXT,
    media JSON, -- Hình ảnh, video, voucher đính kèm
    context_type ENUM('NONE', 'CUSTOM_ORDER', 'PRODUCT') NOT NULL DEFAULT 'NONE', -- thẻ đính kèm: đơn custom hoặc sản phẩm
    context_id VARCHAR(36) NULL,
    context_snapshot JSON NULL, -- tên, giá, ảnh, trạng thái tại lúc gửi
    is_read BOOLEAN DEFAULT FALSE,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by VARCHAR(36),
    FOREIGN KEY (session_id) REFERENCES Chat_Sessions(id),
    FOREIGN KEY (sender_id) REFERENCES Users(id),
    INDEX idx_chat_messages_session (session_id, created_date)
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
    status ENUM('REQUESTED', 'NEGOTIATING', 'QUOTED', 'ACCEPTED', 'REJECTED') DEFAULT 'REQUESTED',
    quoted_price DECIMAL(12, 2), -- báo giá hoa của shop, chưa gồm phí giao hàng
    deposit_percent INT, -- % cọc shop yêu cầu (10 đến 100)
    quote_valid_until DATETIME,
    quoted_date DATETIME,
    reject_reason VARCHAR(2000),
    rejected_by VARCHAR(20), -- SHOP hoặc CUSTOMER
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
    gateway_transaction_no VARCHAR(100), -- mã giao dịch từ cổng thanh toán
    invoice_number VARCHAR(100) UNIQUE, -- Mã hóa đơn duy nhất dùng cho SePay/IPN
    group_invoice VARCHAR(100), -- mã gửi SePay khi thanh toán gộp nhiều đơn; NULL với thanh toán đơn lẻ
    gateway_response JSON,
    status ENUM('PENDING', 'SUCCESS', 'FAILED') DEFAULT 'PENDING',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by VARCHAR(36),
    FOREIGN KEY (order_id) REFERENCES Orders(id),
    INDEX idx_payments_group_invoice (group_invoice)
);

CREATE TABLE Refunds (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    reason TEXT NOT NULL,
    description TEXT NULL,
    evidence_images TEXT NULL,
    shop_response TEXT NULL,
    gateway_refund_id VARCHAR(100),
    gateway_response JSON,
    status ENUM('REQUESTED', 'APPROVED', 'PROCESSED', 'REJECTED', 'CANCELLED') DEFAULT 'REQUESTED',
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
    images JSON, -- danh sách ảnh của bài viết (entity Blog.images)
    status ENUM('DRAFT', 'PUBLISHED', 'HIDDEN') DEFAULT 'PUBLISHED',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by VARCHAR(36),
    FOREIGN KEY (shop_id) REFERENCES Shops(id),
    INDEX idx_blogs_public_feed (status, created_date),
    INDEX idx_blogs_shop_feed (shop_id, created_date)
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
    reminder_sent BOOLEAN DEFAULT FALSE, -- dịp một lần: đã nhắc xong
    recurring_yearly BOOLEAN NOT NULL DEFAULT FALSE, -- lặp lại hằng năm (sinh nhật, kỷ niệm)
    notify_days_before INT NOT NULL DEFAULT 7, -- nhắc trước ngày dịp bao nhiêu ngày (mặc định 1 tuần)
    notify_time TIME NOT NULL DEFAULT '08:00:00', -- giờ gửi nhắc
    last_reminded_for DATE NULL, -- ngày diễn ra của lần dịp đã nhắc gần nhất, tránh nhắc lặp
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by VARCHAR(36),
    FOREIGN KEY (customer_id) REFERENCES Users(id),
    INDEX idx_occasions_customer (customer_id)
);

-- Thông báo trong ứng dụng (chuông thông báo)
CREATE TABLE Notifications (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT,
    link VARCHAR(255),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_date DATETIME(6) NOT NULL,
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES Users(id),
    INDEX ix_notification_user (user_id, created_date)
);

-- Theo dõi shop: sản phẩm đã được công bố (để sửa sản phẩm thường không gửi lại thông báo)
CREATE TABLE Shop_Product_Publications (
    product_id VARCHAR(36) PRIMARY KEY,
    published_at DATETIME(6) NOT NULL
);

-- Hàng đợi email báo sản phẩm mới cho người theo dõi shop
CREATE TABLE Shop_Product_Mails (
    id VARCHAR(36) PRIMARY KEY,
    product_id VARCHAR(36) NOT NULL,
    shop_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    sent_at DATETIME(6),
    CONSTRAINT uk_shop_product_mail UNIQUE (product_id, user_id),
    INDEX ix_shop_product_mail_due (status, next_attempt_at, created_at)
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
-- Mô hình 3 cấp: tỉnh (city) > quận/huyện (district) > phường/xã (ward). ghn_district_id / ghn_ward_code để NULL vì chưa lấy
-- được mã từ API GHN; mở hồ sơ và chọn lại Tỉnh, Quận/Huyện, Phường/Xã để có mã thì mới tính được phí vận chuyển.
INSERT INTO Addresses (id, user_id, shop_id, address_line, ward, district, city, recipient_name, recipient_phone, address_type, is_pickup, is_default, created_by) VALUES
('addr-shop-01', NULL, 'shop-01', 'Khu Công Nghệ Cao Hòa Lạc', 'Thạch Hòa', 'Thạch Thất', 'Hà Nội', 'Nguyễn Đức Bảo', '0912345678', 'OFFICE', TRUE, TRUE, 'user-shop-01'),
('addr-customer-01', 'user-customer-01', NULL, 'Số 10, Ngõ 20, Đường Cầu Giấy', 'Dịch Vọng', 'Cầu Giấy', 'Hà Nội', 'Trần Thị B', '0987654321', 'HOME', FALSE, TRUE, 'user-customer-01');

-- 4. CATEGORIES
INSERT INTO Categories (id, name, description, status, created_by) VALUES
('cat-01', 'Hoa Tình Yêu', 'Các mẫu hoa lãng mạn dành cho cặp đôi', 'ACTIVE', 'user-admin-01'),
('cat-02', 'Hoa Khai Trương', 'Kệ hoa, lẵng hoa chúc mừng khai trương', 'ACTIVE', 'user-admin-01'),
-- Theo dịp
('cat-03', 'Hoa Sinh Nhật', 'Bó hoa, giỏ hoa chúc mừng sinh nhật', 'ACTIVE', 'user-admin-01'),
('cat-04', 'Hoa Chúc Mừng', 'Hoa chúc mừng thăng chức, thành công, sự kiện vui', 'ACTIVE', 'user-admin-01'),
('cat-05', 'Hoa Chia Buồn', 'Vòng hoa, lẵng hoa viếng và chia buồn', 'ACTIVE', 'user-admin-01'),
('cat-06', 'Hoa Chúc Sức Khỏe', 'Hoa thăm hỏi, chúc người thân mau khỏe', 'ACTIVE', 'user-admin-01'),
('cat-07', 'Hoa Cảm Ơn', 'Hoa gửi lời cảm ơn thầy cô, đối tác, người thân', 'ACTIVE', 'user-admin-01'),
('cat-08', 'Hoa Mừng Tốt Nghiệp', 'Hoa tặng lễ tốt nghiệp, bảo vệ luận văn', 'ACTIVE', 'user-admin-01'),
('cat-09', 'Hoa Cưới', 'Hoa cầm tay cô dâu, hoa trang trí tiệc cưới', 'ACTIVE', 'user-admin-01'),
('cat-10', 'Hoa Ngày 20/10', 'Hoa tặng Ngày Phụ nữ Việt Nam', 'ACTIVE', 'user-admin-01'),
('cat-11', 'Hoa Ngày 8/3', 'Hoa tặng Ngày Quốc tế Phụ nữ', 'ACTIVE', 'user-admin-01'),
('cat-12', 'Hoa Ngày Nhà Giáo', 'Hoa tri ân thầy cô ngày 20/11', 'ACTIVE', 'user-admin-01'),
('cat-13', 'Hoa Valentine', 'Hoa tặng người yêu ngày 14/2', 'ACTIVE', 'user-admin-01'),
('cat-14', 'Hoa Tết', 'Hoa trưng bàn, hoa chúc Tết đầu năm', 'ACTIVE', 'user-admin-01'),
('cat-15', 'Hoa Giáng Sinh', 'Hoa và cành trang trí mùa Noel', 'ACTIVE', 'user-admin-01'),
-- Theo loại hoa
('cat-16', 'Hoa Hồng', 'Hồng đỏ, hồng pastel, hồng Ecuador', 'ACTIVE', 'user-admin-01'),
('cat-17', 'Hoa Hướng Dương', 'Bó và giỏ hướng dương rực rỡ', 'ACTIVE', 'user-admin-01'),
('cat-18', 'Hoa Đồng Tiền', 'Đồng tiền nhiều màu, bền hoa', 'ACTIVE', 'user-admin-01'),
('cat-19', 'Hoa Ly', 'Ly thơm, ly Tycoon, ly Sorbonne', 'ACTIVE', 'user-admin-01'),
('cat-20', 'Hoa Cúc', 'Cúc họa mi, cúc mẫu đơn, cúc pingpong', 'ACTIVE', 'user-admin-01'),
('cat-21', 'Hoa Cẩm Chướng', 'Cẩm chướng đơn và chùm', 'ACTIVE', 'user-admin-01'),
('cat-22', 'Hoa Cát Tường', 'Cát tường tím, trắng, hồng', 'ACTIVE', 'user-admin-01'),
('cat-23', 'Baby Flower', 'Hoa baby trắng, baby nhuộm màu', 'ACTIVE', 'user-admin-01'),
('cat-24', 'Lan Hồ Điệp', 'Chậu lan hồ điệp để bàn, làm quà biếu', 'ACTIVE', 'user-admin-01'),
-- Theo kiểu dáng
('cat-25', 'Bó Hoa Tươi', 'Hoa bó gói giấy, cầm tay', 'ACTIVE', 'user-admin-01'),
('cat-26', 'Giỏ Hoa Tươi', 'Hoa cắm giỏ mây, giỏ gỗ', 'ACTIVE', 'user-admin-01'),
('cat-27', 'Hộp Hoa Tươi', 'Hoa cắm hộp tròn, hộp vuông, hộp mica', 'ACTIVE', 'user-admin-01'),
('cat-28', 'Bình Hoa Tươi', 'Hoa cắm sẵn trong bình thủy tinh, bình gốm', 'ACTIVE', 'user-admin-01'),
('cat-29', 'Lẵng Hoa', 'Lẵng hoa chúc mừng và chia buồn cỡ lớn', 'ACTIVE', 'user-admin-01'),
('cat-30', 'Kệ Hoa', 'Kệ hoa khai trương, hội nghị, sự kiện', 'ACTIVE', 'user-admin-01');

SET SQL_SAFE_UPDATES = 0;
UPDATE Categories SET group_name = 'Chủ đề' WHERE name IN ('Hoa Tình Yêu', 'Hoa Khai Trương', 'Hoa Sinh Nhật', 'Hoa Chúc Mừng', 'Hoa Chia Buồn', 'Hoa Chúc Sức Khỏe', 'Hoa Cảm Ơn', 'Hoa Mừng Tốt Nghiệp', 'Hoa Cưới', 'Hoa Ngày 20/10', 'Hoa Ngày 8/3', 'Hoa Ngày Nhà Giáo', 'Hoa Valentine', 'Hoa Tết', 'Hoa Giáng Sinh');
UPDATE Categories SET group_name = 'Hoa tươi' WHERE name IN ('Hoa Hồng', 'Hoa Hướng Dương', 'Hoa Đồng Tiền', 'Hoa Ly', 'Hoa Cúc', 'Hoa Cẩm Chướng', 'Hoa Cát Tường', 'Baby Flower', 'Lan Hồ Điệp');
UPDATE Categories SET group_name = 'Kiểu dáng' WHERE name IN ('Bó Hoa Tươi', 'Giỏ Hoa Tươi', 'Hộp Hoa Tươi', 'Bình Hoa Tươi', 'Lẵng Hoa', 'Kệ Hoa');
SET SQL_SAFE_UPDATES = 1;
-- 5. PRODUCTS
INSERT INTO Products (id, shop_id, category_id, name, description, components, shelf_life_days, price, stock, status, created_by) VALUES
('prod-01', 'shop-01', 'cat-01', 'Bó Hồng Đỏ Mix Baby', 'Bó hoa hồng đỏ Ecuador mix hoa baby trắng', '{"red_roses": 10, "white_baby_breath": 3, "wrapper": "Giấy Kraft"}', 4, 350000.00, 50, 'ACTIVE', 'user-shop-01'),
('prod-02', 'shop-01', 'cat-02', 'Lẵng Hướng Dương Ban Mai', 'Hoa hướng dương tặng khai trương, tốt nghiệp', '{"sunflowers": 5, "yellow_roses": 5}', 5, 650000.00, 20, 'ACTIVE', 'user-shop-01');

-- 5.0 Sản phẩm đang bán được ghi nhận là đã công bố, để sửa sản phẩm thường không gửi thông báo "sản phẩm mới"
INSERT IGNORE INTO Shop_Product_Publications (product_id, published_at)
SELECT id, COALESCE(created_date, NOW()) FROM Products WHERE status = 'ACTIVE';

-- 5.1 PRODUCT IMAGES

-- 5.2 PRODUCT VIDEOS

-- 6. COUPONS
INSERT INTO Coupons (id, shop_id, code, discount_type, discount_value, min_order_value, max_discount_value, start_date, end_date, usage_limit, used_count, created_by) VALUES
('coupon-01', 'shop-01', 'WELCOME10', 'PERCENTAGE', 10.00, 200000.00, 50000.00, '2023-01-01 00:00:00', '2026-12-31 23:59:59', 100, 1, 'user-shop-01');

-- 7. CART ITEMS
INSERT INTO Cart_Items (id, user_id, product_id, quantity, created_by) VALUES
('cart-01', 'user-customer-01', 'prod-02', 1, 'user-customer-01');

-- 8. ORDERS: 2 x 350000 = 700000, giảm 10% tối đa 50000 -> tổng 650000
INSERT INTO Orders (id, customer_id, shop_id, delivery_address_id, recipient_name, recipient_phone, coupon_id, order_type, sub_total, discount_amount, shipping_fee, total_amount, deposit_amount, status, created_by) VALUES
('order-01', 'user-customer-01', 'shop-01', 'addr-customer-01', 'Trần Thị B', '0987654321', NULL, 'STANDARD', 700000.00, 0.00, 0.00, 700000.00, 0.00, 'COMPLETED', 'user-customer-01');

-- 9. ORDER DETAILS
INSERT INTO Order_Details (id, order_id, product_id, price, quantity, created_by) VALUES
('ord-dtl-01', 'order-01', 'prod-01', 350000.00, 2, 'user-customer-01');

-- 10. PAYMENTS
INSERT INTO Payments (id, order_id, payment_type, payment_method, amount, gateway_transaction_no, invoice_number, gateway_response, status, created_by) VALUES
('pay-01', 'order-01', 'FULL', 'ONLINE', 700000.00, 'VNP123456789', 'INV-0001', '{"vnp_ResponseCode":"00", "vnp_BankCode":"NCB"}', 'SUCCESS', 'user-customer-01');

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
('blog-01', 'shop-01', 'Cách Giữ Hoa Hồng Tươi Lâu', '<p>Bí quyết giữ hoa hồng tươi lâu đến 7 ngày...</p>', NULL, 'PUBLISHED', 'user-shop-01');


INSERT INTO Blog_Comments (id, blog_id, user_id, parent_comment_id, content, created_by) VALUES
('cmt-01', 'blog-01', 'user-customer-01', NULL, 'Bài viết rất hữu ích, cảm ơn shop!', 'user-customer-01'),
('cmt-02', 'blog-01', 'user-shop-01', 'cmt-01', 'Dạ shop cảm ơn bạn ạ.', 'user-shop-01');

-- 16. FAVORITES & OCCASIONS
INSERT INTO Favorite_Products (user_id, product_id, created_by) VALUES
('user-customer-01', 'prod-01', 'user-customer-01');

INSERT INTO Customer_Occasions (id, customer_id, title, occasion_date, reminder_sent, recurring_yearly, notify_days_before, notify_time, created_by) VALUES
('occ-01', 'user-customer-01', 'Kỷ niệm ngày cưới', '2026-10-15', FALSE, TRUE, 7, '08:00:00', 'user-customer-01');

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

-- ===================================
-- NOTIFICATIONS & SHOP FOLLOW MAIL (migration 020, 024)
-- ==========================================
CREATE TABLE IF NOT EXISTS Notifications (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT,
    link VARCHAR(255),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_date DATETIME(6) NOT NULL,
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES Users(id),
    INDEX ix_notification_user (user_id, created_date)
);

CREATE TABLE IF NOT EXISTS Shop_Product_Publications (
    product_id VARCHAR(36) PRIMARY KEY,
    published_at DATETIME(6) NOT NULL
);
CREATE TABLE IF NOT EXISTS Shop_Product_Mails (
    id VARCHAR(36) PRIMARY KEY,
    product_id VARCHAR(36) NOT NULL,
    shop_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    sent_at DATETIME(6),
    CONSTRAINT uk_shop_product_mail UNIQUE (product_id, user_id),
    INDEX ix_shop_product_mail_due (status, next_attempt_at, created_at)
);
INSERT IGNORE INTO Shop_Product_Publications(product_id, published_at)
SELECT id, COALESCE(created_date, NOW()) FROM Products WHERE status = 'ACTIVE';
-- 18. DEMO SHOPS ĐỂ KIỂM THỬ THEO DÕI SHOP (chỉ dùng local; chủ shop dùng email example.test và chưa có mật khẩu nên không đăng nhập được)
INSERT IGNORE INTO Users (id, email, full_name, role, is_email_verified, status, created_by) VALUES
('follow-demo-owner-01', 'lotus-owner@example.test', 'Chủ shop Lotus', 'SHOP', TRUE, 'ACTIVE', 'follow-demo'),
('follow-demo-owner-02', 'rose-owner@example.test', 'Chủ shop Rose', 'SHOP', TRUE, 'ACTIVE', 'follow-demo'),
('follow-demo-owner-03', 'sunflower-owner@example.test', 'Chủ shop Sunflower', 'SHOP', TRUE, 'ACTIVE', 'follow-demo');

INSERT IGNORE INTO Shops (id, owner_id, name, description, status, created_by) VALUES
('follow-demo-shop-01', 'follow-demo-owner-01', 'Lotus Flower Studio', 'Hoa tươi và bó hoa theo mùa. Shop mẫu để kiểm thử theo dõi.', 'ACTIVE', 'follow-demo'),
('follow-demo-shop-02', 'follow-demo-owner-02', 'Rose Garden', 'Hoa hồng, hoa sinh nhật và hoa chúc mừng. Shop mẫu để kiểm thử theo dõi.', 'ACTIVE', 'follow-demo'),
('follow-demo-shop-03', 'follow-demo-owner-03', 'Sunflower House', 'Hoa hướng dương và giỏ hoa trang trí. Shop mẫu để kiểm thử theo dõi.', 'ACTIVE', 'follow-demo');

-- SHOP SUBSCRIPTIONS (sql/030_shop_subscriptions.sql)
-- Shop Free/Pro plans. Additive migration; no existing products or orders are removed.
USE flower_shop_db;
SET NAMES utf8mb4;
SET time_zone = '+07:00'; -- Backfill TIMESTAMP creation dates into the Vietnam calendar-month ledger.
CREATE TABLE IF NOT EXISTS Shop_Plans (
    id VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    duration_months INT NOT NULL,
    price DECIMAL(12,2) NOT NULL,
    product_limit INT NULL,
    featured_limit INT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE IF NOT EXISTS Shop_Subscriptions (
    id VARCHAR(36) PRIMARY KEY,
    shop_id VARCHAR(36) NOT NULL UNIQUE,
    plan_id VARCHAR(20) NOT NULL,
    started_at DATETIME(6) NOT NULL,
    ends_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_subscription_shop FOREIGN KEY (shop_id) REFERENCES Shops(id),
    CONSTRAINT fk_subscription_plan FOREIGN KEY (plan_id) REFERENCES Shop_Plans(id),
    INDEX ix_subscription_end (ends_at)
);
CREATE TABLE IF NOT EXISTS Shop_Plan_Payments (
    id VARCHAR(36) PRIMARY KEY,
    shop_id VARCHAR(36) NOT NULL,
    plan_id VARCHAR(20) NOT NULL,
    duration_months INT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    invoice_number VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME(6) NOT NULL,
    confirmed_at DATETIME(6),
    last_checked_at DATETIME(6),
    transaction_no VARCHAR(100),
    CONSTRAINT fk_plan_payment_shop FOREIGN KEY (shop_id) REFERENCES Shops(id),
    CONSTRAINT fk_plan_payment_plan FOREIGN KEY (plan_id) REFERENCES Shop_Plans(id),
    INDEX ix_plan_payment_pending (status, last_checked_at, created_at)
);
-- No product FK: deleting a product does not refund this month's quota.
CREATE TABLE IF NOT EXISTS Shop_Product_Postings (
    id VARCHAR(36) PRIMARY KEY,
    shop_id VARCHAR(36) NOT NULL,
    posted_at DATETIME(6) NOT NULL,
    INDEX ix_product_posting_month (shop_id, posted_at),
    CONSTRAINT fk_product_posting_shop FOREIGN KEY (shop_id) REFERENCES Shops(id)
);
CREATE TABLE IF NOT EXISTS Featured_Products (
    product_id VARCHAR(36) PRIMARY KEY,
    shop_id VARCHAR(36) NOT NULL,
    featured_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_featured_product FOREIGN KEY (product_id) REFERENCES Products(id) ON DELETE CASCADE,
    CONSTRAINT fk_featured_shop FOREIGN KEY (shop_id) REFERENCES Shops(id)
);
CREATE TABLE IF NOT EXISTS Subscription_Reminders (
    id VARCHAR(36) PRIMARY KEY,
    subscription_id VARCHAR(36) NOT NULL,
    expected_end DATETIME(6) NOT NULL,
    reminder_date DATE NOT NULL,
    hour_slot INT NOT NULL,
    mail_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_reminder_subscription FOREIGN KEY (subscription_id) REFERENCES Shop_Subscriptions(id),
    CONSTRAINT uq_subscription_reminder UNIQUE (subscription_id,expected_end,reminder_date,hour_slot),
    INDEX ix_subscription_reminder_mail (mail_status,next_attempt_at)
);
-- 3-month discount = 1 - 149000/(59000*3) = 15.8192%.
-- Same discount for 6/12 months: 298000 / 596000 VND.
INSERT INTO Shop_Plans(id,name,duration_months,price,product_limit,featured_limit,enabled) VALUES
('FREE','Free',0,0,5,0,TRUE),
('PRO1','Pro 1 tháng',1,59000,15,3,TRUE),
('PRO3','Pro 3 tháng',3,149000,25,5,TRUE),
('PRO6','Pro 6 tháng',6,298000,40,8,TRUE),
('PRO12','Pro 1 năm',12,596000,NULL,15,TRUE)
ON DUPLICATE KEY UPDATE id=Shop_Plans.id;
INSERT IGNORE INTO Shop_Product_Postings(id,shop_id,posted_at)
SELECT id,shop_id,COALESCE(created_date,NOW()) FROM Products;

-- ADMIN PRO ACTIVATION (sql/031_admin_pro_activation.sql)
-- Apply after 030_shop_subscriptions.sql. Entitlement audit is separate from payment accounting.
USE flower_shop_db;
SET NAMES utf8mb4;
CREATE TABLE IF NOT EXISTS Shop_Pro_Activations (
    id VARCHAR(36) PRIMARY KEY,
    shop_id VARCHAR(36) NOT NULL,
    plan_id VARCHAR(20) NOT NULL,
    duration_months INT NOT NULL,
    source VARCHAR(20) NOT NULL,
    payment_id VARCHAR(36) NULL UNIQUE,
    actor_id VARCHAR(36) NULL,
    note VARCHAR(1000) NOT NULL,
    starts_at DATETIME(6) NOT NULL,
    ends_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_pro_activation_shop FOREIGN KEY (shop_id) REFERENCES Shops(id),
    CONSTRAINT fk_pro_activation_plan FOREIGN KEY (plan_id) REFERENCES Shop_Plans(id),
    CONSTRAINT fk_pro_activation_payment FOREIGN KEY (payment_id) REFERENCES Shop_Plan_Payments(id),
    CONSTRAINT fk_pro_activation_actor FOREIGN KEY (actor_id) REFERENCES Users(id),
    INDEX ix_pro_activation_shop (shop_id,created_at)
);

-- =====================================================================
-- DỮ LIỆU MẪU MỞ RỘNG (chạy sau toàn bộ schema)
-- Phủ mọi bảng và mọi trạng thái chính. Mật khẩu mọi tài khoản mẫu dùng chung hash với user-admin-01.
-- Mốc thời gian tính từ @now để dữ liệu luôn 'mới' khi import (gói Pro còn hạn, đơn gần đây).
-- =====================================================================
SET @now = NOW();

-- E1. USERS
INSERT INTO Users (id, email, password_hash, google_id, full_name, phone, role, is_email_verified, status, created_date, created_by) VALUES
('user-shop-02', 'shop2@gmail.com', '$2a$12$z2frH4jxzHPjasvwidmC4.GobzOtpx/Q1fhJyXokmGjsDypKXUPf2', NULL, 'Lê Minh Châu', '0903111222', 'SHOP', TRUE, 'ACTIVE', DATE_SUB(@now, INTERVAL 172800 MINUTE), 'user-admin-01'),
('user-shop-03', 'shop3@gmail.com', '$2a$12$z2frH4jxzHPjasvwidmC4.GobzOtpx/Q1fhJyXokmGjsDypKXUPf2', NULL, 'Võ Thanh Hằng', '0904222333', 'SHOP', TRUE, 'ACTIVE', DATE_SUB(@now, INTERVAL 144000 MINUTE), 'user-admin-01'),
('user-shop-04', 'shop4@gmail.com', '$2a$12$z2frH4jxzHPjasvwidmC4.GobzOtpx/Q1fhJyXokmGjsDypKXUPf2', NULL, 'Đặng Quốc Huy', '0905333444', 'SHOP', TRUE, 'ACTIVE', DATE_SUB(@now, INTERVAL 7200 MINUTE), 'user-admin-01'),
('user-shop-05', 'shop5@gmail.com', '$2a$12$z2frH4jxzHPjasvwidmC4.GobzOtpx/Q1fhJyXokmGjsDypKXUPf2', NULL, 'Bùi Thị Lan', '0906444555', 'SHOP', TRUE, 'ACTIVE', DATE_SUB(@now, INTERVAL 288000 MINUTE), 'user-admin-01'),
('user-shop-06', 'shop6@gmail.com', '$2a$12$z2frH4jxzHPjasvwidmC4.GobzOtpx/Q1fhJyXokmGjsDypKXUPf2', NULL, 'Trịnh Văn Tuấn', '0907555666', 'SHOP', TRUE, 'ACTIVE', DATE_SUB(@now, INTERVAL 216000 MINUTE), 'user-admin-01'),
('user-staff-02', 'staff.hoa2@gmail.com', '$2a$12$z2frH4jxzHPjasvwidmC4.GobzOtpx/Q1fhJyXokmGjsDypKXUPf2', NULL, 'Ngô Thị Mai', '0908666777', 'SHOP_STAFF', TRUE, 'ACTIVE', DATE_SUB(@now, INTERVAL 64800 MINUTE), 'user-shop-02'),
('user-staff-03', 'staff.hoa3@gmail.com', '$2a$12$z2frH4jxzHPjasvwidmC4.GobzOtpx/Q1fhJyXokmGjsDypKXUPf2', NULL, 'Lý Văn Khoa', '0909777888', 'SHOP_STAFF', TRUE, 'INACTIVE', DATE_SUB(@now, INTERVAL 28800 MINUTE), 'user-shop-01'),
('user-customer-02', 'khachhang2@gmail.com', '$2a$12$z2frH4jxzHPjasvwidmC4.GobzOtpx/Q1fhJyXokmGjsDypKXUPf2', NULL, 'Nguyễn Hoàng Nam', '0911000002', 'CUSTOMER', TRUE, 'ACTIVE', DATE_SUB(@now, INTERVAL 129600 MINUTE), 'user-admin-01'),
('user-customer-03', 'khachhang3@gmail.com', '$2a$12$z2frH4jxzHPjasvwidmC4.GobzOtpx/Q1fhJyXokmGjsDypKXUPf2', NULL, 'Phạm Thu Trang', '0911000003', 'CUSTOMER', TRUE, 'ACTIVE', DATE_SUB(@now, INTERVAL 108000 MINUTE), 'user-admin-01'),
('user-customer-04', 'khachhang4@gmail.com', '$2a$12$z2frH4jxzHPjasvwidmC4.GobzOtpx/Q1fhJyXokmGjsDypKXUPf2', NULL, 'Lê Quốc Anh', '0911000004', 'CUSTOMER', TRUE, 'ACTIVE', DATE_SUB(@now, INTERVAL 86400 MINUTE), 'user-admin-01'),
('user-customer-05', 'khachhang5@gmail.com', '$2a$12$z2frH4jxzHPjasvwidmC4.GobzOtpx/Q1fhJyXokmGjsDypKXUPf2', NULL, 'Hoàng Mai Linh', '0911000005', 'CUSTOMER', TRUE, 'ACTIVE', DATE_SUB(@now, INTERVAL 43200 MINUTE), 'user-admin-01'),
('user-customer-06', 'khachhang6@gmail.com', '$2a$12$z2frH4jxzHPjasvwidmC4.GobzOtpx/Q1fhJyXokmGjsDypKXUPf2', NULL, 'Đinh Gia Huy', '0911000006', 'CUSTOMER', TRUE, 'ACTIVE', DATE_SUB(@now, INTERVAL 20160 MINUTE), 'user-admin-01'),
('user-customer-07', 'khachhang7@gmail.com', '$2a$12$z2frH4jxzHPjasvwidmC4.GobzOtpx/Q1fhJyXokmGjsDypKXUPf2', NULL, 'Vũ Tuấn Kiệt', '0911000007', 'CUSTOMER', TRUE, 'BANNED', DATE_SUB(@now, INTERVAL 72000 MINUTE), 'user-admin-01'),
('user-customer-08', 'khachhang8@gmail.com', '$2a$12$z2frH4jxzHPjasvwidmC4.GobzOtpx/Q1fhJyXokmGjsDypKXUPf2', NULL, 'Trương Bảo Ngọc', '0911000008', 'CUSTOMER', FALSE, 'ACTIVE', DATE_SUB(@now, INTERVAL 2880 MINUTE), 'user-admin-01'),
('user-customer-09', 'nguyen.google@gmail.com', NULL, 'google-oauth-demo-0001', 'Nguyễn Google User', '0911000009', 'CUSTOMER', TRUE, 'ACTIVE', DATE_SUB(@now, INTERVAL 14400 MINUTE), 'user-admin-01'),
('user-customer-10', 'khachhang10@gmail.com', '$2a$12$z2frH4jxzHPjasvwidmC4.GobzOtpx/Q1fhJyXokmGjsDypKXUPf2', NULL, 'Cao Thị Hạnh', '0911000010', 'CUSTOMER', TRUE, 'INACTIVE', DATE_SUB(@now, INTERVAL 57600 MINUTE), 'user-admin-01');

-- E2. SHOPS (ACTIVE, PENDING, INACTIVE, BANNED)
INSERT INTO Shops (id, owner_id, name, description, logo_url, status, created_by) VALUES
('shop-02', 'user-shop-02', 'Sài Gòn Floral Boutique', 'Boutique hoa cưới, kệ hoa khai trương và hoa sự kiện tại TP.HCM. Giao nhanh nội thành trong 2 giờ.', '/images/product/product-02.jpg', 'ACTIVE', 'user-admin-01'),
('shop-03', 'user-shop-03', 'Đà Lạt Garden', 'Hoa tươi từ vườn Đà Lạt: cát tường, hướng dương, hoa dại. Nhập sáng, giao trong ngày.', '/images/product/product-03.jpg', 'ACTIVE', 'user-admin-01'),
('shop-04', 'user-shop-04', 'Blossom Studio', 'Studio hoa mới mở, đang chờ admin duyệt.', NULL, 'PENDING', 'user-shop-04'),
('shop-05', 'user-shop-05', 'Hoa Xưa', 'Tiệm hoa đã bị khóa do vi phạm chính sách đăng bán.', NULL, 'BANNED', 'user-admin-01'),
('shop-06', 'user-shop-06', 'Tiệm Hoa Cũ', 'Shop tạm ngưng hoạt động theo yêu cầu của chủ shop.', NULL, 'INACTIVE', 'user-shop-06');

UPDATE Shops SET logo_url = '/images/product/product-01.jpg' WHERE id = 'shop-01';

-- E3. ADDRESSES (địa chỉ lấy hàng của shop và địa chỉ nhận của khách; mã GHN để NULL như dữ liệu gốc)
INSERT INTO Addresses (id, user_id, shop_id, address_line, ward, district, city, recipient_name, recipient_phone, address_type, is_pickup, is_default, created_by) VALUES
('addr-shop-02', NULL, 'shop-02', '123 Nguyễn Huệ', 'Bến Nghé', 'Quận 1', 'TP. Hồ Chí Minh', 'Lê Minh Châu', '0903111222', 'OFFICE', TRUE, TRUE, 'user-shop-02'),
('addr-shop-03', NULL, 'shop-03', '45 Hoàng Văn Thụ', 'Phường 9', 'Đà Lạt', 'Lâm Đồng', 'Võ Thanh Hằng', '0904222333', 'OFFICE', TRUE, TRUE, 'user-shop-03'),
('addr-shop-04', NULL, 'shop-04', '8 Lê Lợi', 'Hải Châu 1', 'Hải Châu', 'Đà Nẵng', 'Đặng Quốc Huy', '0905333444', 'OFFICE', TRUE, TRUE, 'user-shop-04'),
('addr-shop-05', NULL, 'shop-05', '77 Trần Phú', 'Lộc Thọ', 'Nha Trang', 'Khánh Hòa', 'Bùi Thị Lan', '0906444555', 'OFFICE', TRUE, TRUE, 'user-shop-05'),
('addr-customer-01b', 'user-customer-01', NULL, 'Tòa nhà FPT, Khu Công Nghệ Cao Hòa Lạc', 'Thạch Hòa', 'Thạch Thất', 'Hà Nội', 'Trần Thị B', '0987654321', 'OFFICE', FALSE, FALSE, 'user-customer-01'),
('addr-customer-02a', 'user-customer-02', NULL, '25 Lê Duẩn', 'Bến Nghé', 'Quận 1', 'TP. Hồ Chí Minh', 'Nguyễn Hoàng Nam', '0911000002', 'HOME', FALSE, TRUE, 'user-customer-02'),
('addr-customer-02b', 'user-customer-02', NULL, 'Tầng 8, Bitexco, 2 Hải Triều', 'Bến Nghé', 'Quận 1', 'TP. Hồ Chí Minh', 'Nguyễn Hoàng Nam', '0911000002', 'OFFICE', FALSE, FALSE, 'user-customer-02'),
('addr-customer-03', 'user-customer-03', NULL, '56 Trần Hưng Đạo', 'Phường 5', 'Quận 5', 'TP. Hồ Chí Minh', 'Phạm Thu Trang', '0911000003', 'HOME', FALSE, TRUE, 'user-customer-03'),
('addr-customer-04', 'user-customer-04', NULL, '12 Bạch Đằng', 'Hải Châu 1', 'Hải Châu', 'Đà Nẵng', 'Lê Quốc Anh', '0911000004', 'HOME', FALSE, TRUE, 'user-customer-04'),
('addr-customer-05', 'user-customer-05', NULL, '88 Láng Hạ', 'Láng Hạ', 'Đống Đa', 'Hà Nội', 'Hoàng Mai Linh', '0911000005', 'HOME', FALSE, TRUE, 'user-customer-05'),
('addr-customer-06', 'user-customer-06', NULL, '5 Nguyễn Trãi', 'Thanh Xuân Trung', 'Thanh Xuân', 'Hà Nội', 'Đinh Gia Huy', '0911000006', 'HOME', FALSE, TRUE, 'user-customer-06');

-- E4. PRODUCTS
INSERT INTO Products (id, shop_id, category_id, name, description, components, shelf_life_days, price, stock, product_type, status, admin_hidden, deleted, created_date, created_by) VALUES
('prod-03', 'shop-01', 'cat-16', 'Bó Hồng Pastel Ngọt Ngào', 'Hồng pastel kết hợp eucalyptus, gói giấy mờ nhẹ nhàng.', '{"pastel_roses": 12, "eucalyptus": 4}', 4, 420000, 30, 'READY_MADE', 'ACTIVE', FALSE, FALSE, DATE_SUB(@now, INTERVAL 86400 MINUTE), 'user-shop-01'),
('prod-04', 'shop-01', 'cat-13', 'Hộp Hoa Valentine Đỏ Thắm', 'Hộp tròn hồng đỏ Ecuador kèm thiệp viết tay.', '{"red_roses": 15, "baby_breath": 3, "card": 1}', 5, 580000, 25, 'READY_MADE', 'ACTIVE', FALSE, FALSE, DATE_SUB(@now, INTERVAL 83520 MINUTE), 'user-shop-01'),
('prod-05', 'shop-01', 'cat-03', 'Bó Hoa Sinh Nhật Rực Rỡ', 'Hướng dương, đồng tiền và cúc mix tông vàng cam.', '{"sunflowers": 3, "gerbera": 6, "daisy": 5}', 5, 390000, 40, 'READY_MADE', 'ACTIVE', FALSE, FALSE, DATE_SUB(@now, INTERVAL 79200 MINUTE), 'user-shop-01'),
('prod-06', 'shop-01', 'cat-08', 'Bó Hướng Dương Tốt Nghiệp', 'Bó hướng dương kèm gấu bông mini chúc mừng tốt nghiệp.', '{"sunflowers": 5, "mini_bear": 1}', 5, 320000, 35, 'READY_MADE', 'ACTIVE', FALSE, FALSE, DATE_SUB(@now, INTERVAL 72000 MINUTE), 'user-shop-01'),
('prod-07', 'shop-01', 'cat-12', 'Giỏ Hoa Tri Ân Thầy Cô', 'Giỏ mây cắm hoa tông trắng xanh, dành cho ngày 20/11.', '{"lily": 3, "carnation": 8, "greens": 1}', 6, 450000, 20, 'READY_MADE', 'ACTIVE', FALSE, FALSE, DATE_SUB(@now, INTERVAL 69120 MINUTE), 'user-shop-01'),
('prod-08', 'shop-01', 'cat-19', 'Bình Hoa Ly Thơm', 'Ly thơm cắm sẵn trong bình thủy tinh, tỏa hương dịu.', '{"oriental_lily": 5, "vase": 1}', 7, 520000, 15, 'READY_MADE', 'ACTIVE', FALSE, FALSE, DATE_SUB(@now, INTERVAL 64800 MINUTE), 'user-shop-01'),
('prod-09', 'shop-01', 'cat-23', 'Bó Baby Trắng Tinh Khôi', 'Baby trắng gói giấy kraft, hợp làm quà nhỏ.', '{"baby_breath": 20}', 5, 250000, 45, 'READY_MADE', 'ACTIVE', FALSE, FALSE, DATE_SUB(@now, INTERVAL 63360 MINUTE), 'user-shop-01'),
('prod-10', 'shop-01', 'cat-24', 'Lan Hồ Điệp Trắng 1 Cành', 'Chậu lan hồ điệp trắng để bàn, bền 3 tuần.', '{"phalaenopsis": 1, "pot": 1}', 21, 890000, 8, 'READY_MADE', 'ACTIVE', FALSE, FALSE, DATE_SUB(@now, INTERVAL 57600 MINUTE), 'user-shop-01'),
('prod-11', 'shop-01', 'cat-20', 'Bó Cúc Họa Mi Mùa Thu', 'Cúc họa mi trắng tinh khôi, giá mềm.', '{"chamomile_daisy": 25}', 5, 210000, 60, 'READY_MADE', 'ACTIVE', FALSE, FALSE, DATE_SUB(@now, INTERVAL 50400 MINUTE), 'user-shop-01'),
('prod-12', 'shop-01', 'cat-10', 'Bó Hoa 20/10 Rạng Rỡ', 'Hồng, ly và hoa đồng tiền tông hồng tặng ngày Phụ nữ Việt Nam.', '{"pink_roses": 8, "lily": 2, "gerbera": 4}', 5, 480000, 22, 'READY_MADE', 'ACTIVE', FALSE, FALSE, DATE_SUB(@now, INTERVAL 43200 MINUTE), 'user-shop-01'),
('prod-33', 'shop-01', 'cat-25', 'Hoa Thiết Kế Theo Yêu Cầu', 'Sản phẩm tham chiếu cho đơn đặt hoa riêng. Giá thực tế do shop báo.', '{"custom": true}', 5, 500000, 999, 'CUSTOM', 'ACTIVE', FALSE, FALSE, DATE_SUB(@now, INTERVAL 100800 MINUTE), 'user-shop-01'),
('prod-34', 'shop-01', 'cat-16', 'Bó Hồng Vàng Ecuador', 'Hồng vàng nhập khẩu, đang hết hàng.', '{"yellow_roses": 10}', 5, 460000, 0, 'READY_MADE', 'OUT_OF_STOCK', FALSE, FALSE, DATE_SUB(@now, INTERVAL 36000 MINUTE), 'user-shop-01'),
('prod-35', 'shop-01', 'cat-15', 'Giỏ Hoa Giáng Sinh Cũ', 'Mẫu mùa Noel năm ngoái, shop đã ẩn.', '{"pine": 5, "red_roses": 6}', 7, 430000, 5, 'READY_MADE', 'INACTIVE', FALSE, FALSE, DATE_SUB(@now, INTERVAL 432000 MINUTE), 'user-shop-01'),
('prod-31', 'shop-01', 'cat-25', 'Bó Hoa Bị Admin Ẩn', 'Bài đăng bị admin ẩn do mô tả sai sự thật.', '{"mixed": 10}', 4, 150000, 10, 'READY_MADE', 'ACTIVE', TRUE, FALSE, DATE_SUB(@now, INTERVAL 28800 MINUTE), 'user-shop-01'),
('prod-32', 'shop-01', 'cat-25', 'Mẫu Hoa Đã Xóa', 'Sản phẩm đã bị xóa mềm, chỉ giữ để đơn cũ tham chiếu.', '{"mixed": 5}', 3, 180000, 0, 'READY_MADE', 'INACTIVE', FALSE, TRUE, DATE_SUB(@now, INTERVAL 129600 MINUTE), 'user-shop-01'),
('prod-13', 'shop-02', 'cat-09', 'Hoa Cầm Tay Cô Dâu Trắng', 'Hồng trắng, lan tuyết và baby trắng tinh khôi.', '{"white_roses": 9, "snow_orchid": 3, "baby_breath": 5}', 3, 750000, 10, 'READY_MADE', 'ACTIVE', FALSE, FALSE, DATE_SUB(@now, INTERVAL 158400 MINUTE), 'user-shop-02'),
('prod-14', 'shop-02', 'cat-30', 'Kệ Hoa Khai Trương Hồng Phát', 'Kệ hai tầng cao 1,6 m, hoa tươi mix rực rỡ.', '{"gerbera": 20, "roses": 15, "lily": 5}', 5, 1200000, 6, 'READY_MADE', 'ACTIVE', FALSE, FALSE, DATE_SUB(@now, INTERVAL 151200 MINUTE), 'user-shop-02'),
('prod-15', 'shop-02', 'cat-29', 'Lẵng Hoa Chúc Mừng Thăng Chức', 'Lẵng hoa cỡ lớn tông vàng đỏ, kèm băng rôn.', '{"sunflowers": 6, "red_roses": 10, "lily": 4}', 5, 680000, 12, 'READY_MADE', 'ACTIVE', FALSE, FALSE, DATE_SUB(@now, INTERVAL 144000 MINUTE), 'user-shop-02'),
('prod-16', 'shop-02', 'cat-05', 'Vòng Hoa Chia Buồn Trắng', 'Vòng hoa cúc trắng, kèm băng chia buồn.', '{"white_chrysanthemum": 40}', 4, 950000, 5, 'READY_MADE', 'ACTIVE', FALSE, FALSE, DATE_SUB(@now, INTERVAL 136800 MINUTE), 'user-shop-02'),
('prod-17', 'shop-02', 'cat-14', 'Chậu Hoa Tết Hoàng Mai', 'Chậu hoàng mai bonsai, sai nụ, giao trước Tết.', '{"apricot_blossom": 1}', 20, 1450000, 9, 'READY_MADE', 'ACTIVE', FALSE, FALSE, DATE_SUB(@now, INTERVAL 129600 MINUTE), 'user-shop-02'),
('prod-18', 'shop-02', 'cat-11', 'Bó Hoa 8/3 Tulip Mix', 'Tulip nhập khẩu mix hồng và baby.', '{"tulip": 12, "baby_breath": 4}', 4, 540000, 30, 'READY_MADE', 'ACTIVE', FALSE, FALSE, DATE_SUB(@now, INTERVAL 122400 MINUTE), 'user-shop-02'),
('prod-19', 'shop-02', 'cat-16', 'Hộp Hồng Nhung Sang Trọng', 'Hộp vuông hồng nhung đỏ thẫm, phủ rêu.', '{"velvet_roses": 20}', 5, 690000, 18, 'READY_MADE', 'ACTIVE', FALSE, FALSE, DATE_SUB(@now, INTERVAL 115200 MINUTE), 'user-shop-02'),
('prod-20', 'shop-02', 'cat-18', 'Bó Đồng Tiền Rực Rỡ', 'Đồng tiền nhiều màu, bền hoa.', '{"gerbera": 15}', 7, 300000, 50, 'READY_MADE', 'ACTIVE', FALSE, FALSE, DATE_SUB(@now, INTERVAL 108000 MINUTE), 'user-shop-02'),
('prod-21', 'shop-02', 'cat-21', 'Cẩm Chướng Mẹ Yêu', 'Cẩm chướng đỏ và hồng tặng mẹ.', '{"carnation": 20}', 8, 260000, 40, 'READY_MADE', 'ACTIVE', FALSE, FALSE, DATE_SUB(@now, INTERVAL 100800 MINUTE), 'user-shop-02'),
('prod-22', 'shop-02', 'cat-22', 'Cát Tường Tím Dịu Dàng', 'Cát tường tím gói giấy trắng.', '{"lisianthus": 15}', 6, 360000, 28, 'READY_MADE', 'ACTIVE', FALSE, FALSE, DATE_SUB(@now, INTERVAL 93600 MINUTE), 'user-shop-02'),
('prod-23', 'shop-03', 'cat-22', 'Bó Cát Tường Đà Lạt', 'Cát tường trắng hồng nhập từ vườn Đà Lạt.', '{"lisianthus": 18}', 6, 340000, 25, 'READY_MADE', 'ACTIVE', FALSE, FALSE, DATE_SUB(@now, INTERVAL 129600 MINUTE), 'user-shop-03'),
('prod-24', 'shop-03', 'cat-17', 'Giỏ Hướng Dương Đà Lạt', 'Giỏ gỗ hướng dương và lá bạc.', '{"sunflowers": 8, "silver_leaf": 3}', 5, 480000, 20, 'READY_MADE', 'ACTIVE', FALSE, FALSE, DATE_SUB(@now, INTERVAL 122400 MINUTE), 'user-shop-03'),
('prod-25', 'shop-03', 'cat-15', 'Vòng Hoa Giáng Sinh Thông', 'Vòng thông tươi trang trí cửa, kèm nơ đỏ.', '{"pine": 1, "ribbon": 1}', 14, 410000, 14, 'READY_MADE', 'ACTIVE', FALSE, FALSE, DATE_SUB(@now, INTERVAL 115200 MINUTE), 'user-shop-03'),
('prod-26', 'shop-03', 'cat-25', 'Bó Hoa Dại Đồng Nội', 'Hoa dại mix tự nhiên, phong cách rustic.', '{"wildflowers": 30}', 4, 230000, 50, 'READY_MADE', 'ACTIVE', FALSE, FALSE, DATE_SUB(@now, INTERVAL 108000 MINUTE), 'user-shop-03'),
('prod-27', 'shop-03', 'cat-28', 'Bình Hoa Cúc Pingpong', 'Cúc pingpong cắm sẵn bình gốm.', '{"pingpong": 15, "ceramic_vase": 1}', 7, 380000, 24, 'READY_MADE', 'ACTIVE', FALSE, FALSE, DATE_SUB(@now, INTERVAL 100800 MINUTE), 'user-shop-03'),
('prod-28', 'shop-03', 'cat-27', 'Hộp Hoa Mini Quà Tặng', 'Hộp nhỏ cắm hoa mini, hợp quà cảm ơn.', '{"mini_roses": 6, "carnation": 4}', 5, 270000, 35, 'READY_MADE', 'ACTIVE', FALSE, FALSE, DATE_SUB(@now, INTERVAL 93600 MINUTE), 'user-shop-03'),
('prod-29', 'shop-03', 'cat-06', 'Giỏ Hoa Thăm Bệnh Nhẹ Nhàng', 'Giỏ hoa tông xanh trắng, không mùi nồng.', '{"daisy": 10, "lisianthus": 6}', 5, 560000, 11, 'READY_MADE', 'ACTIVE', FALSE, FALSE, DATE_SUB(@now, INTERVAL 86400 MINUTE), 'user-shop-03'),
('prod-30', 'shop-03', 'cat-07', 'Bó Hoa Cảm Ơn Pastel', 'Bó pastel kèm thiệp cảm ơn.', '{"pastel_mix": 15}', 5, 310000, 33, 'READY_MADE', 'ACTIVE', FALSE, FALSE, DATE_SUB(@now, INTERVAL 79200 MINUTE), 'user-shop-03'),
('prod-36', 'shop-04', 'cat-25', 'Bó Hoa Thử Nghiệm', 'Sản phẩm của shop đang chờ duyệt, chưa hiển thị công khai.', '{"mixed": 10}', 4, 199000, 10, 'READY_MADE', 'ACTIVE', FALSE, FALSE, DATE_SUB(@now, INTERVAL 4320 MINUTE), 'user-shop-04'),
('prod-37', 'shop-05', 'cat-25', 'Bó Hoa Shop Bị Khóa', 'Sản phẩm của shop đã bị khóa.', '{"mixed": 10}', 4, 175000, 10, 'READY_MADE', 'ACTIVE', FALSE, FALSE, DATE_SUB(@now, INTERVAL 216000 MINUTE), 'user-shop-05'),
('prod-38', 'shop-06', 'cat-25', 'Bó Hoa Shop Tạm Nghỉ', 'Sản phẩm của shop đang tạm nghỉ.', '{"mixed": 12}', 4, 205000, 7, 'READY_MADE', 'ACTIVE', FALSE, FALSE, DATE_SUB(@now, INTERVAL 201600 MINUTE), 'user-shop-06');

-- Sản phẩm đang bán (ACTIVE, không bị ẩn) của shop hoạt động được ghi nhận là đã công bố
INSERT IGNORE INTO Shop_Product_Publications (product_id, published_at)
SELECT id, COALESCE(created_date, @now) FROM Products WHERE status = 'ACTIVE' AND deleted = FALSE;
INSERT IGNORE INTO Shop_Product_Postings (id, shop_id, posted_at)
SELECT id, shop_id, COALESCE(created_date, @now) FROM Products;

-- E4.1 PRODUCT IMAGES (ảnh chính + ảnh phụ)
INSERT INTO Product_Images (id, product_id, image_url, is_primary, display_order) VALUES
('img-prod-03-1', 'prod-03', '/images/product/product-01.jpg', TRUE, 0),
('img-prod-03-2', 'prod-03', '/images/product/product-02.jpg', FALSE, 1),
('img-prod-03-3', 'prod-03', '/images/product/product-03.jpg', FALSE, 2),
('img-prod-04-1', 'prod-04', '/images/product/product-02.jpg', TRUE, 0),
('img-prod-04-2', 'prod-04', '/images/product/product-03.jpg', FALSE, 1),
('img-prod-05-1', 'prod-05', '/images/product/product-03.jpg', TRUE, 0),
('img-prod-05-2', 'prod-05', '/images/product/product-04.jpg', FALSE, 1),
('img-prod-06-1', 'prod-06', '/images/product/product-04.jpg', TRUE, 0),
('img-prod-06-2', 'prod-06', '/images/product/product-05.jpg', FALSE, 1),
('img-prod-07-1', 'prod-07', '/images/product/product-05.jpg', TRUE, 0),
('img-prod-07-2', 'prod-07', '/images/product/product-01.jpg', FALSE, 1),
('img-prod-07-3', 'prod-07', '/images/product/product-02.jpg', FALSE, 2),
('img-prod-08-1', 'prod-08', '/images/product/product-01.jpg', TRUE, 0),
('img-prod-08-2', 'prod-08', '/images/product/product-02.jpg', FALSE, 1),
('img-prod-09-1', 'prod-09', '/images/product/product-02.jpg', TRUE, 0),
('img-prod-09-2', 'prod-09', '/images/product/product-03.jpg', FALSE, 1),
('img-prod-10-1', 'prod-10', '/images/product/product-03.jpg', TRUE, 0),
('img-prod-10-2', 'prod-10', '/images/product/product-04.jpg', FALSE, 1),
('img-prod-11-1', 'prod-11', '/images/product/product-04.jpg', TRUE, 0),
('img-prod-11-2', 'prod-11', '/images/product/product-05.jpg', FALSE, 1),
('img-prod-11-3', 'prod-11', '/images/product/product-01.jpg', FALSE, 2),
('img-prod-12-1', 'prod-12', '/images/product/product-05.jpg', TRUE, 0),
('img-prod-12-2', 'prod-12', '/images/product/product-01.jpg', FALSE, 1),
('img-prod-33-1', 'prod-33', '/images/product/product-01.jpg', TRUE, 0),
('img-prod-33-2', 'prod-33', '/images/product/product-02.jpg', FALSE, 1),
('img-prod-34-1', 'prod-34', '/images/product/product-02.jpg', TRUE, 0),
('img-prod-35-1', 'prod-35', '/images/product/product-03.jpg', TRUE, 0),
('img-prod-35-2', 'prod-35', '/images/product/product-04.jpg', FALSE, 1),
('img-prod-35-3', 'prod-35', '/images/product/product-05.jpg', FALSE, 2),
('img-prod-31-1', 'prod-31', '/images/product/product-04.jpg', TRUE, 0),
('img-prod-31-2', 'prod-31', '/images/product/product-05.jpg', FALSE, 1),
('img-prod-32-1', 'prod-32', '/images/product/product-05.jpg', TRUE, 0),
('img-prod-13-1', 'prod-13', '/images/product/product-01.jpg', TRUE, 0),
('img-prod-13-2', 'prod-13', '/images/product/product-02.jpg', FALSE, 1),
('img-prod-14-1', 'prod-14', '/images/product/product-02.jpg', TRUE, 0),
('img-prod-14-2', 'prod-14', '/images/product/product-03.jpg', FALSE, 1),
('img-prod-14-3', 'prod-14', '/images/product/product-04.jpg', FALSE, 2),
('img-prod-15-1', 'prod-15', '/images/product/product-03.jpg', TRUE, 0),
('img-prod-15-2', 'prod-15', '/images/product/product-04.jpg', FALSE, 1),
('img-prod-16-1', 'prod-16', '/images/product/product-04.jpg', TRUE, 0),
('img-prod-16-2', 'prod-16', '/images/product/product-05.jpg', FALSE, 1),
('img-prod-17-1', 'prod-17', '/images/product/product-05.jpg', TRUE, 0),
('img-prod-17-2', 'prod-17', '/images/product/product-01.jpg', FALSE, 1),
('img-prod-18-1', 'prod-18', '/images/product/product-01.jpg', TRUE, 0),
('img-prod-18-2', 'prod-18', '/images/product/product-02.jpg', FALSE, 1),
('img-prod-18-3', 'prod-18', '/images/product/product-03.jpg', FALSE, 2),
('img-prod-19-1', 'prod-19', '/images/product/product-02.jpg', TRUE, 0),
('img-prod-19-2', 'prod-19', '/images/product/product-03.jpg', FALSE, 1),
('img-prod-20-1', 'prod-20', '/images/product/product-03.jpg', TRUE, 0),
('img-prod-20-2', 'prod-20', '/images/product/product-04.jpg', FALSE, 1),
('img-prod-21-1', 'prod-21', '/images/product/product-04.jpg', TRUE, 0),
('img-prod-21-2', 'prod-21', '/images/product/product-05.jpg', FALSE, 1),
('img-prod-22-1', 'prod-22', '/images/product/product-05.jpg', TRUE, 0),
('img-prod-22-2', 'prod-22', '/images/product/product-01.jpg', FALSE, 1),
('img-prod-22-3', 'prod-22', '/images/product/product-02.jpg', FALSE, 2),
('img-prod-23-1', 'prod-23', '/images/product/product-01.jpg', TRUE, 0),
('img-prod-23-2', 'prod-23', '/images/product/product-02.jpg', FALSE, 1),
('img-prod-24-1', 'prod-24', '/images/product/product-02.jpg', TRUE, 0),
('img-prod-24-2', 'prod-24', '/images/product/product-03.jpg', FALSE, 1),
('img-prod-25-1', 'prod-25', '/images/product/product-03.jpg', TRUE, 0),
('img-prod-25-2', 'prod-25', '/images/product/product-04.jpg', FALSE, 1),
('img-prod-26-1', 'prod-26', '/images/product/product-04.jpg', TRUE, 0),
('img-prod-26-2', 'prod-26', '/images/product/product-05.jpg', FALSE, 1),
('img-prod-26-3', 'prod-26', '/images/product/product-01.jpg', FALSE, 2),
('img-prod-27-1', 'prod-27', '/images/product/product-05.jpg', TRUE, 0),
('img-prod-27-2', 'prod-27', '/images/product/product-01.jpg', FALSE, 1),
('img-prod-28-1', 'prod-28', '/images/product/product-01.jpg', TRUE, 0),
('img-prod-28-2', 'prod-28', '/images/product/product-02.jpg', FALSE, 1),
('img-prod-29-1', 'prod-29', '/images/product/product-02.jpg', TRUE, 0),
('img-prod-29-2', 'prod-29', '/images/product/product-03.jpg', FALSE, 1),
('img-prod-30-1', 'prod-30', '/images/product/product-03.jpg', TRUE, 0),
('img-prod-30-2', 'prod-30', '/images/product/product-04.jpg', FALSE, 1),
('img-prod-30-3', 'prod-30', '/images/product/product-05.jpg', FALSE, 2),
('img-prod-36-1', 'prod-36', '/images/product/product-04.jpg', TRUE, 0),
('img-prod-36-2', 'prod-36', '/images/product/product-05.jpg', FALSE, 1),
('img-prod-37-1', 'prod-37', '/images/product/product-05.jpg', TRUE, 0),
('img-prod-37-2', 'prod-37', '/images/product/product-01.jpg', FALSE, 1),
('img-prod-38-1', 'prod-38', '/images/product/product-01.jpg', TRUE, 0),
('img-prod-38-2', 'prod-38', '/images/product/product-02.jpg', FALSE, 1),
('img-prod-01-1', 'prod-01', '/images/product/product-01.jpg', TRUE, 0),
('img-prod-02-1', 'prod-02', '/images/product/product-02.jpg', TRUE, 0);

-- E4.2 PRODUCT VIDEOS
INSERT INTO Product_Videos (id, product_id, video_url, title, description, display_order) VALUES
('vid-prod-03', 'prod-03', 'https://res.cloudinary.com/demo/video/upload/dog.mp4', 'Cách gói bó hồng pastel', 'Video ngắn thợ hoa gói bó hồng pastel.', 0),
('vid-prod-13', 'prod-13', 'https://res.cloudinary.com/demo/video/upload/elephants.mp4', 'Hoa cầm tay cô dâu', 'Quay cận cảnh bó hoa cầm tay.', 0),
('vid-prod-14', 'prod-14', 'https://res.cloudinary.com/demo/video/upload/dog.mp4', 'Lắp đặt kệ khai trương', 'Đội shop lắp đặt kệ hoa tại cửa hàng khách.', 0),
('vid-prod-24', 'prod-24', 'https://res.cloudinary.com/demo/video/upload/elephants.mp4', 'Giỏ hướng dương Đà Lạt', 'Hoa nhập sáng từ vườn Đà Lạt.', 0);

-- E5. COUPONS (platform và shop; còn hạn, hết hạn, tắt, hết lượt)
INSERT INTO Coupons (id, shop_id, code, discount_type, discount_value, min_order_value, max_discount_value, start_date, end_date, usage_limit, used_count, status, created_by) VALUES
('coupon-02', NULL, 'FLOWER50K', 'FIXED_AMOUNT', 50000, 300000, 50000, DATE_SUB(@now, INTERVAL 43200 MINUTE), DATE_ADD(@now, INTERVAL 86400 MINUTE), 200, 14, 'ACTIVE', 'user-admin-01'),
('coupon-03', NULL, 'FREESHIP30', 'FIXED_AMOUNT', 30000, 150000, 30000, DATE_SUB(@now, INTERVAL 28800 MINUTE), DATE_ADD(@now, INTERVAL 57600 MINUTE), 500, 62, 'ACTIVE', 'user-admin-01'),
('coupon-04', 'shop-02', 'SGON15', 'PERCENTAGE', 15, 500000, 100000, DATE_SUB(@now, INTERVAL 21600 MINUTE), DATE_ADD(@now, INTERVAL 43200 MINUTE), 50, 8, 'ACTIVE', 'user-shop-02'),
('coupon-05', 'shop-03', 'DALAT20', 'PERCENTAGE', 20, 250000, 80000, DATE_SUB(@now, INTERVAL 14400 MINUTE), DATE_ADD(@now, INTERVAL 28800 MINUTE), 30, 5, 'ACTIVE', 'user-shop-03'),
('coupon-06', 'shop-01', 'EXPIRED10', 'PERCENTAGE', 10, 100000, 40000, DATE_SUB(@now, INTERVAL 129600 MINUTE), DATE_SUB(@now, INTERVAL 43200 MINUTE), 100, 37, 'EXPIRED', 'user-shop-01'),
('coupon-07', 'shop-01', 'TETSALE', 'PERCENTAGE', 12, 300000, 70000, DATE_SUB(@now, INTERVAL 7200 MINUTE), DATE_ADD(@now, INTERVAL 86400 MINUTE), 100, 0, 'DISABLED', 'user-shop-01'),
('coupon-08', NULL, 'HETLUOT', 'FIXED_AMOUNT', 20000, 100000, 20000, DATE_SUB(@now, INTERVAL 14400 MINUTE), DATE_ADD(@now, INTERVAL 14400 MINUTE), 10, 10, 'ACTIVE', 'user-admin-01'),
('coupon-09', NULL, 'NEWBIE25', 'PERCENTAGE', 25, 100000, 60000, DATE_SUB(@now, INTERVAL 43200 MINUTE), DATE_ADD(@now, INTERVAL 129600 MINUTE), 1000, 121, 'ACTIVE', 'user-admin-01');

-- E6. CART ITEMS
INSERT INTO Cart_Items (id, user_id, product_id, quantity, created_by) VALUES
('cart-02', 'user-customer-02', 'prod-05', 1, 'user-customer-02'),
('cart-03', 'user-customer-02', 'prod-18', 2, 'user-customer-02'),
('cart-04', 'user-customer-03', 'prod-27', 1, 'user-customer-03'),
('cart-05', 'user-customer-04', 'prod-12', 1, 'user-customer-04'),
('cart-06', 'user-customer-05', 'prod-20', 3, 'user-customer-05'),
('cart-07', 'user-customer-05', 'prod-23', 1, 'user-customer-05'),
('cart-08', 'user-customer-01', 'prod-03', 1, 'user-customer-01');

-- E7. CUSTOM ORDER REQUESTS (đủ 4 trạng thái)
INSERT INTO Custom_Order_Requests (id, customer_id, shop_id, budget, desired_components, description, status, created_date, created_by) VALUES
('req-02', 'user-customer-01', 'shop-01', 1200000, '{"main_flowers": ["Hồng Ecuador", "Cát tường"], "wrapper": "Giấy mờ", "color_tone": "Hồng phấn", "occasion": "Sinh nhật mẹ"}', 'Lẵng hoa tông hồng phấn tặng mẹ nhân dịp 60 tuổi.', 'ACCEPTED', DATE_SUB(@now, INTERVAL 12960 MINUTE), 'user-customer-01'),
('req-03', 'user-customer-03', 'shop-01', 900000, '{"main_flowers": ["Hướng dương"], "wrapper": "Kraft", "occasion": "Khai trương"}', 'Hộp hoa hướng dương mừng khai trương tiệm nail.', 'ACCEPTED', DATE_SUB(@now, INTERVAL 8640 MINUTE), 'user-customer-03'),
('req-04', 'user-customer-02', 'shop-02', 1500000, '{"main_flowers": ["Tulip", "Hồng trắng"], "wrapper": "Hộp mica", "occasion": "Kỷ niệm"}', 'Hộp hoa tulip kỷ niệm 5 năm ngày cưới.', 'ACCEPTED', DATE_SUB(@now, INTERVAL 37440 MINUTE), 'user-customer-02'),
('req-05', 'user-customer-04', 'shop-03', 700000, '{"main_flowers": ["Cát tường"], "color_tone": "Tím"}', 'Bó cát tường tím tặng bạn gái.', 'REQUESTED', DATE_SUB(@now, INTERVAL 180 MINUTE), 'user-customer-04'),
('req-06', 'user-customer-05', 'shop-01', 300000, '{"main_flowers": ["Lan Hồ Điệp"]}', 'Chậu lan hồ điệp ngân sách thấp.', 'REJECTED', DATE_SUB(@now, INTERVAL 20160 MINUTE), 'user-customer-05'),
('req-07', 'user-customer-06', 'shop-02', 2000000, '{"main_flowers": ["Hồng", "Ly"], "wrapper": "Giỏ mây", "occasion": "Tân gia"}', 'Giỏ hoa lớn trang trí tiệc tân gia.', 'NEGOTIATING', DATE_SUB(@now, INTERVAL 2880 MINUTE), 'user-customer-06');

-- E8. ORDERS (đủ trạng thái, kèm chi tiết, thanh toán, vận đơn, hoàn tiền)
INSERT INTO Orders (id, customer_id, shop_id, delivery_address_id, recipient_name, recipient_phone, coupon_id, order_type, sub_total, discount_amount, shipping_fee, total_amount, deposit_amount, customer_note, cancel_reason, shop_note, checkout_group, custom_request_id, status, created_date, created_by) VALUES
('order-02', 'user-customer-02', 'shop-01', 'addr-customer-02a', 'Nguyễn Hoàng Nam', '0911000002', 'coupon-02', 'STANDARD', 670000, 50000, 30000, 650000, 0, 'Giao giờ hành chính giúp mình nhé.', NULL, NULL, NULL, NULL, 'COMPLETED', DATE_SUB(@now, INTERVAL 57600 MINUTE), 'user-customer-02'),
('order-03', 'user-customer-03', 'shop-02', 'addr-customer-03', 'Phạm Thu Trang', '0911000003', 'coupon-04', 'STANDARD', 1230000, 100000, 35000, 1165000, 0, NULL, NULL, NULL, NULL, NULL, 'COMPLETED', DATE_SUB(@now, INTERVAL 43200 MINUTE), 'user-customer-03'),
('order-04', 'user-customer-04', 'shop-03', 'addr-customer-04', 'Lê Quốc Anh', '0911000004', NULL, 'STANDARD', 940000, 0, 30000, 970000, 0, 'Gọi trước khi giao.', NULL, NULL, NULL, NULL, 'COMPLETED', DATE_SUB(@now, INTERVAL 36000 MINUTE), 'user-customer-04'),
('order-05', 'user-customer-05', 'shop-01', 'addr-customer-05', 'Hoàng Mai Linh', '0911000005', NULL, 'STANDARD', 580000, 0, 25000, 605000, 0, NULL, NULL, NULL, NULL, NULL, 'DELIVERING', DATE_SUB(@now, INTERVAL 1440 MINUTE), 'user-customer-05'),
('order-06', 'user-customer-01', 'shop-02', 'addr-customer-01', 'Trần Thị B', '0987654321', NULL, 'STANDARD', 750000, 0, 40000, 790000, 0, NULL, NULL, NULL, NULL, NULL, 'DELIVERING', DATE_SUB(@now, INTERVAL 2880 MINUTE), 'user-customer-01'),
('order-07', 'user-customer-02', 'shop-01', 'addr-customer-02a', 'Nguyễn Hoàng Nam', '0911000002', 'coupon-01', 'STANDARD', 780000, 50000, 30000, 760000, 0, NULL, NULL, 'Shop đang gói hoa.', NULL, NULL, 'PROCESSING', DATE_SUB(@now, INTERVAL 180 MINUTE), 'user-customer-02'),
('order-08', 'user-customer-03', 'shop-03', 'addr-customer-03', 'Phạm Thu Trang', '0911000003', 'coupon-05', 'STANDARD', 650000, 80000, 30000, 600000, 0, NULL, NULL, NULL, NULL, NULL, 'PROCESSING', DATE_SUB(@now, INTERVAL 300 MINUTE), 'user-customer-03'),
('order-09', 'user-customer-04', 'shop-01', 'addr-customer-04', 'Lê Quốc Anh', '0911000004', NULL, 'STANDARD', 420000, 0, 25000, 445000, 10, NULL, NULL, NULL, NULL, NULL, 'PENDING', DATE_SUB(@now, INTERVAL 0 MINUTE), 'user-customer-04'),
('order-10', 'user-customer-05', 'shop-02', 'addr-customer-05', 'Hoàng Mai Linh', '0911000005', NULL, 'STANDARD', 560000, 0, 35000, 595000, 8, NULL, NULL, NULL, 'group-01', NULL, 'PENDING', DATE_SUB(@now, INTERVAL 0 MINUTE), 'user-customer-05'),
('order-11', 'user-customer-05', 'shop-03', 'addr-customer-05', 'Hoàng Mai Linh', '0911000005', NULL, 'STANDARD', 540000, 0, 30000, 570000, 8, NULL, NULL, NULL, 'group-01', NULL, 'PENDING', DATE_SUB(@now, INTERVAL 0 MINUTE), 'user-customer-05'),
('order-12', 'user-customer-06', 'shop-01', 'addr-customer-06', 'Đinh Gia Huy', '0911000006', NULL, 'STANDARD', 320000, 0, 25000, 345000, 0, NULL, 'Khách đổi ý, không cần nữa.', NULL, NULL, NULL, 'CANCELLED', DATE_SUB(@now, INTERVAL 21600 MINUTE), 'user-customer-06'),
('order-13', 'user-customer-02', 'shop-02', 'addr-customer-02b', 'Nguyễn Hoàng Nam', '0911000002', NULL, 'STANDARD', 680000, 0, 35000, 715000, 0, NULL, 'Quá hạn 10 phút không quét mã thanh toán', NULL, NULL, NULL, 'CANCELLED', DATE_SUB(@now, INTERVAL 17280 MINUTE), 'user-customer-02'),
('order-14', 'user-customer-03', 'shop-01', 'addr-customer-03', 'Phạm Thu Trang', '0911000003', NULL, 'STANDARD', 450000, 0, 25000, 475000, 0, NULL, 'Hoa không đúng mô tả, shop đồng ý hoàn tiền.', NULL, NULL, NULL, 'REFUNDED', DATE_SUB(@now, INTERVAL 28800 MINUTE), 'user-customer-03'),
('order-15', 'user-customer-04', 'shop-02', 'addr-customer-04', 'Lê Quốc Anh', '0911000004', NULL, 'STANDARD', 720000, 0, 35000, 755000, 0, NULL, NULL, NULL, NULL, NULL, 'COMPLETED', DATE_SUB(@now, INTERVAL 25920 MINUTE), 'user-customer-04'),
('order-16', 'user-customer-01', 'shop-03', 'addr-customer-01', 'Trần Thị B', '0987654321', NULL, 'STANDARD', 560000, 0, 30000, 590000, 0, NULL, NULL, NULL, NULL, NULL, 'COMPLETED', DATE_SUB(@now, INTERVAL 47520 MINUTE), 'user-customer-01'),
('order-17', 'user-customer-05', 'shop-01', 'addr-customer-05', 'Hoàng Mai Linh', '0911000005', 'coupon-09', 'STANDARD', 1000000, 60000, 30000, 970000, 0, NULL, NULL, NULL, NULL, NULL, 'COMPLETED', DATE_SUB(@now, INTERVAL 11520 MINUTE), 'user-customer-05'),
('order-21', 'user-customer-04', 'shop-01', 'addr-customer-04', 'Lê Quốc Anh', '0911000004', NULL, 'STANDARD', 890000, 0, 25000, 915000, 0, NULL, NULL, 'Giao lần 1 thất bại, đang liên hệ khách.', NULL, NULL, 'DELIVERING', DATE_SUB(@now, INTERVAL 1680 MINUTE), 'user-customer-04'),
('order-18', 'user-customer-01', 'shop-01', 'addr-customer-01b', 'Trần Thị B', '0987654321', NULL, 'CUSTOM', 1200000, 0, 40000, 1240000, 372000, NULL, NULL, NULL, NULL, 'req-02', 'AWAITING_DEPOSIT', DATE_SUB(@now, INTERVAL 1560 MINUTE), 'user-customer-01'),
('order-19', 'user-customer-03', 'shop-01', 'addr-customer-03', 'Phạm Thu Trang', '0911000003', NULL, 'CUSTOM', 900000, 0, 30000, 930000, 279000, NULL, NULL, NULL, NULL, 'req-03', 'DEPOSIT_PAID', DATE_SUB(@now, INTERVAL 4320 MINUTE), 'user-customer-03'),
('order-20', 'user-customer-02', 'shop-02', 'addr-customer-02a', 'Nguyễn Hoàng Nam', '0911000002', NULL, 'CUSTOM', 1500000, 0, 40000, 1540000, 462000, NULL, NULL, NULL, NULL, 'req-04', 'COMPLETED', DATE_SUB(@now, INTERVAL 31680 MINUTE), 'user-customer-02');

INSERT INTO Order_Details (id, order_id, product_id, custom_materials, price, quantity, created_by) VALUES
('dtl-order-02-1', 'order-02', 'prod-03', NULL, 420000, 1, 'user-customer-02'),
('dtl-order-02-2', 'order-02', 'prod-09', NULL, 250000, 1, 'user-customer-02'),
('dtl-order-03-1', 'order-03', 'prod-19', NULL, 690000, 1, 'user-customer-03'),
('dtl-order-03-2', 'order-03', 'prod-18', NULL, 540000, 1, 'user-customer-03'),
('dtl-order-04-1', 'order-04', 'prod-24', NULL, 480000, 1, 'user-customer-04'),
('dtl-order-04-2', 'order-04', 'prod-26', NULL, 230000, 2, 'user-customer-04'),
('dtl-order-05-1', 'order-05', 'prod-04', NULL, 580000, 1, 'user-customer-05'),
('dtl-order-06-1', 'order-06', 'prod-13', NULL, 750000, 1, 'user-customer-01'),
('dtl-order-07-1', 'order-07', 'prod-05', NULL, 390000, 2, 'user-customer-02'),
('dtl-order-08-1', 'order-08', 'prod-23', NULL, 340000, 1, 'user-customer-03'),
('dtl-order-08-2', 'order-08', 'prod-30', NULL, 310000, 1, 'user-customer-03'),
('dtl-order-09-1', 'order-09', 'prod-11', NULL, 210000, 2, 'user-customer-04'),
('dtl-order-10-1', 'order-10', 'prod-20', NULL, 300000, 1, 'user-customer-05'),
('dtl-order-10-2', 'order-10', 'prod-21', NULL, 260000, 1, 'user-customer-05'),
('dtl-order-11-1', 'order-11', 'prod-28', NULL, 270000, 2, 'user-customer-05'),
('dtl-order-12-1', 'order-12', 'prod-06', NULL, 320000, 1, 'user-customer-06'),
('dtl-order-13-1', 'order-13', 'prod-15', NULL, 680000, 1, 'user-customer-02'),
('dtl-order-14-1', 'order-14', 'prod-07', NULL, 450000, 1, 'user-customer-03'),
('dtl-order-15-1', 'order-15', 'prod-22', NULL, 360000, 2, 'user-customer-04'),
('dtl-order-16-1', 'order-16', 'prod-29', NULL, 560000, 1, 'user-customer-01'),
('dtl-order-17-1', 'order-17', 'prod-08', NULL, 520000, 1, 'user-customer-05'),
('dtl-order-17-2', 'order-17', 'prod-12', NULL, 480000, 1, 'user-customer-05'),
('dtl-order-21-1', 'order-21', 'prod-10', NULL, 890000, 1, 'user-customer-04'),
('dtl-order-18-1', 'order-18', 'prod-33', '{"main_flower": "Hồng Ecuador hồng phấn", "secondary": "Cát tường", "wrapper": "Giấy mờ", "ribbon": "Hồng nhạt"}', 1200000, 1, 'user-customer-01'),
('dtl-order-19-1', 'order-19', 'prod-33', '{"main_flower": "Hướng dương", "wrapper": "Kraft", "accessory": "Thiệp chúc mừng"}', 900000, 1, 'user-customer-03'),
('dtl-order-20-1', 'order-20', 'prod-33', '{"main_flower": "Tulip", "secondary": "Hồng trắng", "wrapper": "Hộp mica"}', 1500000, 1, 'user-customer-02');

INSERT INTO Payments (id, order_id, payment_type, payment_method, amount, gateway_transaction_no, invoice_number, group_invoice, gateway_response, status, created_by) VALUES
('pay-02', 'order-02', 'FULL', 'ONLINE', 650000, 'SEPAY-TX-0002', 'INV-0002', NULL, '{"order_status":"CAPTURED","payment_method":"BANK_TRANSFER"}', 'SUCCESS', 'user-customer-02'),
('pay-03', 'order-03', 'FULL', 'ONLINE', 1165000, 'SEPAY-TX-0003', 'INV-0003', NULL, '{"order_status":"CAPTURED","payment_method":"CARD"}', 'SUCCESS', 'user-customer-03'),
('pay-04', 'order-04', 'FULL', 'COD', 970000, NULL, 'COD-DEMO0004', NULL, NULL, 'SUCCESS', 'user-customer-04'),
('pay-05', 'order-05', 'FULL', 'ONLINE', 605000, 'SEPAY-TX-0005', 'INV-0005', NULL, '{"order_status":"CAPTURED"}', 'SUCCESS', 'user-customer-05'),
('pay-06', 'order-06', 'FULL', 'COD', 790000, NULL, 'COD-DEMO0006', NULL, NULL, 'PENDING', 'user-customer-01'),
('pay-07', 'order-07', 'FULL', 'ONLINE', 760000, 'SEPAY-TX-0007', 'INV-0007', NULL, '{"order_status":"CAPTURED"}', 'SUCCESS', 'user-customer-02'),
('pay-08', 'order-08', 'FULL', 'ONLINE', 600000, 'SEPAY-TX-0008', 'INV-0008', NULL, '{"order_status":"CAPTURED"}', 'SUCCESS', 'user-customer-03'),
('pay-09', 'order-09', 'FULL', 'ONLINE', 445000, NULL, 'INV-0009', NULL, NULL, 'PENDING', 'user-customer-04'),
('pay-10', 'order-10', 'FULL', 'ONLINE', 595000, NULL, 'INV-0010', 'GRP-DEMO0001', NULL, 'PENDING', 'user-customer-05'),
('pay-11', 'order-11', 'FULL', 'ONLINE', 570000, NULL, 'INV-0011', 'GRP-DEMO0001', NULL, 'PENDING', 'user-customer-05'),
('pay-12', 'order-12', 'FULL', 'ONLINE', 345000, NULL, 'INV-0012', NULL, '{"order_status":"CANCELLED"}', 'FAILED', 'user-customer-06'),
('pay-13', 'order-13', 'FULL', 'ONLINE', 715000, NULL, 'INV-0013', NULL, '{"order_status":"EXPIRED"}', 'FAILED', 'user-customer-02'),
('pay-14', 'order-14', 'FULL', 'ONLINE', 475000, 'SEPAY-TX-0014', 'INV-0014', NULL, '{"order_status":"CAPTURED"}', 'SUCCESS', 'user-customer-03'),
('pay-15', 'order-15', 'FULL', 'ONLINE', 755000, 'SEPAY-TX-0015', 'INV-0015', NULL, '{"order_status":"CAPTURED"}', 'SUCCESS', 'user-customer-04'),
('pay-16', 'order-16', 'FULL', 'ONLINE', 590000, 'SEPAY-TX-0016', 'INV-0016', NULL, '{"order_status":"CAPTURED"}', 'SUCCESS', 'user-customer-01'),
('pay-17', 'order-17', 'FULL', 'ONLINE', 970000, 'SEPAY-TX-0017', 'INV-0017', NULL, '{"order_status":"CAPTURED"}', 'SUCCESS', 'user-customer-05'),
('pay-21', 'order-21', 'FULL', 'ONLINE', 915000, 'SEPAY-TX-0021', 'INV-0021', NULL, '{"order_status":"CAPTURED"}', 'SUCCESS', 'user-customer-04'),
('pay-18', 'order-18', 'DEPOSIT', 'ONLINE', 372000, NULL, 'INV-0018-DEP', NULL, NULL, 'PENDING', 'user-customer-01'),
('pay-19', 'order-19', 'DEPOSIT', 'ONLINE', 279000, 'SEPAY-TX-0019', 'INV-0019-DEP', NULL, '{"order_status":"CAPTURED"}', 'SUCCESS', 'user-customer-03'),
('pay-20a', 'order-20', 'DEPOSIT', 'ONLINE', 462000, 'SEPAY-TX-0020', 'INV-0020-DEP', NULL, '{"order_status":"CAPTURED"}', 'SUCCESS', 'user-customer-02'),
('pay-20b', 'order-20', 'FINAL', 'COD', 1078000, NULL, 'COD-DEMO0020', NULL, NULL, 'SUCCESS', 'user-customer-02');

INSERT INTO Deliveries (id, order_id, delivery_partner_id, tracking_code, status, ghn_status, tracking_notes, created_by) VALUES
('del-02', 'order-02', 'GHN', 'GHN-DEMO-0002', 'DELIVERED', 'delivered', 'Giao thành công, người nhận ký nhận', 'user-shop-01'),
('del-03', 'order-03', 'GHN', 'GHN-DEMO-0003', 'DELIVERED', 'delivered', 'Đã giao đến người nhận', 'user-shop-02'),
('del-04', 'order-04', 'GHN', 'GHN-DEMO-0004', 'DELIVERED', 'delivered', 'Thu hộ COD thành công', 'user-shop-03'),
('del-05', 'order-05', 'GHN', 'GHN-DEMO-0005', 'ON_THE_WAY', 'delivering', 'Shipper đang giao', 'user-shop-01'),
('del-06', 'order-06', 'GHN', 'GHN-DEMO-0006', 'PICKED_UP', 'picked', 'Đã lấy hàng tại shop', 'user-shop-02'),
('del-14', 'order-14', 'GHN', 'GHN-DEMO-0014', 'RETURNED', 'returned', 'Hàng đã hoàn về shop', 'user-shop-01'),
('del-15', 'order-15', 'GHN', 'GHN-DEMO-0015', 'DELIVERED', 'delivered', 'Giao thành công', 'user-shop-02'),
('del-16', 'order-16', 'GHN', 'GHN-DEMO-0016', 'DELIVERED', 'delivered', 'Giao thành công', 'user-shop-03'),
('del-17', 'order-17', 'GHN', 'GHN-DEMO-0017', 'DELIVERED', 'delivered', 'Giao thành công', 'user-shop-01'),
('del-21', 'order-21', 'GHN', 'GHN-DEMO-0021', 'FAILED', 'delivery_fail', 'Người nhận không nghe máy', 'user-shop-01'),
('del-20', 'order-20', 'GHN', 'GHN-DEMO-0020', 'DELIVERED', 'delivered', 'Thu nốt phần còn lại khi giao', 'user-shop-02');

INSERT INTO Refunds (id, order_id, amount, reason, gateway_response, gateway_refund_id, status, created_by) VALUES
('ref-01', 'order-14', 475000, 'Hoa không đúng mô tả, héo ngay khi nhận.', '{"refund_status":"DONE"}', 'SEPAY-RF-0014', 'PROCESSED', 'user-customer-03'),
('ref-02', 'order-15', 150000, 'Một vài cành cát tường bị dập khi nhận hàng.', NULL, NULL, 'REQUESTED', 'user-customer-04'),
('ref-03', 'order-16', 100000, 'Muốn hoàn một phần vì giao trễ nửa ngày.', NULL, NULL, 'REJECTED', 'user-customer-01'),
('ref-04', 'order-17', 120000, 'Thiếu 2 cành ly so với mô tả.', NULL, NULL, 'APPROVED', 'user-customer-05');

-- E9. PRODUCT REVIEWS (user + sản phẩm là duy nhất)
INSERT INTO Product_Reviews (id, product_id, user_id, order_id, rating, comment, media, shop_reply, created_by) VALUES
('rev-02', 'prod-03', 'user-customer-02', 'order-02', 5, 'Hồng pastel rất đẹp, đúng ảnh. Giao đúng hẹn.', NULL, 'Cảm ơn bạn đã ủng hộ FPTU Smart Floral!', 'user-customer-02'),
('rev-03', 'prod-09', 'user-customer-02', 'order-02', 4, 'Baby trắng tươi, hơi ít so với mong đợi.', NULL, NULL, 'user-customer-02'),
('rev-04', 'prod-19', 'user-customer-03', 'order-03', 5, 'Hộp hồng nhung sang trọng, người nhận rất thích.', '["/images/product/product-01.jpg"]', 'Sài Gòn Floral rất vui vì bạn hài lòng!', 'user-customer-03'),
('rev-05', 'prod-18', 'user-customer-03', 'order-03', 4, 'Tulip tươi, gói đẹp.', NULL, NULL, 'user-customer-03'),
('rev-06', 'prod-24', 'user-customer-04', 'order-04', 5, 'Hướng dương to, tươi cả tuần.', NULL, 'Đà Lạt Garden cảm ơn bạn nhiều ạ.', 'user-customer-04'),
('rev-07', 'prod-26', 'user-customer-04', 'order-04', 3, 'Hoa dại ổn nhưng hơi rối, mong shop gói gọn hơn.', NULL, 'Shop xin ghi nhận và cải thiện cách gói ạ.', 'user-customer-04'),
('rev-08', 'prod-08', 'user-customer-05', 'order-17', 5, 'Ly thơm nở đẹp, thơm cả phòng.', '["/images/product/product-01.jpg"]', NULL, 'user-customer-05'),
('rev-09', 'prod-12', 'user-customer-05', 'order-17', 4, 'Màu hồng dịu, hợp tặng mẹ.', NULL, NULL, 'user-customer-05'),
('rev-10', 'prod-22', 'user-customer-04', 'order-15', 2, 'Cát tường bị dập vài cành khi giao.', NULL, 'Shop xin lỗi bạn, đã tiếp nhận yêu cầu hoàn tiền một phần.', 'user-customer-04'),
('rev-11', 'prod-29', 'user-customer-01', 'order-16', 5, 'Giỏ hoa nhẹ nhàng, ba mình rất thích.', '["/images/product/product-01.jpg"]', 'Cảm ơn bạn, chúc bác mau khỏe!', 'user-customer-01'),
('rev-12', 'prod-33', 'user-customer-02', 'order-20', 5, 'Shop thiết kế đúng ý, tulip rất tươi.', '["/images/product/product-01.jpg"]', 'Cảm ơn anh Nam đã tin tưởng ạ.', 'user-customer-02');

-- E10. FAVORITE PRODUCTS và FAVORITE SHOPS (theo dõi shop)
INSERT INTO Favorite_Products (user_id, product_id, created_by) VALUES
('user-customer-01', 'prod-13', 'user-customer-01'),
('user-customer-01', 'prod-24', 'user-customer-01'),
('user-customer-02', 'prod-03', 'user-customer-02'),
('user-customer-02', 'prod-19', 'user-customer-02'),
('user-customer-03', 'prod-10', 'user-customer-03'),
('user-customer-03', 'prod-23', 'user-customer-03'),
('user-customer-04', 'prod-12', 'user-customer-04'),
('user-customer-05', 'prod-17', 'user-customer-05'),
('user-customer-05', 'prod-20', 'user-customer-05'),
('user-customer-06', 'prod-04', 'user-customer-06');

INSERT INTO Favorite_Shops (user_id, shop_id, created_by) VALUES
('user-customer-01', 'shop-01', 'user-customer-01'),
('user-customer-01', 'shop-02', 'user-customer-01'),
('user-customer-02', 'shop-01', 'user-customer-02'),
('user-customer-02', 'shop-02', 'user-customer-02'),
('user-customer-03', 'shop-01', 'user-customer-03'),
('user-customer-03', 'shop-03', 'user-customer-03'),
('user-customer-04', 'shop-03', 'user-customer-04'),
('user-customer-05', 'shop-01', 'user-customer-05'),
('user-customer-05', 'shop-02', 'user-customer-05'),
('user-customer-05', 'shop-03', 'user-customer-05'),
('user-customer-06', 'shop-01', 'user-customer-06');

-- E11. BLOGS, LIKE/SAVE và BÌNH LUẬN
INSERT INTO Blogs (id, shop_id, title, content, thumbnail_url, images, status, created_date, created_by) VALUES
('blog-02', 'shop-01', 'Ý Nghĩa Các Loại Hoa Hồng', '<p>Hồng đỏ là tình yêu nồng nàn, hồng phấn là sự trân trọng, hồng vàng là tình bạn...</p>', '/images/product/product-01.jpg', '["/images/product/product-01.jpg", "/images/product/product-02.jpg"]', 'PUBLISHED', DATE_SUB(@now, INTERVAL 57600 MINUTE), 'user-shop-01'),
('blog-03', 'shop-01', 'Chọn Hoa Theo Từng Dịp Trong Năm', '<p>Sinh nhật nên chọn hoa tươi sáng, tốt nghiệp hợp hướng dương, chia buồn dùng cúc trắng...</p>', '/images/product/product-03.jpg', '["/images/product/product-03.jpg"]', 'PUBLISHED', DATE_SUB(@now, INTERVAL 40320 MINUTE), 'user-shop-01'),
('blog-04', 'shop-01', 'Bản Nháp: Xu Hướng Gói Hoa 2027', '<p>Đang soạn nội dung về giấy gói tối giản...</p>', NULL, '[]', 'DRAFT', DATE_SUB(@now, INTERVAL 2880 MINUTE), 'user-shop-01'),
('blog-05', 'shop-01', 'Bài Viết Đã Ẩn', '<p>Bài viết cũ bị shop ẩn.</p>', NULL, '[]', 'HIDDEN', DATE_SUB(@now, INTERVAL 172800 MINUTE), 'user-shop-01'),
('blog-06', 'shop-02', 'Bí Quyết Chọn Hoa Cưới Hợp Dáng Cô Dâu', '<p>Cô dâu nhỏ nhắn hợp bó tròn gọn, cô dâu cao hợp bó thác nước...</p>', '/images/product/product-04.jpg', '["/images/product/product-04.jpg", "/images/product/product-05.jpg"]', 'PUBLISHED', DATE_SUB(@now, INTERVAL 50400 MINUTE), 'user-shop-02'),
('blog-07', 'shop-02', 'Cách Bày Kệ Hoa Khai Trương Đón Tài Lộc', '<p>Đặt kệ hoa hai bên cửa, hướng về phía có nhiều người qua lại...</p>', '/images/product/product-02.jpg', '["/images/product/product-02.jpg"]', 'PUBLISHED', DATE_SUB(@now, INTERVAL 25920 MINUTE), 'user-shop-02'),
('blog-08', 'shop-03', 'Hành Trình Hoa Từ Vườn Đà Lạt Đến Tay Bạn', '<p>Hoa được cắt lúc 4 giờ sáng, đóng thùng lạnh và bay ngay trong ngày...</p>', '/images/product/product-03.jpg', '["/images/product/product-03.jpg", "/images/product/product-04.jpg"]', 'PUBLISHED', DATE_SUB(@now, INTERVAL 31680 MINUTE), 'user-shop-03'),
('blog-09', 'shop-03', '5 Cách Giữ Hoa Cát Tường Tươi Lâu', '<p>Cắt chéo cuống, thay nước mỗi ngày, tránh ánh nắng trực tiếp...</p>', '/images/product/product-01.jpg', '["/images/product/product-01.jpg"]', 'PUBLISHED', DATE_SUB(@now, INTERVAL 12960 MINUTE), 'user-shop-03');

INSERT INTO Blog_Interactions (user_id, blog_id, interaction_type, created_by) VALUES
('user-customer-01', 'blog-01', 'LIKE', 'user-customer-01'),
('user-customer-02', 'blog-01', 'LIKE', 'user-customer-02'),
('user-customer-02', 'blog-02', 'LIKE', 'user-customer-02'),
('user-customer-02', 'blog-02', 'SAVE', 'user-customer-02'),
('user-customer-03', 'blog-03', 'LIKE', 'user-customer-03'),
('user-customer-03', 'blog-06', 'SAVE', 'user-customer-03'),
('user-customer-04', 'blog-08', 'LIKE', 'user-customer-04'),
('user-customer-04', 'blog-09', 'LIKE', 'user-customer-04'),
('user-customer-04', 'blog-09', 'SAVE', 'user-customer-04'),
('user-customer-05', 'blog-07', 'LIKE', 'user-customer-05'),
('user-customer-05', 'blog-06', 'LIKE', 'user-customer-05'),
('user-customer-06', 'blog-02', 'SAVE', 'user-customer-06');

INSERT INTO Blog_Comments (id, blog_id, user_id, parent_comment_id, content, created_by) VALUES
('cmt-03', 'blog-02', 'user-customer-02', NULL, 'Mình mới biết hồng phấn mang nghĩa trân trọng, cảm ơn shop.', 'user-customer-02'),
('cmt-04', 'blog-02', 'user-shop-01', 'cmt-03', 'Dạ shop cảm ơn bạn đã đọc bài ạ.', 'user-shop-01'),
('cmt-05', 'blog-06', 'user-customer-03', NULL, 'Bài rất hay, mình sắp cưới nên đang tìm hiểu.', 'user-customer-03'),
('cmt-06', 'blog-06', 'user-shop-02', 'cmt-05', 'Chúc bạn có một đám cưới thật đẹp! Cần tư vấn cứ nhắn shop nhé.', 'user-shop-02'),
('cmt-07', 'blog-08', 'user-customer-04', NULL, 'Hoa Đà Lạt tươi thật sự, mình đã đặt thử.', 'user-customer-04'),
('cmt-08', 'blog-09', 'user-customer-05', NULL, 'Mẹo cắt chéo cuống hiệu quả lắm, hoa bền thêm mấy ngày.', 'user-customer-05'),
('cmt-09', 'blog-07', 'user-customer-01', NULL, 'Mình đang định mở cửa hàng, bài này rất đúng lúc.', 'user-customer-01');

-- E12. CUSTOMER OCCASIONS (dịp lặp hằng năm, dịp một lần, đã nhắc)
INSERT INTO Customer_Occasions (id, customer_id, title, occasion_date, reminder_sent, recurring_yearly, notify_days_before, notify_time, last_reminded_for, created_by) VALUES
('occ-02', 'user-customer-01', 'Sinh nhật mẹ', DATE_ADD(CURDATE(), INTERVAL 5 DAY), FALSE, TRUE, 7, '08:00:00', NULL, 'user-customer-01'),
('occ-03', 'user-customer-02', 'Kỷ niệm 5 năm ngày cưới', DATE_ADD(CURDATE(), INTERVAL 20 DAY), FALSE, TRUE, 10, '09:00:00', NULL, 'user-customer-02'),
('occ-04', 'user-customer-03', 'Sinh nhật bạn thân', DATE_SUB(CURDATE(), INTERVAL 3 DAY), TRUE, FALSE, 3, '07:30:00', DATE_SUB(CURDATE(), INTERVAL 3 DAY), 'user-customer-03'),
('occ-05', 'user-customer-04', 'Ngày của mẹ', DATE_ADD(CURDATE(), INTERVAL 60 DAY), FALSE, TRUE, 14, '08:00:00', NULL, 'user-customer-04'),
('occ-06', 'user-customer-05', 'Sinh nhật sếp', DATE_ADD(CURDATE(), INTERVAL 2 DAY), FALSE, FALSE, 2, '18:00:00', NULL, 'user-customer-05'),
('occ-07', 'user-customer-06', 'Lễ tốt nghiệp', DATE_ADD(CURDATE(), INTERVAL 45 DAY), FALSE, FALSE, 7, '08:00:00', NULL, 'user-customer-06');

-- E13. CHAT (phiên, tin nhắn, thẻ đính kèm sản phẩm và đơn đặt hoa riêng)
INSERT INTO Chat_Sessions (id, customer_id, shop_id, last_message_at, last_message_preview, customer_unread_count, shop_unread_count, created_by) VALUES
('session-02', 'user-customer-02', 'shop-01', DATE_SUB(@now, INTERVAL 60 MINUTE), 'Dạ shop gói xong sẽ báo bạn ngay ạ.', 1, 0, 'user-customer-02'),
('session-03', 'user-customer-03', 'shop-02', DATE_SUB(@now, INTERVAL 1440 MINUTE), 'Mình muốn hỏi thêm về bó hoa cầm tay.', 0, 2, 'user-customer-03'),
('session-04', 'user-customer-04', 'shop-03', DATE_SUB(@now, INTERVAL 4320 MINUTE), 'Cảm ơn shop nhé!', 0, 0, 'user-customer-04'),
('session-05', 'user-customer-01', 'shop-02', DATE_SUB(@now, INTERVAL 2880 MINUTE), 'Shop ơi đơn của mình đã lấy hàng chưa ạ?', 0, 1, 'user-customer-01');

INSERT INTO Chat_Messages (id, session_id, sender_id, sender_type, message, media, context_type, context_id, context_snapshot, is_read, created_by) VALUES
('msg-03', 'session-02', 'user-customer-02', 'CUSTOMER', 'Shop ơi bó hồng pastel này còn không ạ?', NULL, 'PRODUCT', 'prod-03', '{"name": "Bó Hồng Pastel Ngọt Ngào", "price": 420000, "image": "/images/product/product-01.jpg", "status": "ACTIVE"}', TRUE, 'user-customer-02'),
('msg-04', 'session-02', 'user-shop-01', 'SHOP', 'Dạ còn ạ, bạn cần giao khi nào?', NULL, 'NONE', NULL, NULL, TRUE, 'user-shop-01'),
('msg-05', 'session-02', 'user-customer-02', 'CUSTOMER', 'Chiều nay giao giúp mình nhé.', NULL, 'NONE', NULL, NULL, TRUE, 'user-customer-02'),
('msg-06', 'session-02', 'user-shop-01', 'SHOP', 'Dạ shop gói xong sẽ báo bạn ngay ạ.', NULL, 'NONE', NULL, NULL, FALSE, 'user-shop-01'),
('msg-07', 'session-03', 'user-customer-03', 'CUSTOMER', 'Mình muốn hỏi thêm về bó hoa cầm tay.', NULL, 'PRODUCT', 'prod-13', '{"name": "Hoa Cầm Tay Cô Dâu Trắng", "price": 750000, "image": "/images/product/product-02.jpg", "status": "ACTIVE"}', FALSE, 'user-customer-03'),
('msg-08', 'session-03', 'user-customer-03', 'CUSTOMER', 'Có thể đổi sang tông hồng phấn được không ạ?', '["/images/product/product-04.jpg"]', 'NONE', NULL, NULL, FALSE, 'user-customer-03'),
('msg-09', 'session-04', 'user-customer-04', 'CUSTOMER', 'Hoa giao tới rồi, đẹp lắm shop ơi.', NULL, 'NONE', NULL, NULL, TRUE, 'user-customer-04'),
('msg-10', 'session-04', 'user-shop-03', 'SHOP', 'Cảm ơn bạn nhiều, chúc bạn một ngày vui!', NULL, 'NONE', NULL, NULL, TRUE, 'user-shop-03'),
('msg-11', 'session-01', 'user-customer-01', 'CUSTOMER', 'Mình đã gửi yêu cầu đặt hoa riêng cho shop.', NULL, 'CUSTOM_ORDER', 'req-02', '{"name": "Yêu cầu hoa riêng req-02", "price": 1200000, "status": "ACCEPTED"}', FALSE, 'user-customer-01'),
('msg-12', 'session-05', 'user-customer-01', 'CUSTOMER', 'Shop ơi đơn của mình đã lấy hàng chưa ạ?', NULL, 'NONE', NULL, NULL, FALSE, 'user-customer-01');

-- E14. STAFF, ỨNG TUYỂN, LỜI MỜI, ĐƠN MỞ SHOP, OTP, PHIÊN ĐĂNG NHẬP
INSERT INTO Shop_Staff (id, shop_id, user_id, active, created_by) VALUES
('staff-02', 'shop-02', 'user-staff-02', TRUE, 'user-shop-02'),
('staff-03', 'shop-01', 'user-staff-03', FALSE, 'user-shop-01');

INSERT INTO Staff_Applications (id, shop_id, user_id, full_name, phone, introduction, status, submitted_at) VALUES
('staff-app-02', 'shop-01', 'user-customer-04', 'Lê Quốc Anh', '0911000004', 'Mình thích cắm hoa, có thể làm ca tối và cuối tuần.', 'PENDING', DATE_SUB(@now, INTERVAL 1440 MINUTE)),
('staff-app-03', 'shop-02', 'user-customer-05', 'Hoàng Mai Linh', '0911000005', 'Đã từng làm thu ngân tại cửa hàng hoa 6 tháng.', 'REJECTED', DATE_SUB(@now, INTERVAL 17280 MINUTE)),
('staff-app-04', 'shop-02', 'user-staff-02', 'Ngô Thị Mai', '0908666777', 'Em có kinh nghiệm cắm hoa sự kiện.', 'APPROVED', DATE_SUB(@now, INTERVAL 72000 MINUTE)),
('staff-app-05', 'shop-03', 'user-customer-06', 'Đinh Gia Huy', '0911000006', 'Sinh viên muốn làm thêm giao hoa.', 'PENDING', DATE_SUB(@now, INTERVAL 300 MINUTE));

INSERT INTO Staff_Invitations (id, shop_id, email, token_hash, issued_at, expires_at, window_start, send_count, status) VALUES
('staff-inv-02', 'shop-02', 'staff.hoa2@gmail.com', 'a1b2c3d4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f60718293a4b5c6d7e8f90', DATE_SUB(@now, INTERVAL 66240 MINUTE), DATE_SUB(@now, INTERVAL 56160 MINUTE), DATE_SUB(@now, INTERVAL 66240 MINUTE), 1, 'ACCEPTED'),
('staff-inv-03', 'shop-01', 'staff.old@gmail.com', 'b1b2c3d4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f60718293a4b5c6d7e8f90', DATE_SUB(@now, INTERVAL 43200 MINUTE), DATE_SUB(@now, INTERVAL 33120 MINUTE), DATE_SUB(@now, INTERVAL 43200 MINUTE), 2, 'EXPIRED'),
('staff-inv-04', 'shop-03', 'staff.cancel@gmail.com', 'c1b2c3d4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f60718293a4b5c6d7e8f90', DATE_SUB(@now, INTERVAL 7200 MINUTE), DATE_ADD(@now, INTERVAL 2880 MINUTE), DATE_SUB(@now, INTERVAL 7200 MINUTE), 1, 'CANCELLED'),
('staff-inv-05', 'shop-03', 'staff.new@gmail.com', 'd1b2c3d4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f60718293a4b5c6d7e8f90', DATE_SUB(@now, INTERVAL 1440 MINUTE), DATE_ADD(@now, INTERVAL 8640 MINUTE), DATE_SUB(@now, INTERVAL 1440 MINUTE), 1, 'PENDING');

INSERT INTO Manager_Applications (id, user_id, full_name, phone, shop_name, description, address_line, city, district, ward, status, review_note, submitted_at, reviewed_at, reviewed_by, shop_id) VALUES
('mgr-app-02', 'user-shop-02', 'Lê Minh Châu', '0903111222', 'Sài Gòn Floral Boutique', 'Boutique hoa cưới và sự kiện tại Quận 1.', '123 Nguyễn Huệ', 'TP. Hồ Chí Minh', 'Quận 1', 'Bến Nghé', 'APPROVED', 'Hồ sơ đầy đủ, đã duyệt.', DATE_SUB(@now, INTERVAL 180000 MINUTE), DATE_SUB(@now, INTERVAL 174240 MINUTE), 'user-admin-01', 'shop-02'),
('mgr-app-03', 'user-shop-03', 'Võ Thanh Hằng', '0904222333', 'Đà Lạt Garden', 'Hoa tươi từ vườn Đà Lạt.', '45 Hoàng Văn Thụ', 'Lâm Đồng', 'Đà Lạt', 'Phường 9', 'APPROVED', 'Đã duyệt.', DATE_SUB(@now, INTERVAL 151200 MINUTE), DATE_SUB(@now, INTERVAL 145440 MINUTE), 'user-admin-01', 'shop-03'),
('mgr-app-04', 'user-shop-04', 'Đặng Quốc Huy', '0905333444', 'Blossom Studio', 'Studio hoa mới mở tại Đà Nẵng.', '8 Lê Lợi', 'Đà Nẵng', 'Hải Châu', 'Hải Châu 1', 'PENDING', NULL, DATE_SUB(@now, INTERVAL 7200 MINUTE), NULL, NULL, NULL),
('mgr-app-05', 'user-customer-06', 'Đinh Gia Huy', '0911000006', 'Hoa Test', 'Đăng ký thử nghiệm.', '5 Nguyễn Trãi', 'Hà Nội', 'Thanh Xuân', 'Thanh Xuân Trung', 'REJECTED', 'Thiếu giấy phép kinh doanh, vui lòng bổ sung.', DATE_SUB(@now, INTERVAL 14400 MINUTE), DATE_SUB(@now, INTERVAL 11520 MINUTE), 'user-admin-01', NULL);

INSERT INTO Otp_Challenges (id, user_id, purpose, code_hash, issued_at, expires_at, used_at, window_start, failed_attempts, send_count) VALUES
('otp-02', 'user-customer-02', 'RESET_PASSWORD', '$2a$12$z2frH4jxzHPjasvwidmC4.GobzOtpx/Q1fhJyXokmGjsDypKXUPf2', DATE_SUB(@now, INTERVAL 3 MINUTE), DATE_ADD(@now, INTERVAL 0 MINUTE), NULL, DATE_SUB(@now, INTERVAL 3 MINUTE), 0, 1),
('otp-03', 'user-customer-08', 'REGISTER', '$2a$12$z2frH4jxzHPjasvwidmC4.GobzOtpx/Q1fhJyXokmGjsDypKXUPf2', DATE_SUB(@now, INTERVAL 8 MINUTE), DATE_SUB(@now, INTERVAL -2 MINUTE), NULL, DATE_SUB(@now, INTERVAL 8 MINUTE), 2, 3),
('otp-04', 'user-customer-03', 'RESET_PASSWORD', '$2a$12$z2frH4jxzHPjasvwidmC4.GobzOtpx/Q1fhJyXokmGjsDypKXUPf2', DATE_SUB(@now, INTERVAL 5760 MINUTE), DATE_SUB(@now, INTERVAL 5755 MINUTE), DATE_SUB(@now, INTERVAL 5759 MINUTE), DATE_SUB(@now, INTERVAL 5760 MINUTE), 0, 1);

INSERT INTO Pending_Registrations (email, full_name, password_hash, otp_hash, shop_name, shop_description, issued_at, expires_at, window_start, failed_attempts, send_count) VALUES
('khach.moi@gmail.com', 'Khách Mới Đăng Ký', '$2a$12$z2frH4jxzHPjasvwidmC4.GobzOtpx/Q1fhJyXokmGjsDypKXUPf2', '$2a$12$z2frH4jxzHPjasvwidmC4.GobzOtpx/Q1fhJyXokmGjsDypKXUPf2', NULL, NULL, DATE_SUB(@now, INTERVAL 4 MINUTE), DATE_ADD(@now, INTERVAL 0 MINUTE), DATE_SUB(@now, INTERVAL 4 MINUTE), 0, 1),
('chushop.moi@gmail.com', 'Chủ Shop Mới', '$2a$12$z2frH4jxzHPjasvwidmC4.GobzOtpx/Q1fhJyXokmGjsDypKXUPf2', '$2a$12$z2frH4jxzHPjasvwidmC4.GobzOtpx/Q1fhJyXokmGjsDypKXUPf2', 'Hoa Mới Mở', 'Tiệm hoa nhỏ chuyên hoa sinh nhật.', DATE_SUB(@now, INTERVAL 9 MINUTE), DATE_SUB(@now, INTERVAL 4 MINUTE), DATE_SUB(@now, INTERVAL 9 MINUTE), 1, 2);

INSERT INTO Auth_Sessions (id, user_id, refresh_hash, expires_at, revoked) VALUES
('sess-02', 'user-admin-01', '1111111111111111111111111111111111111111111111111111111111111111', DATE_ADD(@now, INTERVAL 20160 MINUTE), FALSE),
('sess-03', 'user-shop-01', '2222222222222222222222222222222222222222222222222222222222222222', DATE_ADD(@now, INTERVAL 14400 MINUTE), FALSE),
('sess-04', 'user-shop-02', '3333333333333333333333333333333333333333333333333333333333333333', DATE_ADD(@now, INTERVAL 10080 MINUTE), FALSE),
('sess-05', 'user-customer-02', '4444444444444444444444444444444444444444444444444444444444444444', DATE_ADD(@now, INTERVAL 4320 MINUTE), FALSE),
('sess-06', 'user-customer-03', '5555555555555555555555555555555555555555555555555555555555555555', DATE_SUB(@now, INTERVAL 2880 MINUTE), FALSE),
('sess-07', 'user-customer-04', '6666666666666666666666666666666666666666666666666666666666666666', DATE_ADD(@now, INTERVAL 7200 MINUTE), TRUE);

-- E15. NOTIFICATIONS và HÀNG ĐỢI EMAIL SẢN PHẨM MỚI
INSERT INTO Notifications (id, user_id, title, message, link, is_read, created_date) VALUES
('noti-01', 'user-customer-02', 'Đơn hàng đang được chuẩn bị', 'Shop đã nhận đơn order-07 và đang gói hoa cho bạn.', '/orders/order-07', FALSE, DATE_SUB(@now, INTERVAL 180 MINUTE)),
('noti-02', 'user-customer-05', 'Đơn hàng đang trên đường giao', 'Đơn order-05 đã được bàn giao cho GHN.', '/orders/order-05', FALSE, DATE_SUB(@now, INTERVAL 1440 MINUTE)),
('noti-03', 'user-customer-02', 'Giao hàng thành công', 'Đơn order-02 đã giao thành công. Hãy để lại đánh giá nhé.', '/orders/order-02', TRUE, DATE_SUB(@now, INTERVAL 57600 MINUTE)),
('noti-04', 'user-customer-03', 'Hoàn tiền thành công', 'Shop đã hoàn 475.000đ cho đơn order-14.', '/orders/order-14', TRUE, DATE_SUB(@now, INTERVAL 27360 MINUTE)),
('noti-05', 'user-customer-04', 'Yêu cầu hoàn tiền đã được gửi', 'Shop sẽ phản hồi yêu cầu hoàn tiền của đơn order-15 sớm.', '/orders/order-15', FALSE, DATE_SUB(@now, INTERVAL 24480 MINUTE)),
('noti-06', 'user-customer-01', 'Báo giá hoa riêng', 'Shop đã báo giá yêu cầu req-02, vui lòng xác nhận.', '/custom-orders', FALSE, DATE_SUB(@now, INTERVAL 11520 MINUTE)),
('noti-07', 'user-customer-01', 'Nhắc dịp đặc biệt', 'Sinh nhật mẹ còn 5 ngày nữa, đặt hoa ngay?', '/occasions', FALSE, DATE_SUB(@now, INTERVAL 60 MINUTE)),
('noti-08', 'user-customer-03', 'Dịp đặc biệt đã qua', 'Sinh nhật bạn thân vừa diễn ra 3 ngày trước.', '/occasions', TRUE, DATE_SUB(@now, INTERVAL 4320 MINUTE)),
('noti-09', 'user-shop-01', 'Có đơn hàng mới', 'Khách Lê Quốc Anh vừa đặt đơn order-09.', '/shop-admin/orders', FALSE, DATE_SUB(@now, INTERVAL 0 MINUTE)),
('noti-10', 'user-shop-01', 'Yêu cầu hoàn tiền mới', 'Đơn order-17 có yêu cầu hoàn tiền một phần.', '/shop-admin/orders', TRUE, DATE_SUB(@now, INTERVAL 7200 MINUTE)),
('noti-11', 'user-shop-02', 'Có đơn hàng mới', 'Khách Hoàng Mai Linh vừa đặt đơn order-10.', '/shop-admin/orders', FALSE, DATE_SUB(@now, INTERVAL 0 MINUTE)),
('noti-12', 'user-shop-02', 'Ứng tuyển nhân viên', 'Hoàng Mai Linh ứng tuyển làm nhân viên.', '/shop-admin/staff', TRUE, DATE_SUB(@now, INTERVAL 17280 MINUTE)),
('noti-13', 'user-shop-03', 'Gói Pro sắp hết hạn', 'Gói Pro của Đà Lạt Garden hết hạn sau 2 ngày.', '/shop-admin/subscription', FALSE, DATE_SUB(@now, INTERVAL 120 MINUTE)),
('noti-14', 'user-admin-01', 'Đơn mở shop mới', 'Blossom Studio vừa gửi đơn mở shop, cần duyệt.', '/admin/shops', FALSE, DATE_SUB(@now, INTERVAL 7200 MINUTE)),
('noti-15', 'user-admin-01', 'Sản phẩm bị báo cáo', 'Một sản phẩm của Hoa Xưa bị khách báo cáo.', '/admin/categories', TRUE, DATE_SUB(@now, INTERVAL 28800 MINUTE)),
('noti-16', 'user-shop-04', 'Đơn mở shop đang chờ duyệt', 'Admin sẽ xem xét đơn của bạn trong 1 đến 2 ngày.', '/shop-admin', TRUE, DATE_SUB(@now, INTERVAL 7200 MINUTE));

INSERT INTO Shop_Product_Mails (id, product_id, shop_id, user_id, status, attempts, next_attempt_at, created_at, sent_at) VALUES
('mail-01', 'prod-12', 'shop-01', 'user-customer-02', 'SENT', 1, DATE_SUB(@now, INTERVAL 43200 MINUTE), DATE_SUB(@now, INTERVAL 43200 MINUTE), DATE_SUB(@now, INTERVAL 43200 MINUTE)),
('mail-02', 'prod-12', 'shop-01', 'user-customer-03', 'SENT', 1, DATE_SUB(@now, INTERVAL 43200 MINUTE), DATE_SUB(@now, INTERVAL 43200 MINUTE), DATE_SUB(@now, INTERVAL 43200 MINUTE)),
('mail-03', 'prod-12', 'shop-01', 'user-customer-05', 'FAILED', 5, DATE_SUB(@now, INTERVAL 41760 MINUTE), DATE_SUB(@now, INTERVAL 43200 MINUTE), NULL),
('mail-04', 'prod-30', 'shop-03', 'user-customer-04', 'SENT', 1, DATE_SUB(@now, INTERVAL 79200 MINUTE), DATE_SUB(@now, INTERVAL 79200 MINUTE), DATE_SUB(@now, INTERVAL 79200 MINUTE)),
('mail-05', 'prod-30', 'shop-03', 'user-customer-05', 'PENDING', 2, DATE_ADD(@now, INTERVAL 60 MINUTE), DATE_SUB(@now, INTERVAL 120 MINUTE), NULL),
('mail-06', 'prod-20', 'shop-02', 'user-customer-01', 'PENDING', 0, DATE_ADD(@now, INTERVAL 0 MINUTE), DATE_SUB(@now, INTERVAL 5 MINUTE), NULL),
('mail-07', 'prod-20', 'shop-02', 'user-customer-02', 'CANCELLED', 0, DATE_SUB(@now, INTERVAL 100800 MINUTE), DATE_SUB(@now, INTERVAL 108000 MINUTE), NULL);

-- E16. GÓI PRO: đăng ký, thanh toán, lịch sử kích hoạt, sản phẩm nổi bật, nhắc hạn
INSERT INTO Shop_Subscriptions (id, shop_id, plan_id, started_at, ends_at) VALUES
('sub-01', 'shop-01', 'PRO12', DATE_SUB(@now, INTERVAL 86400 MINUTE), DATE_ADD(@now, INTERVAL 439200 MINUTE)),
('sub-02', 'shop-02', 'PRO6', DATE_SUB(@now, INTERVAL 43200 MINUTE), DATE_ADD(@now, INTERVAL 216000 MINUTE)),
('sub-03', 'shop-03', 'PRO1', DATE_SUB(@now, INTERVAL 40320 MINUTE), DATE_ADD(@now, INTERVAL 2880 MINUTE));

INSERT INTO Shop_Plan_Payments (id, shop_id, plan_id, duration_months, amount, invoice_number, status, created_at, confirmed_at, last_checked_at, transaction_no) VALUES
('planpay-01', 'shop-01', 'PRO12', 12, 596000, 'SUB-DEMO-0001', 'SUCCESS', DATE_SUB(@now, INTERVAL 86400 MINUTE), DATE_SUB(@now, INTERVAL 86400 MINUTE), DATE_SUB(@now, INTERVAL 86400 MINUTE), 'SEPAY-SUB-0001'),
('planpay-02', 'shop-02', 'PRO6', 6, 298000, 'SUB-DEMO-0002', 'SUCCESS', DATE_SUB(@now, INTERVAL 43200 MINUTE), DATE_SUB(@now, INTERVAL 43200 MINUTE), DATE_SUB(@now, INTERVAL 43200 MINUTE), 'SEPAY-SUB-0002'),
('planpay-03', 'shop-03', 'PRO1', 1, 59000, 'SUB-DEMO-0003', 'SUCCESS', DATE_SUB(@now, INTERVAL 40320 MINUTE), DATE_SUB(@now, INTERVAL 40320 MINUTE), DATE_SUB(@now, INTERVAL 40320 MINUTE), 'SEPAY-SUB-0003'),
('planpay-04', 'shop-03', 'PRO3', 3, 149000, 'SUB-DEMO-0004', 'PENDING', DATE_SUB(@now, INTERVAL 60 MINUTE), NULL, DATE_SUB(@now, INTERVAL 1 MINUTE), NULL),
('planpay-05', 'shop-02', 'PRO1', 1, 59000, 'SUB-DEMO-0005', 'FAILED', DATE_SUB(@now, INTERVAL 64800 MINUTE), NULL, DATE_SUB(@now, INTERVAL 64800 MINUTE), NULL),
('planpay-06', 'shop-01', 'PRO3', 3, 149000, 'SUB-DEMO-0006', 'REVIEW', DATE_SUB(@now, INTERVAL 2880 MINUTE), NULL, DATE_SUB(@now, INTERVAL 1440 MINUTE), NULL);

INSERT INTO Shop_Pro_Activations (id, shop_id, plan_id, duration_months, source, payment_id, actor_id, note, starts_at, ends_at, created_at) VALUES
('act-01', 'shop-01', 'PRO12', 12, 'SEPAY', 'planpay-01', NULL, 'Thanh toán SePay thành công', DATE_SUB(@now, INTERVAL 86400 MINUTE), DATE_ADD(@now, INTERVAL 439200 MINUTE), DATE_SUB(@now, INTERVAL 86400 MINUTE)),
('act-02', 'shop-02', 'PRO6', 6, 'SEPAY', 'planpay-02', NULL, 'Thanh toán SePay thành công', DATE_SUB(@now, INTERVAL 43200 MINUTE), DATE_ADD(@now, INTERVAL 216000 MINUTE), DATE_SUB(@now, INTERVAL 43200 MINUTE)),
('act-03', 'shop-03', 'PRO1', 1, 'SEPAY', 'planpay-03', NULL, 'Thanh toán SePay thành công', DATE_SUB(@now, INTERVAL 40320 MINUTE), DATE_ADD(@now, INTERVAL 2880 MINUTE), DATE_SUB(@now, INTERVAL 40320 MINUTE)),
('act-04', 'shop-02', 'PRO1', 1, 'ADMIN_GRANT', NULL, 'user-admin-01', 'Tặng 1 tháng nhân dịp khai trương', DATE_SUB(@now, INTERVAL 108000 MINUTE), DATE_SUB(@now, INTERVAL 64800 MINUTE), DATE_SUB(@now, INTERVAL 108000 MINUTE));

INSERT INTO Featured_Products (product_id, shop_id, featured_at) VALUES
('prod-03', 'shop-01', DATE_SUB(@now, INTERVAL 43200 MINUTE)),
('prod-04', 'shop-01', DATE_SUB(@now, INTERVAL 43200 MINUTE)),
('prod-10', 'shop-01', DATE_SUB(@now, INTERVAL 40320 MINUTE)),
('prod-12', 'shop-01', DATE_SUB(@now, INTERVAL 28800 MINUTE)),
('prod-13', 'shop-02', DATE_SUB(@now, INTERVAL 36000 MINUTE)),
('prod-14', 'shop-02', DATE_SUB(@now, INTERVAL 36000 MINUTE)),
('prod-15', 'shop-02', DATE_SUB(@now, INTERVAL 28800 MINUTE)),
('prod-23', 'shop-03', DATE_SUB(@now, INTERVAL 21600 MINUTE));

INSERT INTO Subscription_Reminders (id, subscription_id, expected_end, reminder_date, hour_slot, mail_status, attempts, next_attempt_at)
SELECT 'reminder-01', id, ends_at, CURDATE(), 9, 'SENT', 1, @now FROM Shop_Subscriptions WHERE id = 'sub-03';
INSERT INTO Subscription_Reminders (id, subscription_id, expected_end, reminder_date, hour_slot, mail_status, attempts, next_attempt_at)
SELECT 'reminder-02', id, ends_at, CURDATE(), 19, 'PENDING', 0, @now FROM Shop_Subscriptions WHERE id = 'sub-03';

