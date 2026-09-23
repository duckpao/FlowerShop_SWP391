# Frontend MVC và xác thực JWT

Luồng đăng ký Manager, duyệt shop và Staff nhận lời mời: [SHOP_ONBOARDING_GUIDE.md](SHOP_ONBOARDING_GUIDE.md).

Manager/Staff quản lý cửa hàng: [MANAGER_SHOP_GUIDE.md](MANAGER_SHOP_GUIDE.md).

Quản lý cửa hàng: xem [ADMIN_SHOP_GUIDE.md](ADMIN_SHOP_GUIDE.md).

Hồ sơ cá nhân và địa chỉ giao hàng Hà Nội đã được bổ sung; xem
[PROFILE_ADDRESS_GUIDE.md](PROFILE_ADDRESS_GUIDE.md) để biết API và các bước test.

Admin quản lý khách hàng: xem [ADMIN_CUSTOMER_GUIDE.md](ADMIN_CUSTOMER_GUIDE.md).

## Cấu trúc

React được tổ chức theo trách nhiệm MVC (React không có một chuẩn MVC bắt buộc):

```text
flower-shop-client/src/
  models/authModel.js              # Access token trong bộ nhớ, nhãn role
  views/AuthView.jsx               # Giao diện và liên kết sự kiện
  controllers/useAuthController.js # State và điều phối thao tác người dùng
  services/authService.js          # HTTP, CSRF, login, refresh, logout
  styles/auth.css                  # CSS của giao diện auth
  App.jsx                         # Ghép controller với view
```

Backend: `SessionController` nhận HTTP → `TokenAuthService` xử lý nghiệp vụ →
`AuthSessionRepository` lưu phiên. `JwtService` ký và kiểm tra JWT.
`JwtAuthenticationFilter` là middleware chạy trong Spring Security trước controller.
Filter kiểm tra chữ ký HS256, issuer, audience, thời hạn, loại token và phiên trong DB;
sau đó lấy role/trạng thái tài khoản hiện tại để tạo principal.

## Các API mới

| API | Chức năng |
| --- | --- |
| POST `/api/auth/login` | Nhận email/password; chỉ tài khoản ACTIVE đã xác thực được đăng nhập |
| GET `/api/auth/me` | Nhận `Authorization: Bearer <accessToken>`, trả thông tin người dùng |
| POST `/api/auth/refresh` | Dùng cookie refresh để cấp access token và xoay refresh token |
| POST `/api/auth/logout` | Thu hồi phiên hiện tại và xóa cookie refresh |

Access token tồn tại 10 phút, frontend chỉ giữ trong bộ nhớ. Refresh token có thời hạn
tuyệt đối 7 ngày, nằm trong cookie HttpOnly, SameSite=Lax, path `/api/auth`.
DB chỉ giữ SHA-256 của refresh token. Dùng lại refresh token cũ sau khi xoay sẽ thu hồi phiên đó.
Frontend tuần tự hóa refresh trong một tab và dùng Web Locks giữa các tab khi trình duyệt hỗ trợ.
Trình duyệt không hỗ trợ Web Locks có thể cần đăng nhập lại nếu nhiều tab refresh đồng thời.

Đăng xuất thu hồi một phiên; đặt lại mật khẩu thu hồi mọi phiên của tài khoản.
Mỗi yêu cầu được bảo vệ đều kiểm tra DB để việc thu hồi có hiệu lực ngay ở yêu cầu tiếp theo.
Spring Security không lưu đăng nhập trong HttpSession. Cookie phiên vẫn được dùng để bảo vệ CSRF:
mọi POST phải lấy token từ GET `/api/auth/csrf` và gửi header `X-CSRF-TOKEN`
trong cùng phiên trình duyệt. Frontend tự thực hiện bước này.

Phân quyền đường dẫn: `/api/admin/**` → ADMIN, `/api/shop/**` → SHOP (Shop Manager),
`/api/customer/**` → CUSTOMER, `/api/staff/**` → SHOP_STAFF.
SHOP là mã nội bộ của Shop Manager (Vendor), không bao gồm Shop Staff.
GHN là hệ thống bên ngoài, không phải role người dùng. Đường dẫn `/api/delivery/**`
cũ bị chặn; tích hợp GHN sau này cần xác thực riêng theo giao thức của nhà cung cấp.
Đây là quy tắc bảo vệ; không tự tạo các API nghiệp vụ còn thiếu.
Đăng ký thường tạo CUSTOMER sau OTP đúng. Đăng ký shop qua `/api/auth/register-shop`
tạo SHOP và một shop PENDING sau OTP đúng; không cho client chọn role tùy ý.

