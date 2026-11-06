-- Run against the application's database before starting the updated backend.
-- Re-running this migration preserves labels, sort order and disabled flags already edited.
SET NAMES utf8mb4;
CREATE TABLE IF NOT EXISTS Custom_Flower_Options (
    id VARCHAR(36) PRIMARY KEY,
    option_group VARCHAR(20) NOT NULL,
    label VARCHAR(100) NOT NULL,
    disabled BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_custom_flower_option UNIQUE (option_group, label)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO Custom_Flower_Options (id, option_group, label, disabled, sort_order) VALUES
('flower-rose', 'FLOWER', 'Hoa hồng', FALSE, 10),
('flower-baby', 'FLOWER', 'Hoa baby', FALSE, 20),
('flower-tulip', 'FLOWER', 'Hoa tulip', FALSE, 30),
('flower-sunflower', 'FLOWER', 'Hoa hướng dương', FALSE, 40),
('flower-hydrangea', 'FLOWER', 'Hoa cẩm tú cầu', FALSE, 50),
('flower-carnation', 'FLOWER', 'Hoa cẩm chướng', FALSE, 60),
('flower-orchid', 'FLOWER', 'Hoa lan', FALSE, 70),
('flower-lily', 'FLOWER', 'Hoa ly', FALSE, 80),
('flower-chrysanthemum', 'FLOWER', 'Hoa cúc', FALSE, 90),
('flower-lotus', 'FLOWER', 'Hoa sen', FALSE, 100),
('flower-peony', 'FLOWER', 'Hoa mẫu đơn', FALSE, 110),
('flower-gerbera', 'FLOWER', 'Hoa đồng tiền', FALSE, 120),
('flower-mixed', 'FLOWER', 'Phối nhiều loại hoa', FALSE, 130),
('flower-other', 'FLOWER', 'Khác', FALSE, 140),
('occasion-birthday', 'OCCASION', 'Sinh nhật', FALSE, 10),
('occasion-graduation', 'OCCASION', 'Tốt nghiệp', FALSE, 20),
('occasion-anniversary', 'OCCASION', 'Kỷ niệm', FALSE, 30),
('occasion-love', 'OCCASION', 'Tình yêu', FALSE, 40),
('occasion-opening', 'OCCASION', 'Khai trương', FALSE, 50),
('occasion-congratulations', 'OCCASION', 'Chúc mừng', FALSE, 60),
('occasion-thanks', 'OCCASION', 'Cảm ơn', FALSE, 70),
('occasion-wedding', 'OCCASION', 'Ngày cưới', FALSE, 80),
('occasion-condolence', 'OCCASION', 'Chia buồn', FALSE, 90),
('occasion-other', 'OCCASION', 'Khác', FALSE, 100),
('wrapping-kraft', 'WRAPPING', 'Giấy kraft', FALSE, 10),
('wrapping-tissue', 'WRAPPING', 'Giấy lụa', FALSE, 20),
('wrapping-cellophane', 'WRAPPING', 'Giấy bóng kính', FALSE, 30),
('wrapping-korean', 'WRAPPING', 'Giấy gói Hàn Quốc', FALSE, 40),
('accessory-ribbon', 'ACCESSORY', 'Ruy băng', FALSE, 10),
('accessory-bow', 'ACCESSORY', 'Nơ', FALSE, 20),
('accessory-card', 'ACCESSORY', 'Thiệp chúc mừng', FALSE, 30),
('accessory-led', 'ACCESSORY', 'Đèn LED', FALSE, 40),
('accessory-teddy', 'ACCESSORY', 'Gấu bông', FALSE, 50)
ON DUPLICATE KEY UPDATE id = Custom_Flower_Options.id;
