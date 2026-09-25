# Chạy React và backend cùng cổng 8080

React sử dụng JSX: các thẻ div, form, button là JSX chuẩn được React render. Không có thao tác đổi chúng thành một bộ thẻ HTML khác.

Yêu cầu: Java 17, Node/npm, MySQL đang chạy và cấu hình backend như trước.

Từ thư mục gốc, chỉ chạy:

```powershell
cd flowershop
.\gradlew.bat bootRun
```

Gradle tự cài dependency frontend bằng npm ci khi cần, build React và đưa dist vào tài nguyên static của backend. bootJar cũng đóng gói giao diện. Không chạy hai tiến trình trên cùng cổng.

Hoặc từ flower-shop-client chạy `npm.cmd run dev`: lệnh này hiện khởi động Gradle bootRun, không còn mở Vite server.

- Trang chủ: http://localhost:8080/
- Đăng nhập: http://localhost:8080/login
- Admin: http://localhost:8080/admin
- Manager: http://localhost:8080/shop-admin
- Swagger: http://localhost:8080/swagger-ui/index.html

Khi sửa frontend, dừng và chạy lại bootRun để Gradle build/copy tài nguyên mới, sau đó tải lại trình duyệt. Chế độ này không có Vite hot reload.

Các route React được forward về index.html để mở trực tiếp hoặc F5. API vẫn được bảo vệ bởi JWT/CSRF và phân quyền; cho tải giao diện không cấp quyền gọi API. Bản build không hiển thị các trang bypass dev-admin/dev-manager; test bằng tài khoản đăng nhập thật.
