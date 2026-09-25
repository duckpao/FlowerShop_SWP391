# Admin quản lý khách hàng

Shop Management đã có: xem [ADMIN_SHOP_GUIDE.md](ADMIN_SHOP_GUIDE.md).
Dev-admin nay cũng hỗ trợ GET danh sách/chi tiết shop và PUT approve/block/unblock
dưới `/api/admin/shops`, vẫn kiểm tra localhost và CSRF như quản lý khách hàng.

## Test local không cần đăng nhập

Dừng backend đang chạy bằng Ctrl+C, sau đó trong thư mục `flowershop`:

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=dev-admin"
```

Trong thư mục `flower-shop-client`, chạy `npm.cmd run dev` và mở
`http://localhost:8080/dev-admin` (đổi cổng nếu Vite báo cổng khác).
Trang này không cần tài khoản/Authorize; các thao tác vẫn lấy CSRF tự động.
Swagger cũng có thể gọi các API Customer Management không cần Authorize khi profile này bật;
PUT vẫn phải lấy CSRF mới trước mỗi lần gọi.

Profile bắt buộc server.address=127.0.0.1; không dùng cùng profile prod/production.
Chỉ GET danh sách/chi tiết và PUT blocked dưới `/api/admin/customers` nhận danh tính thử nghiệm
`local-dev-admin`, chỉ khi kết nối loopback và không gửi Authorization.
Các API khác vẫn giữ xác thực bình thường. JWT sai không được tự chuyển thành Admin thử nghiệm.
Không tạo hoặc nâng quyền tài khoản nào trong DB. Thao tác khóa vẫn ghi vào DB đang cấu hình.
Không công khai backend/frontend này qua tunnel/reverse proxy hoặc chạy Vite với `--host`.

Tắt chế độ bằng Ctrl+C rồi chạy lại `.\gradlew.bat bootRun`; API Admin lại yêu cầu JWT.
Trang `/dev-admin` chỉ được bật ở Vite development, không bật trong bản frontend build production.

## Chế độ đăng nhập thông thường

Đã triển khai danh sách có tìm kiếm/phân trang/lọc trạng thái, chi tiết và khóa/mở khóa Customer.
Chỉ role ADMIN được dùng; không cho quản lý Admin/Shop Manager/Shop Staff qua API này.

| Method | URL | Mục đích |
| --- | --- | --- |
| GET | `/api/admin/customers?q=&status=&page=0&size=10` | Tìm theo tên/email/điện thoại; lọc ACTIVE/BANNED/INACTIVE |
| GET | `/api/admin/customers/{id}` | Xem chi tiết Customer |
| PUT | `/api/admin/customers/{id}/blocked` | Body `{"blocked":true}` để khóa hoặc `{"blocked":false}` để mở khóa |

Tất cả yêu cầu JWT của Admin. PUT cần thêm CSRF mới lấy từ `/api/auth/csrf` trong cùng phiên trình duyệt.
Trang bắt đầu từ 0; kích thước 1–100 (mặc định 10), từ khóa tối đa 100 ký tự.
Response phân trang gồm content, page, size, totalElements, totalPages. Không trả hash mật khẩu/token/googleId.

Khóa chuyển trạng thái sang BANNED, thu hồi toàn bộ phiên trong cùng transaction và ghi last_modify_by.
Mở khóa chuyển BANNED về ACTIVE; không xác thực email hộ khách hàng và không khôi phục token cũ.
Không dùng thao tác mở khóa để kích hoạt trực tiếp tài khoản INACTIVE.
Khóa lặp lại an toàn; mở khóa ACTIVE giữ ACTIVE. Không xóa khách hàng hoặc dữ liệu đơn hàng.
Hiện lưu người cập nhật cuối trên Users, chưa có lịch sử audit đầy đủ.

## Test

1. Restart backend, chạy frontend, đăng nhập Admin ACTIVE đã xác thực email.
2. Trang tài khoản có mục Quản lý khách hàng: tìm kiếm, lọc, chuyển trang và xem chi tiết.
3. Đăng nhập Customer thử nghiệm ở cửa sổ ẩn danh. Admin khóa tài khoản đó.
4. Customer gọi API hoặc kiểm tra phiên: bị từ chối; đăng nhập cũng bị chặn.
5. Admin mở khóa: Customer phải đăng nhập mới; token cũ vẫn không dùng được.
6. Token Customer gọi API Admin phải trả 403. Token Admin nhắm ID tài khoản Shop/Admin phải trả 404.

Tài khoản admin seed chưa xác thực sẽ không đăng nhập được. Không cần tắt kiểm tra OTP:
dùng một tài khoản đã hoàn tất đăng ký OTP và được quản trị viên cấp role ADMIN trong môi trường test.
Thay đổi này không tự nâng quyền bất kỳ tài khoản thật nào.

Không cần migration mới. Không chạy lại database.sql.

```powershell
.\gradlew.bat test --tests com.example.flowershop.AdminCustomerTests
```

Test sử dụng H2 và mail giả lập. Frontend theo MVC:
`controllers/useAdminCustomersController.js`, `views/AdminCustomersView.jsx`, `services/adminCustomerService.js`.
