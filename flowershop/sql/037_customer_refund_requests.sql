-- SRS 5.2: khách tự gửi yêu cầu hoàn tiền (lý do, mô tả, ảnh bằng chứng), được hủy khi còn chờ;
-- shop ghi phản hồi khi từ chối.
ALTER TABLE Refunds
    MODIFY status ENUM('REQUESTED', 'APPROVED', 'PROCESSED', 'REJECTED', 'CANCELLED') DEFAULT 'REQUESTED',
    ADD COLUMN description TEXT NULL AFTER reason,
    ADD COLUMN evidence_images TEXT NULL AFTER description,
    ADD COLUMN shop_response TEXT NULL AFTER evidence_images;
