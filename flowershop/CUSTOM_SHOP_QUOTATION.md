# Đặt hoa riêng do shop báo giá

Khách chọn shop, loại hoa, tông màu, dịp, giấy gói/phụ kiện, ngày nhận, số lượng và mẫu tham khảo tùy chọn. Mô tả không bắt buộc. Khách không nhập hoặc gửi ngân sách trong request.

`POST /api/customer/custom-orders` nhận `shopId`, `quantity`, `description`, `preferences`. Yêu cầu mới chưa có giá; shop nhập báo giá tổng và mức cọc. Khách xem báo giá, đồng ý rồi chọn địa chỉ/voucher/thanh toán cọc theo luồng hiện có.

Mẫu catalog chỉ phục vụ tham khảo thiết kế; giá hiện tại được chụp vào `estimatedTotal`, không giới hạn báo giá của shop. Danh sách tham khảo không yêu cầu tham số budget. Backend vẫn kiểm tra shop, lựa chọn đang hoạt động, ngày nhận và tồn kho mẫu tham khảo.

Giá chính thức của yêu cầu ở `desired_components.quotedPrice`; cột `budget` chỉ giữ ngân sách lịch sử và NULL với yêu cầu mới. Các đơn tương thích có snapshot cũ vẫn đọc được, nhưng không dùng ngân sách cũ làm trần báo giá.

Database có sẵn phải chạy `sql/034_custom_orders_shop_quotation.sql`. Không cần xóa dữ liệu cũ hoặc import lại database.sql.