### Phạm vi vai trò theo context diagram

| Vai trò | Chức năng trong sơ đồ |
| --- | --- |
| Admin | Quản lý tài khoản, xem log hệ thống, cấu hình hệ thống, duyệt/đình chỉ cửa hàng |
| Customer | Đăng ký/quản lý tài khoản, tùy chỉnh bó hoa, chat, thanh toán, đánh giá, lưu dịp quan trọng, tương tác bài viết; xem catalog, báo giá và trạng thái đơn |
| Shop Manager (Vendor) | Cập nhật hồ sơ shop, quản lý tài khoản nhân viên, đăng bài chăm sóc hoa, cấu hình khuyến mãi/coupon, xem doanh thu và báo cáo |
| Shop Staff | Quản lý catalog, cập nhật trạng thái xử lý đơn, chat/báo giá; nhận hàng đợi đơn và yêu cầu đặt hoa riêng |

GHN, Payment Gateway và Google là ba hệ thống tích hợp bên ngoài.
Các quyền nghiệp vụ trên mô tả phạm vi cần triển khai; hiện chỉ có auth và quy tắc
phân quyền đường dẫn. API shop/nhân viên khi triển khai còn phải kiểm tra quyền trên
đúng cửa hàng (owner hoặc quan hệ nhân viên–shop), không chỉ kiểm tra role.

## Chạy và kiểm tra

1. Database hiện tại đã được bổ sung bảng `Auth_Sessions` bằng migration
   `flowershop/sql/004_auth_sessions.sql`. Máy khác cần chạy migration này trên DB đã có.
   Không chạy lại `database.sql` để nâng cấp vì đó là script khởi tạo có xóa bảng.
   Chạy thêm `sql/005_context_diagram_roles.sql` để bổ sung SHOP_STAFF.
   DELIVERY được giữ trong enum và DB để xử lý sau; tài khoản cũ không bị đổi role
   hoặc xóa. Login, refresh và xác thực access token đều từ chối DELIVERY hiện tại.
   Bốn vai trò hoạt động vẫn là ADMIN, SHOP, CUSTOMER và SHOP_STAFF.
2. Khởi động lại backend trong thư mục `flowershop`: `./gradlew.bat bootRun`.
3. Trong `flower-shop-client`: `npm.cmd run dev` và mở địa chỉ Vite in ra.
4. Đăng ký → nhận OTP → xác thực → Sign in. Dùng tài khoản vừa xác thực đăng nhập.
5. Kiểm tra trang tài khoản hiển thị email/role. Reload trang để thử refresh cookie.
6. Đăng xuất rồi reload: không khôi phục tài khoản. Thử quên mật khẩu và đăng nhập bằng mật khẩu mới.

Swagger: lấy CSRF trước, POST login với email/password và header CSRF.
Copy `accessToken` trả về vào nút **Authorize** (chỉ token, không thêm chữ Bearer).
Gọi GET `/api/auth/me` phải trả 200. Gọi khi chưa Authorize phải trả 401.
Refresh/logout dùng cookie tự lưu của trình duyệt và header CSRF; logout xong token cũ phải trả 401.

## Cấu hình triển khai

Local cho phép sinh khóa JWT ngẫu nhiên mỗi lần backend khởi động, nên token cũ mất hiệu lực sau restart.
Khi triển khai, đặt `JWT_SECRET` là Base64 của ít nhất 32 byte ngẫu nhiên, lưu trong secret manager;
đặt `JWT_ALLOW_EPHEMERAL_KEY=false` và `SESSION_COOKIE_SECURE=true`, chạy HTTPS.
Dùng cùng khóa cho các instance. Không commit khóa hoặc mật khẩu SMTP vào Git.
Thiết kế cookie hiện tại phù hợp frontend/backend cùng site, ví dụ reverse proxy `/api`.

Login có giới hạn 20 yêu cầu/IP/phút trong bộ nhớ từng instance. Khi chạy nhiều instance
cần rate limiter dùng chung và cấu hình proxy tin cậy. Mail vẫn gửi sau commit,
chưa có hàng đợi bền vững để tự thử lại khi SMTP lỗi.

## Kiểm thử

Backend: `./gradlew.bat test --tests com.example.flowershop.JwtAuthenticationTests --tests com.example.flowershop.EmailVerificationTests`.
Test dùng H2 và mail giả lập, không gửi thư thật. Bao gồm OTP, login, CSRF,
token sai/hết hạn, refresh replay, phân quyền, logout và thu hồi phiên sau reset.
Frontend: `npm.cmd run build` và `npm.cmd run lint`.
