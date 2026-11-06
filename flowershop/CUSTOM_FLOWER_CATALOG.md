# Danh mục đặt hoa theo yêu cầu

Chạy `sql/023_custom_flower_options.sql` trên database đang cấu hình trong backend trước khi khởi động phiên bản này. Script chỉ thêm bảng/dữ liệu danh mục, không xóa dữ liệu đơn hàng. Có thể chạy lại mà không ghi đè các mục đã sửa.

Trong MySQL Workbench, chọn schema của ứng dụng, mở và chạy file SQL trên. Hoặc trong MySQL CLI:

```sql
USE flower_shop_db;
SOURCE C:/Fall2026/SWP391/tmp/FlowerShop_SWP391/flowershop/sql/023_custom_flower_options.sql;
```

`Custom_Flower_Options` lưu các lựa chọn `FLOWER` (loại hoa), `OCCASION` (dịp tặng), `WRAPPING` (giấy gói), `ACCESSORY` (phụ kiện). `label` là tên hiển thị, `sort_order` là thứ tự trong từng nhóm, `disabled` quyết định có được chọn hay không. Mục bị Disable vẫn hiển thị. Đổi trực tiếp database rồi tải lại trang; không cần sửa React hay build lại frontend.

```sql
UPDATE Custom_Flower_Options SET disabled = TRUE WHERE id = 'accessory-led';
UPDATE Custom_Flower_Options SET disabled = FALSE WHERE id = 'accessory-led';
INSERT INTO Custom_Flower_Options (id, option_group, label, disabled, sort_order)
VALUES (UUID(), 'ACCESSORY', 'Hoa khô trang trí', FALSE, 60);
```

Tên lựa chọn không chứa dấu phẩy vì hợp đồng API hiện tại lưu nhiều phụ kiện dưới dạng chuỗi phân cách bằng dấu phẩy. Giữ tên riêng biệt giữa hai nhóm giấy gói và phụ kiện. Tổng chuỗi phụ kiện được chọn tối đa 200 ký tự theo giới hạn thiết kế hiện tại.

Frontend gọi `GET /api/public/custom-orders/options`, không có danh sách dự phòng hardcode. Có trạng thái tải, lỗi và thử lại. Nếu danh mục chưa được cấu hình hoặc API lỗi, form ngăn gửi cho đến khi tải được danh mục. Backend kiểm tra dữ liệu lựa chọn trong database ở cả API tạo yêu cầu cũ lẫn API tạo đơn CUSTOM; từ chối lựa chọn không tồn tại, sai nhóm hoặc bị Disable, kể cả khi gửi trực tiếp qua API.

Shop, địa chỉ và mẫu hoa tham khảo tiếp tục lấy từ các bảng hiện có. Ngân sách không còn mặc định 500.000đ; khách tự nhập. Số lượng ban đầu là 1, không phải dữ liệu danh mục. Tông màu, mô tả, ngày nhận do khách nhập và được lưu cùng thiết kế. Các nhãn giao diện và giới hạn kiểm tra dữ liệu vẫn nằm trong code.

Đơn CUSTOM tiếp tục lưu tại `Orders` và `Order_Details.custom_materials` (JSON gồm ngân sách, mô tả và các tùy chọn thiết kế). API cũ lưu tại `Custom_Order_Requests.desired_components`. Tên lựa chọn được chụp lại lúc đặt hàng để đổi tên, Disable hoặc xóa danh mục sau này không làm mất nội dung đơn cũ. Không đổi luồng shop báo giá → khách đồng ý/chọn thanh toán → chuẩn bị/giao hàng.
