
CREATE TABLE Users (
    id NVARCHAR(36) PRIMARY KEY,
    email NVARCHAR(255) UNIQUE NOT NULL,
    password_hash NVARCHAR(255),
    google_id NVARCHAR(255) UNIQUE,
    full_name NVARCHAR(100),
    phone NVARCHAR(20),
    `role` ENUM('ADMIN', 'SHOP', 'CUSTOMER', 'SHOP_STAFF', 'DELIVERY') NOT NULL,
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
    user_id NVARCHAR(36), 
    shop_id NVARCHAR(36),
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
    components JSON, 
    shelf_life_days INT, 
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
    image_url NVARCHAR(500) NOT NULL, 
    is_primary BOOLEAN DEFAULT FALSE, 
    display_order INT DEFAULT 0, 
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by NVARCHAR(36),
    FOREIGN KEY (product_id) REFERENCES Products(id) ON DELETE CASCADE
);

CREATE TABLE Product_Videos (
    id NVARCHAR(36) PRIMARY KEY,
    product_id NVARCHAR(36) NOT NULL,
    video_url NVARCHAR(500) NOT NULL, 
    title NVARCHAR(255),
    description TEXT,
    display_order INT DEFAULT 0, 
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by NVARCHAR(36),
    FOREIGN KEY (product_id) REFERENCES Products(id) ON DELETE CASCADE
);


CREATE TABLE Coupons (
    id NVARCHAR(36) PRIMARY KEY,
    shop_id NVARCHAR(36),
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
    custom_materials JSON,
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
    media JSON, 
    is_read BOOLEAN DEFAULT FALSE,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by NVARCHAR(36),
    FOREIGN KEY (session_id) REFERENCES Chat_Sessions(id),
    FOREIGN KEY (sender_id) REFERENCES Users(id)
);


CREATE TABLE Custom_Order_Requests (
    id NVARCHAR(36) PRIMARY KEY,
    customer_id NVARCHAR(36) NOT NULL,
    shop_id NVARCHAR(36) NOT NULL,
    budget DECIMAL(12, 2) NOT NULL,
    desired_components JSON,
    description TEXT,
    status ENUM('REQUESTED', 'NEGOTIATING', 'ACCEPTED', 'REJECTED') DEFAULT 'REQUESTED',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by NVARCHAR(36),
    FOREIGN KEY (customer_id) REFERENCES Users(id),
    FOREIGN KEY (shop_id) REFERENCES Shops(id)
);


CREATE TABLE Payments (
    id NVARCHAR(36) PRIMARY KEY,
    order_id NVARCHAR(36) NOT NULL,
    payment_type ENUM('DEPOSIT', 'FINAL', 'FULL') NOT NULL,
    payment_method ENUM('ONLINE', 'COD') NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    gateway_transaction_no NVARCHAR(100),
    invoice_number NVARCHAR(100) UNIQUE,
    gateway_response JSON, 
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
    gateway_refund_id NVARCHAR(100), 
    gateway_response JSON,
    status ENUM('REQUESTED', 'APPROVED', 'PROCESSED', 'REJECTED') DEFAULT 'REQUESTED',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(36),
    last_modify_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_modify_by NVARCHAR(36),
    FOREIGN KEY (order_id) REFERENCES Orders(id)
);


CREATE TABLE Product_Reviews (
    id NVARCHAR(36) PRIMARY KEY,
    product_id NVARCHAR(36) NOT NULL,
    user_id NVARCHAR(36) NOT NULL,
    order_id NVARCHAR(36) NULL, 
    rating INT CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    media JSON,
    shop_reply TEXT, 
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


CREATE TABLE Blogs (
    id NVARCHAR(36) PRIMARY KEY,
    shop_id NVARCHAR(36) NOT NULL,
    title NVARCHAR(255) NOT NULL,
    content LONGTEXT NOT NULL, 
    thumbnail_url NVARCHAR(500),
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
    image_url NVARCHAR(500) NOT NULL, 
    display_order INT DEFAULT 0,
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
    parent_comment_id NVARCHAR(36),
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
