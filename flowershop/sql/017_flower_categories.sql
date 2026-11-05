-- Flower categories modelled on hoayeuthuong.com: occasion, flower type, arrangement style.
-- Skips any name that already exists, so it is safe to run more than once.
INSERT INTO Categories (id, name, description, status)
SELECT UUID(), v.name, v.description, 'ACTIVE'
FROM (
    SELECT 'Hoa Sinh Nhật' AS name, 'Bó hoa, giỏ hoa chúc mừng sinh nhật' AS description
    UNION ALL SELECT 'Hoa Chúc Mừng', 'Hoa chúc mừng thăng chức, thành công, sự kiện vui'
    UNION ALL SELECT 'Hoa Chia Buồn', 'Vòng hoa, lẵng hoa viếng và chia buồn'
    UNION ALL SELECT 'Hoa Chúc Sức Khỏe', 'Hoa thăm hỏi, chúc người thân mau khỏe'
    UNION ALL SELECT 'Hoa Cảm Ơn', 'Hoa gửi lời cảm ơn thầy cô, đối tác, người thân'
    UNION ALL SELECT 'Hoa Mừng Tốt Nghiệp', 'Hoa tặng lễ tốt nghiệp, bảo vệ luận văn'
    UNION ALL SELECT 'Hoa Cưới', 'Hoa cầm tay cô dâu, hoa trang trí tiệc cưới'
    UNION ALL SELECT 'Hoa Ngày 20/10', 'Hoa tặng Ngày Phụ nữ Việt Nam'
    UNION ALL SELECT 'Hoa Ngày 8/3', 'Hoa tặng Ngày Quốc tế Phụ nữ'
    UNION ALL SELECT 'Hoa Ngày Nhà Giáo', 'Hoa tri ân thầy cô ngày 20/11'
    UNION ALL SELECT 'Hoa Valentine', 'Hoa tặng người yêu ngày 14/2'
    UNION ALL SELECT 'Hoa Tết', 'Hoa trưng bàn, hoa chúc Tết đầu năm'
    UNION ALL SELECT 'Hoa Giáng Sinh', 'Hoa và cành trang trí mùa Noel'
    UNION ALL SELECT 'Hoa Hồng', 'Hồng đỏ, hồng pastel, hồng Ecuador'
    UNION ALL SELECT 'Hoa Hướng Dương', 'Bó và giỏ hướng dương rực rỡ'
    UNION ALL SELECT 'Hoa Đồng Tiền', 'Đồng tiền nhiều màu, bền hoa'
    UNION ALL SELECT 'Hoa Ly', 'Ly thơm, ly Tycoon, ly Sorbonne'
    UNION ALL SELECT 'Hoa Cúc', 'Cúc họa mi, cúc mẫu đơn, cúc pingpong'
    UNION ALL SELECT 'Hoa Cẩm Chướng', 'Cẩm chướng đơn và chùm'
    UNION ALL SELECT 'Hoa Cát Tường', 'Cát tường tím, trắng, hồng'
    UNION ALL SELECT 'Baby Flower', 'Hoa baby trắng, baby nhuộm màu'
    UNION ALL SELECT 'Lan Hồ Điệp', 'Chậu lan hồ điệp để bàn, làm quà biếu'
    UNION ALL SELECT 'Bó Hoa Tươi', 'Hoa bó gói giấy, cầm tay'
    UNION ALL SELECT 'Giỏ Hoa Tươi', 'Hoa cắm giỏ mây, giỏ gỗ'
    UNION ALL SELECT 'Hộp Hoa Tươi', 'Hoa cắm hộp tròn, hộp vuông, hộp mica'
    UNION ALL SELECT 'Bình Hoa Tươi', 'Hoa cắm sẵn trong bình thủy tinh, bình gốm'
    UNION ALL SELECT 'Lẵng Hoa', 'Lẵng hoa chúc mừng và chia buồn cỡ lớn'
    UNION ALL SELECT 'Kệ Hoa', 'Kệ hoa khai trương, hội nghị, sự kiện'
) AS v
WHERE NOT EXISTS (SELECT 1 FROM Categories c WHERE c.name = v.name);
