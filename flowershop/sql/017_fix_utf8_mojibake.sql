USE flower_shop_db;

-- Sửa dữ liệu tiếng Việt bị lỗi font (mojibake) do database.sql từng được nạp bằng client latin1.
--
-- Nguyên nhân gốc đã vá trong database.sql và db.sql (thêm SET NAMES utf8mb4), nên database tạo mới
-- sẽ không dính lỗi này. File này chỉ để chữa database đang chạy mà không phải tạo lại.
--
-- Mỗi lệnh có ba điều kiện bảo vệ, nên chạy lại nhiều lần vẫn an toàn và KHÔNG đụng vào
-- dữ liệu vốn đã đúng:
--   1. Kết quả chuyển đổi phải là UTF-8 hợp lệ (IS NOT NULL).
--   2. Kết quả phải khác giá trị hiện tại (bỏ qua chuỗi ASCII thuần).
--   3. Số dấu '?' không được tăng lên sau khi chuyển sang latin1. Dấu '?' phát sinh nghĩa là chuỗi
--      vốn đã đúng và sẽ bị hỏng nếu chuyển. Phải ĐẾM chứ không thể đòi bằng 0, vì câu chữ bình
--      thường có thể chứa dấu hỏi thật (ví dụ "còn hàng không ạ?").

-- Cột văn bản
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

-- Cột JSON
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

-- Dọn bảng thăm dò nếu còn sót từ lúc chẩn đoán.
DROP TABLE IF EXISTS _charset_probe;
