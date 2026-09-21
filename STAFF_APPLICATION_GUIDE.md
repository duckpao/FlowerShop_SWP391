# Test luồng Customer xin làm Staff

Chạy migration `flowershop/sql/009_staff_applications.sql` trên database `flower_shop_db` rồi khởi động lại backend. Không chạy lại `database.sql` trên DB đang có dữ liệu vì file đó xóa database.

1. Mở http://localhost:5173/ — trang index hiển thị các shop ACTIVE, tìm theo tên (tối đa 50 kết quả).
2. Bấm Đăng nhập → `/login`. Đăng ký trên giao diện luôn tạo Customer, xác thực OTP rồi đăng nhập. Trang index hiển thị “Bạn đang là Customer”.
3. Tìm shop, bấm Vào shop để mở `/shops/{id}`. Giao diện shop hiển thị giới thiệu và mục Tuyển dụng, chưa mở form. Bấm Đăng ký làm nhân viên mới mở form; khách chưa đăng nhập được chuyển đến login và quay lại đúng shop. Điền họ tên, số điện thoại, giới thiệu/kinh nghiệm, gửi đơn. Form đóng lại và thông báo gửi thành công; quyền vẫn là Customer.
4. Mở trình duyệt khác hoặc cửa sổ riêng tư, đăng nhập tài khoản Manager sở hữu shop đó. Trang index hiển thị Manager; bấm Vào trang quản trị shop.
5. Manager nhận email về đơn mới; trên trang chủ có Thông báo đăng ký làm nhân viên kèm số đơn chờ duyệt. Bấm để vào trang quản trị và mở danh sách. Chọn Duyệt hoặc Từ chối. Manager chỉ xem/xử lý đơn của shop mình. Danh sách tự tải lại mỗi 15 giây.
6. Nếu từ chối: tài khoản vẫn Customer, có thể gửi lại đơn. Nếu duyệt: tài khoản chuyển SHOP_STAFF, liên kết với shop được tạo và các phiên cũ bị thu hồi.
7. Customer nhận email kết quả và xem tại Thông báo ứng tuyển của tôi (trạng thái được lưu trong DB). Người được duyệt đăng nhập lại: index hiển thị “Bạn đang là Staff”, có nút Vào trang quản trị shop; chỉ thấy shop được phân công. Phiên cũ bị thu hồi nên sẽ hiện yêu cầu đăng nhập lại khi tải thông báo. Người bị từ chối vẫn Customer, nhận lời cảm ơn và thông báo chưa được tiếp nhận. Email gửi sau commit; lỗi SMTP được ghi log, kết quả vẫn xem được trong ứng dụng. Trang Staff hiện là trang cơ bản kiểm tra phân quyền, chưa bổ sung catalog/đơn hàng/chat.
8. Kiểm tra gửi đơn trùng đang chờ bị từ chối; Staff không gửi đơn Customer; shop bị khóa không nhận/duyệt đơn; Manager khác không đọc/duyệt được đơn.

API mới:
- GET `/api/public/shops?q=...`: danh sách công khai các shop ACTIVE.
- POST `/api/customer/shops/{shopId}/applications`: body `{"fullName":"Nguyễn Văn A","phone":"0912345678","introduction":"Kinh nghiệm cắm hoa"}`.
- GET `/api/account/staff-applications`: đơn của người đăng nhập.
- GET `/api/shop/mine/{shopId}/applications`: Manager xem đơn shop mình.
- PUT `/api/shop/mine/{shopId}/applications/{id}`: body `{"approve":true}` hoặc `false`.

Các API ghi vẫn cần JWT và CSRF; frontend tự gửi. Không truyền role hoặc userId trong form để tự nâng quyền. Luồng lời mời cũ không còn hiển thị ở các trang chính; API lời mời vẫn giữ để tương thích. API đăng ký shop riêng vẫn giữ cho nghiệp vụ Vendor, nhưng không còn lựa chọn này trong form đăng ký Customer.

Để test quyền thực tế, dùng tài khoản Manager đăng nhập bình thường. Trang `/dev-manager` chỉ dùng thử với profile dev-manager và chủ shop cấu hình ở backend.
